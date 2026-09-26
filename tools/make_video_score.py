"""
The soundtrack for the Devpost demo video, composed from the same instruments as the app's
NEON DRIVE music pack (tools/make_audio.py) and arranged to video/timeline.json.

Every shot in the timeline has a length in bars and a `music` section name; this script writes
that section for exactly that many bars, so the score always follows the edit. Re-run it after
changing any shot's `bars` or `music`:

    python tools/make_video_score.py

Output: video/public/audio/score.wav, plus WAV copies of the app's sound effects in
video/public/audio/ (the renderer is happier with WAV than with Ogg Vorbis).

Like everything make_audio.py makes, the output is an original work, dedicated to the public
domain under CC0 1.0 (docs/AUDIO-LICENSE.md). No samples, loops or recordings from anyone else.
"""
import json
import pathlib
import sys

import numpy as np
import soundfile as sf

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from make_audio import (  # noqa: E402  (the path insert has to come first)
    SR, Track, crash, gated_snare, hat, highpass, kick, lowpass, noise, pad, pluck,
    room, samples, saw, sine, synth_bass, synth_lead, up, OUT as RAW_DIR,
)

ROOT = pathlib.Path(__file__).resolve().parent.parent
TIMELINE = ROOT / "video" / "timeline.json"
AUDIO_OUT = ROOT / "video" / "public" / "audio"

MINOR = [(["A3", "C4", "E4"], "A1"), (["F3", "A3", "C4"], "F1"),
         (["C4", "E4", "G4"], "C2"), (["G3", "B3", "D4"], "G1")]
CONT = [(["E4", "G4", "B4"], "E1"), (["C4", "E4", "G4"], "C2"),
        (["D4", "F#4", "A4"], "D2"), (["B3", "D#4", "F#4"], "B1")]
MAJOR = [(["C4", "E4", "G4"], "C2"), (["G3", "B3", "D4"], "G1"),
         (["A3", "C4", "E4"], "A1"), (["F3", "A3", "C4"], "F1")]
VICTORY = [(["C4", "E4", "G4"], "C2"), (["F3", "A3", "C4"], "F1"),
           (["G3", "B3", "D4"], "G1"), (["C4", "E4", "G4", "C5"], "C2"),
           (["C4", "E4", "G4", "C5"], "C2")]

# The NEON DRIVE title melody, 4 bars.
THEME = [("E5", 1.5), ("D5", .5), ("C5", 1), ("D5", 1),
         ("C5", 1.5), ("A4", .5), ("C5", 1), ("F5", 1),
         ("E5", 1.5), ("D5", .5), ("C5", 1), ("G5", 1),
         ("D5", 3), (None, 1)]
FANFARE = [("G4", .5), ("C5", .5), ("E5", .5), ("G5", .5), ("C6", 2),
           ("E6", 2), ("D6", 1), ("C6", 1),
           ("D6", 1.5), ("B5", .5), ("G5", 2),
           ("C6", 4), (None, 4)]


def riser(seconds: float, vol=0.18) -> np.ndarray:
    """A saw sweeping up three octaves under rising noise: the 'here it comes' before a drop."""
    n = samples(seconds)
    t = np.linspace(0, 1, n)
    tone = lowpass(saw(110 * 2 ** (3 * t), n), 3500) * 0.5
    air = highpass(noise(n), 2000) * 0.5
    return (tone + air) * t ** 2 * vol


def impact(vol=0.6) -> np.ndarray:
    """A sub drop for big text hits."""
    n = samples(1.2)
    f = 38 + 70 * np.exp(-np.arange(n) / (0.05 * SR))
    return sine(f, n) * np.exp(-np.arange(n) / (0.35 * SR)) * vol


def chord_at(prog, bar):
    return prog[bar % len(prog)]


def lay_pads(tr, b0, bars, prog, vol=0.11, bright=2200, octave_up=False):
    for i in range(bars):
        ch, _ = chord_at(prog, i)
        notes = [up(c) for c in ch] if octave_up else ch
        tr.add(b0 + i * 4, pad(notes, tr.seconds(4.2), vol=vol, attack=0.25, release=0.3,
                               bright=bright, kind="saw"))


