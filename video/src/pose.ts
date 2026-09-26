import {Easing, interpolate} from 'remotion';
import {beats, PHONE_VISIBLE, PlacedShot, SHOTS} from './timeline';

/** Phone height in px at scale 1, in a 1920×1080 frame. */
export const PHONE_H = 860;

export type Pose = {x: number; y: number; s: number; ry: number; rx: number; rz: number};

const BASE: Record<'left' | 'right' | 'center', Pose> = {
	right: {x: 430, y: 10, s: 1, ry: -17, rx: 5, rz: 1.2},
	left: {x: -430, y: 10, s: 1, ry: 17, rx: 5, rz: -1.2},
	center: {x: 0, y: 0, s: 1, ry: 0, rx: 4, rz: 0},
};

const expoOut = Easing.bezier(0.16, 1, 0.3, 1);
const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

export const mix = (a: Pose, b: Pose, k: number): Pose => ({
	x: a.x + (b.x - a.x) * k,
	y: a.y + (b.y - a.y) * k,
	s: a.s + (b.s - a.s) * k,
	ry: a.ry + (b.ry - a.ry) * k,
	rx: a.rx + (b.rx - a.rx) * k,
	rz: a.rz + (b.rz - a.rz) * k,
});

/** Where a shot wants the phone `f` frames in: its layout, a slow drift, and any push-in. */
const shotPose = (shot: PlacedShot, f: number): Pose | null => {
	if (!PHONE_VISIBLE[shot.layout]) return null;
	const d = shot.duration;

	if (shot.layout === 'cold') {
		// Start close enough that the CRT fills the middle of the frame, then pull back to reveal it.
		const k = interpolate(f, [d * 0.3, d], [0, 1], {...clamp, easing: Easing.inOut(Easing.cubic)});
		return mix({x: 0, y: 40, s: 2.5, ry: 0, rx: 0, rz: 0}, {...BASE.center, s: 1.02}, k);
	}

	const base = BASE[shot.layout as 'left' | 'right' | 'center'];
	// Never static: a slow turn toward the viewer and a slight push-in across the whole shot.
	const t = interpolate(f, [0, d], [0, 1], {...clamp, easing: Easing.inOut(Easing.sin)});
	let p: Pose = {
		...base,
		ry: base.ry * (1 - 0.3 * t),
		s: base.s * (1 + 0.045 * t),
		y: base.y - 12 * t,
	};

	if (shot.push) {
		const pb = beats(shot.push.beat);
		const k = interpolate(f, [pb, pb + 40], [0, 1], {...clamp, easing: expoOut});
		if (k > 0) {
			const s = shot.push.scale;
			const target: Pose = {
				x: shot.push.center ? 0 : p.x,
				// Put the focus point (a fraction of the phone's height from the top) at frame centre.
				y: -(shot.push.focusY - 0.5) * PHONE_H * s,
				s,
				ry: shot.push.center ? 0 : p.ry * 0.4,
				rx: 0,
				rz: 0,
			};
			p = mix(p, target, k);
		}
	}
	return p;
};

/** The phone's pose on any frame, gliding from the previous shot's pose into this one's. */
export const phonePoseAt = (frame: number): Pose | null => {
	let i = SHOTS.length - 1;
	while (i > 0 && frame < SHOTS[i].start) i--;
	const shot = SHOTS[i];
	const f = frame - shot.start;
	const cur = shotPose(shot, f);
	const prevShot = i > 0 ? SHOTS[i - 1] : null;
	const prev = prevShot ? shotPose(prevShot, prevShot.duration) : null;

	if (cur && prev) {
		return mix(prev, cur, interpolate(f, [0, 32], [0, 1], {...clamp, easing: expoOut}));
	}
	if (cur && !prev) {
		if (i === 0) return cur;
		const from = {...cur, y: cur.y + 900, ry: cur.ry * 2, rx: 25};
		return mix(from, cur, interpolate(f, [0, 36], [0, 1], {...clamp, easing: expoOut}));
	}
	if (!cur && prev) {
		const k = interpolate(f, [0, 16], [0, 1], {...clamp, easing: Easing.in(Easing.cubic)});
		if (k >= 1) return null;
		return mix(prev, {...prev, y: prev.y + 1000, rx: -20}, k);
	}
	return null;
};
