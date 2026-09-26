import React from 'react';
import {Easing, Img, interpolate, staticFile} from 'remotion';
import {Pose} from '../pose';
import {beats, PlacedShot} from '../timeline';

const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;
const expoOut = Easing.bezier(0.16, 1, 0.3, 1);

/**
 * The share card the phone just rendered, pulled out of the screen and held up beside it (the
 * "exploded UI" move): it starts small at the phone's centre, flies forward and to the side with a
 * tilt, floats, then drops back into the phone before the next clip.
 */
export const FlyingCard: React.FC<{shot: PlacedShot; frame: number; pose: Pose}> = ({shot, frame, pose}) => {
	const o = shot.overlay!;
	const start = beats(o.beat);
	const end = beats(o.beat + o.beats);
	const inK = interpolate(frame, [start, start + 30], [0, 1], {...clamp, easing: expoOut});
	const outK = interpolate(frame, [end - 14, end], [0, 1], {...clamp, easing: Easing.in(Easing.cubic)});
	if (inK <= 0 || outK >= 1) return null;
	const k = inK * (1 - outK);
	const float = Math.sin((frame - start) / 22) * 6;
	const phoneX = 960 + pose.x;
	// Toward the frame's centre from the phone, so it sits between the phone and the headline.
	const dir = pose.x > 0 ? -1 : 1;
	const x = phoneX + dir * 300 * k;
	const y = 540 + pose.y - 10 * k + float;
	const w = 540;
	return (
		<div style={{position: 'absolute', inset: 0, perspective: 2200, pointerEvents: 'none'}}>
			<div
				style={{
					position: 'absolute',
					left: x - w / 2,
					top: y - (w * 743) / 595 / 2,
					width: w,
					transform: `scale(${0.45 + 0.55 * k}) rotateY(${dir * -14 * k}deg) rotateZ(${dir * -3 * k}deg)`,
					opacity: Math.min(1, k * 1.6),
					filter: `drop-shadow(0 40px 60px rgba(0,0,0,0.7)) drop-shadow(0 0 40px rgba(247,201,72,${0.25 * k}))`,
				}}
			>
				<Img src={staticFile(o.image)} style={{width: '100%', borderRadius: 18, display: 'block', boxShadow: '0 0 0 2px rgba(247,201,72,0.55)'}} />
			</div>
		</div>
	);
};