def lay_bass(tr, b0, bars, prog, vol=0.4, octave=True, step=0.5):
    for i in range(bars):
        _, root = chord_at(prog, i)
        k = 0
        b = 0.0
        while b < 4 - 1e-6:
            note = up(root) if (octave and k % 2) else root
            tr.add(b0 + i * 4 + b, synth_bass(note, tr.seconds(step * 0.9), vol))
            b += step
            k += 1


def lay_plucks(tr, b0, bars, prog, vol=0.05):
    for i in range(bars):
        ch, _ = chord_at(prog, i)
        for j in range(16):
            tr.add(b0 + i * 4 + j * 0.25, pluck(up(ch[j % len(ch)]), tr.seconds(0.22), vol))


def lay_drums(tr, b0, bars, vol=1.0, snare=True, open_hats=False):
    for i in range(bars):
        b = b0 + i * 4
        tr.add(b, kick(0.8 * vol))
        tr.add(b + 2, kick(0.8 * vol))
        if snare:
            tr.add(b + 1, gated_snare(0.4 * vol))
            tr.add(b + 3, gated_snare(0.4 * vol))
        for j in range(8):
            h = hat(0.28 * vol, open_=open_hats and j % 2 == 1)
            tr.add(b + j * 0.5, lowpass(h, 9000))


def roll(tr, b0, beats, vol=0.35):
    """A snare roll that doubles its rate halfway and swells."""
    b, k = 0.0, 0
    while b < beats - 1e-6:
        step = 0.5 if b < beats / 2 else 0.25
        tr.add(b0 + b, gated_snare(vol * (0.35 + 0.65 * b / beats)))
        b += step
        k += 1


def lead(tr, b0, notes, vol=0.2, cutoff=3200):
    tr.melody(b0, notes, lambda nm, secs: synth_lead(nm, secs * 0.95, vol, cutoff))


# ─────────────── sections: fn(track, start_beat, bars) ───────────────

def s_intro(tr, b0, bars):
    # Under the CRT power-on: a low drone that opens up, nothing rhythmic.
    tr.add(b0, pad(["A2", "E3"], tr.seconds(bars * 4 + 0.5), vol=0.07, attack=2.5, release=0.3,
                   bright=700, kind="saw"))


def s_build(tr, b0, bars):
    lay_pads(tr, b0, bars, MINOR, vol=0.08, bright=1200)
    for i in range(bars):  # bass enters filtered and opens up bar by bar
        _, root = chord_at(MINOR, i)
        for j in range(8):
            x = synth_bass(root, tr.seconds(0.45), 0.34)
            tr.add(b0 + i * 4 + j * 0.5, lowpass(x, 300 + 250 * i))
        for j in range(16 if i == bars - 1 else 8):
            tr.add(b0 + i * 4 + j * (0.25 if i == bars - 1 else 0.5), lowpass(hat(0.16), 9000))
    tr.add(b0 + (bars - 1) * 4, kick(0.6))
    tr.add(b0 + (bars - 1) * 4 + 2, kick(0.6))


def s_build2(tr, b0, bars):
    # The tagline card: held chord, a roll and a riser into the drop, last half-beat silent.
    tr.add(b0, impact(0.5))
    tr.add(b0, crash(0.16))
    tr.add(b0, pad(["F3", "A3", "C4", "E4"], tr.seconds(4.2), vol=0.1, attack=0.05, release=0.4,
                   bright=2000, kind="saw"))
    tr.add(b0 + 4, pad(["G3", "B3", "D4"], tr.seconds(3.4), vol=0.1, attack=0.05, release=0.1,
                       bright=2400, kind="saw"))
    roll(tr, b0 + 4, bars * 4 - 4.5)
    tr.add(b0 + 4, riser(tr.seconds(bars * 4 - 4.5)))


def s_drop(tr, b0, bars):
    tr.add(b0, crash(0.22))
    tr.add(b0, impact(0.4))
    lay_pads(tr, b0, bars, MINOR)
    lay_bass(tr, b0, bars, MINOR)
    lay_plucks(tr, b0, bars, MINOR)
    lay_drums(tr, b0, bars)


def s_groove(tr, b0, bars):
    lay_pads(tr, b0, bars, MINOR, vol=0.1)
    lay_bass(tr, b0, bars, MINOR)
    lay_plucks(tr, b0, bars, MINOR, vol=0.045)
    lay_drums(tr, b0, bars, vol=0.9)


