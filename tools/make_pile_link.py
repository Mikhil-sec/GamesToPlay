"""
Builds a signed FRIENDS pile link without the app — for testing the import sheet on a device,
and as an independent implementation of the format in PileSnapshotCodec.kt.

    python tools/make_pile_link.py                     # a sample pile, fresh key
    python tools/make_pile_link.py --seq 2 --key k.pem # same "friend", newer version
    adb shell am start -a android.intent.action.VIEW -d "<printed link>"

Needs the `cryptography` package. The key file is created on first use when --key is given, so
re-running with the same --key and a higher --seq exercises the "pile updated" path.
"""
import argparse
import base64
import os
import time

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

HOST = "continue-worker.gamestoplay.workers.dev"
DOMAIN = b"CONTINUE?/pile-link/v1\n"
# Wire order — must match PileSnapshotCodec.SECTION_ORDER.
SECTIONS = ["PLAYING", "COMPLETED", "BACKLOG", "WISHLIST", "DROPPED"]

SAMPLE = {
    "PLAYING": [119133, 1942],          # Elden Ring, The Witcher 3
    "COMPLETED": [7346, 26758],         # Zelda: Breath of the Wild, Super Mario Odyssey
    # GTA V, Dark Souls III, Hollow Knight, RDR2, The Last of Us Part II — and 72870, which is
    # deliberately NOT in the bundled offline index, so it exercises the network fill-in path.
    "BACKLOG": [1020, 11133, 14593, 25076, 26192, 72870],
    "WISHLIST": [119171],               # Baldur's Gate III
    "DROPPED": [1877],                  # Cyberpunk 2077
}
SAMPLE_RANKING = [26758, 7346, 1942]


def varint(value: int) -> bytes:
    out = bytearray()
    while value >= 0x80:
        out.append((value & 0x7F) | 0x80)
        value >>= 7
    out.append(value)
    return bytes(out)


def load_key(path):
    if path and os.path.exists(path):
        with open(path, "rb") as f:
            return serialization.load_pem_private_key(f.read(), password=None)
    key = ec.generate_private_key(ec.SECP256R1())
    if path:
        with open(path, "wb") as f:
            f.write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                                      serialization.NoEncryption()))
    return key


def encode(key, seq, shared_at, sections, ranking) -> str:
    body = bytearray([1])
    body += key.public_key().public_bytes(serialization.Encoding.X962,
                                          serialization.PublicFormat.UncompressedPoint)
    body += varint(seq) + varint(shared_at)
    for name in SECTIONS:
        ids = sorted(sections.get(name, []))
        body += varint(len(ids)) + varint(len(ids))
        previous = 0
        for game_id in ids:
            body += varint(game_id - previous)
            previous = game_id
    body += varint(len(ranking)) + b"".join(varint(i) for i in ranking)
    signature = key.sign(DOMAIN + bytes(body), ec.ECDSA(hashes.SHA256()))
    body += bytes([len(signature)]) + signature
    return base64.urlsafe_b64encode(bytes(body)).rstrip(b"=").decode()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--key", help="PEM file to reuse (created if missing)")
    parser.add_argument("--seq", type=int, default=1)
    args = parser.parse_args()
    payload = encode(load_key(args.key), args.seq, int(time.time()), SAMPLE, SAMPLE_RANKING)
    print(f"https://{HOST}/p#{payload}")


if __name__ == "__main__":
    main()