def s_drop2(tr, b0, bars):
    s_drop(tr, b0, bars)
    for k in range(0, bars, 4):
        lead(tr, b0 + k * 4, THEME, vol=0.2)


def s_theme2(tr, b0, bars):
    # "Like this soundtrack?": the NEON DRIVE melody comes forward, alone over the groove, for
    # exactly the bars given (the first half of the theme for a 2-bar shot).
    tr.add(b0, crash(0.18))
    lay_pads(tr, b0, bars, MINOR, vol=0.09)
    lay_bass(tr, b0, bars, MINOR, vol=0.34)
    lay_drums(tr, b0, bars, vol=0.8)
    notes, total = [], 0.0
    for nm, ln in THEME:
        if total >= bars * 4:
            break
        notes.append((nm, min(ln, bars * 4 - total)))
        total += ln
    lead(tr, b0, notes, vol=0.26)


def s_break(tr, b0, bars):
    # Everything drops out for "it's a decision problem": pad, sub, one lead phrase.
    tr.add(b0, impact(0.45))
    lay_pads(tr, b0, bars, MINOR, vol=0.09, bright=1100)
    for i in range(bars):
        _, root = chord_at(MINOR, i)
        tr.add(b0 + i * 4, lowpass(synth_bass(root, tr.seconds(3.8), 0.3), 250))
    lead(tr, b0 + 4 * (bars - 1), [("E5", 2), ("D5", 1), ("C5", 1)], vol=0.12, cutoff=1800)


def s_rise(tr, b0, bars):
    # The dials: sixteenth bass opening up, roll, riser, then a hard stop for the lever.
    lay_pads(tr, b0, bars, [MINOR[3], MINOR[3]], vol=0.09, bright=1600)
    total = bars * 4
    b = 0.0
    while b < total - 0.5 - 1e-6:
        x = synth_bass("G1", tr.seconds(0.22), 0.32)
        tr.add(b0 + b, lowpass(x, 300 + 1400 * b / total))
        b += 0.25
    roll(tr, b0 + 4 * (bars - 1), 3.5, vol=0.4)
    tr.add(b0, riser(tr.seconds(total - 0.5), vol=0.14))


def s_stop(tr, b0, bars):
    pass  # silence; the lever carries it


def s_continue(tr, b0, bars):
    # The CONTINUE? gate: minor, a ticking clock, patient.
    lay_pads(tr, b0, bars, CONT, vol=0.09, bright=1800)
    for i in range(bars):
        _, root = chord_at(CONT, i)
        for j in range(8):
            tr.add(b0 + i * 4 + j * 0.5, synth_bass(root, tr.seconds(0.42), 0.36))
        tr.add(b0 + i * 4, kick(0.7))
        tr.add(b0 + i * 4 + 2, kick(0.5))
        for beat in range(4):
            tr.add(b0 + i * 4 + beat, lowpass(hat(0.38 if beat == 0 else 0.24), 8000))
    phrase = [("B4", 4), ("C5", 4), ("A4", 4), ("D#5", 4)]
    lead(tr, b0, [phrase[i % len(phrase)] for i in range(bars)], vol=0.13, cutoff=2200)


def s_free(tr, b0, bars):
    # FREE PLAY: the power-up, back to major, everything bright.
    tr.add(b0, crash(0.22))
    lay_pads(tr, b0, bars, MAJOR, vol=0.11, bright=2800)
    lay_bass(tr, b0, bars, MAJOR)
    lay_plucks(tr, b0, bars, MAJOR, vol=0.055)
    lay_drums(tr, b0, bars, open_hats=True)


def s_victory(tr, b0, bars):
    tr.add(b0, crash(0.24))
    lay_pads(tr, b0, bars, VICTORY, vol=0.12, bright=2600)
    lay_bass(tr, b0, min(bars, 4), VICTORY, vol=0.36)
    lay_drums(tr, b0, min(bars, 4), open_hats=True)
    tr.add(b0 + 12, crash(0.2))
    lead(tr, b0, FANFARE, vol=0.24, cutoff=3800)


def s_outro(tr, b0, bars):
    # End card: a G bar with a light roll, then the final C lands with the coin on beat 4.
    tr.add(b0, pad(["G3", "B3", "D4"], tr.seconds(4), vol=0.1, attack=0.05, release=0.1,
                   bright=2200, kind="saw"))
    for j in range(8):
        tr.add(b0 + j * 0.5, synth_bass("G1", tr.seconds(0.45), 0.34))
    roll(tr, b0 + 2, 2, vol=0.3)
    ring = bars * 4 - 4
    tr.add(b0 + 4, crash(0.26))
    tr.add(b0 + 4, kick(0.9))
    tr.add(b0 + 4, impact(0.5))
    tr.add(b0 + 4, pad(["C4", "E4", "G4", "C5"], tr.seconds(ring + 2.5), vol=0.13, attack=0.02,
                       release=2.5, bright=2600, kind="saw"))
    tr.add(b0 + 4, synth_bass("C2", tr.seconds(ring + 1), 0.3))
    lead(tr, b0 + 4, [("C6", ring)], vol=0.18, cutoff=3000)


SECTIONS = {
    "intro": s_intro, "build": s_build, "build2": s_build2, "drop": s_drop, "groove": s_groove,
    "drop2": s_drop2, "theme2": s_theme2, "break": s_break, "rise": s_rise, "stop": s_stop, "continue": s_continue,
    "free": s_free, "victory": s_victory, "outro": s_outro,
}


def main() -> None:
    tl = json.loads(TIMELINE.read_text(encoding="utf-8"))
    bpm = tl["bpm"]
    total_bars = sum(s["bars"] for s in tl["shots"])
    tr = Track(bpm, total_bars * 4, loop=False)

    beat = 0
    for shot in tl["shots"]:
        fn = SECTIONS.get(shot["music"])
        if fn is None:
            raise SystemExit(f"shot {shot['id']}: unknown music section {shot['music']!r}; "
                             f"use one of {', '.join(SECTIONS)}")
        fn(tr, beat, shot["bars"])
        beat += shot["bars"] * 4

    x = room(tr.buf, 0.18, 1.0)[: samples(tr.seconds(total_bars * 4) + 3.0)]
    x = lowpass(x, 9000)
    x = x - np.mean(x)
    x = x * (10 ** (-17 / 20) / (np.sqrt(np.mean(x ** 2)) + 1e-12))
    x = 0.89 * np.tanh(x / 0.89)
    fo = samples(2.5)
    x[-fo:] *= np.linspace(1, 0, fo) ** 2

    AUDIO_OUT.mkdir(parents=True, exist_ok=True)
    sf.write(AUDIO_OUT / "score.wav", np.stack([x, x], axis=1), SR, subtype="PCM_16")
    for ogg in sorted(RAW_DIR.glob("sfx_*.ogg")):
        data, rate = sf.read(ogg)
        sf.write(AUDIO_OUT / (ogg.stem + ".wav"), data, rate, subtype="PCM_16")

    # The final soundtrack: the score plus every shot's sound effects at its beat, then one soft
    # limiter over the lot. Mixing here rather than in the renderer means an effect landing on a
    # drum hit can't clip, and the whole thing sits at one loudness.
    mix = x * 0.8
    beat = 0
    for shot in tl["shots"]:
        for fx in shot.get("sfx", []):
            data, _ = sf.read(AUDIO_OUT / fx["file"])
            data = data if data.ndim == 1 else data[:, 0]
            at = samples(tr.seconds(beat + fx["beat"]))
            end = min(len(mix), at + len(data))
            mix[at:end] += data[: end - at] * fx.get("volume", 1.0) * 0.6
        beat += shot["bars"] * 4
    mix = mix * (10 ** (-15 / 20) / (np.sqrt(np.mean(mix ** 2)) + 1e-12))
    mix = 0.95 * np.tanh(mix / 0.95)
    sf.write(AUDIO_OUT / "mix.wav", np.stack([mix, mix], axis=1), SR, subtype="PCM_16")
    print(f"mix.wav: peak {np.abs(mix).max():.2f}, "
          f"rms {20 * np.log10(np.sqrt(np.mean(mix ** 2))):.1f} dBFS")

    secs = tr.seconds(total_bars * 4)
    print(f"score.wav: {total_bars} bars at {bpm} bpm = {secs:.1f} s "
          f"({int(secs // 60)}:{secs % 60:04.1f})")


if __name__ == "__main__":
    main()
