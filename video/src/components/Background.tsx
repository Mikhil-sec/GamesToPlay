import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C} from '../theme';

/**
 * One background for the whole video: near-black, a glow behind the phone in the colour the app
 * uses for that moment, a synthwave floor grid that pulses on the kick, scanlines and a vignette.
 */
export const Background: React.FC<{
	frame: number;
	glowX: number;
	rgb: readonly number[];
	pulse: number;
	gridSpeed: number;
}> = ({frame, glowX, rgb, pulse, gridSpeed}) => {
	const [r, g, b] = rgb;
	const horizon = 650;
	const phase = ((frame * gridSpeed) / 90) % 1;
	const alpha = 0.11 + pulse * 0.08;

	const hLines: React.ReactNode[] = [];
	for (let k = 0; k < 16; k++) {
		const z = k + 1 - phase;
		if (z <= 0.05) continue;
		const y = horizon + 520 / z;
		if (y > 1100) continue;
		const fade = Math.min(1, (y - horizon) / 220);
		hLines.push(
			<line key={`h${k}`} x1={0} x2={1920} y1={y} y2={y}
				stroke={`rgba(${r},${g},${b},${alpha * fade})`} strokeWidth={1.5} />,
		);
	}
	const vLines: React.ReactNode[] = [];
	for (let i = -24; i <= 24; i++) {
		vLines.push(
			<line key={`v${i}`} x1={960 + i * 40} y1={horizon} x2={960 + i * 380} y2={1080}
				stroke={`rgba(${r},${g},${b},${alpha * 0.8})`} strokeWidth={1.2} />,
		);
	}

	return (
		<AbsoluteFill style={{backgroundColor: C.void}}>
			{/* The glow sits behind the phone and follows it, so light always comes from the product. */}
			<AbsoluteFill
				style={{
					background: `radial-gradient(ellipse 900px 700px at ${glowX}px 520px, rgba(${r},${g},${b},${0.22 + pulse * 0.05}) 0%, rgba(${r},${g},${b},0.07) 42%, transparent 75%)`,
				}}
			/>
			<svg width={1920} height={1080} style={{position: 'absolute', inset: 0}}>
				<defs>
					<linearGradient id="floorFade" x1="0" y1="0" x2="0" y2="1">
						<stop offset="0" stopColor="white" stopOpacity="0" />
						<stop offset="0.25" stopColor="white" stopOpacity="1" />
					</linearGradient>
					<mask id="floorMask">
						<rect x={0} y={horizon} width={1920} height={1080 - horizon} fill="url(#floorFade)" />
					</mask>
				</defs>
				<g mask="url(#floorMask)">
					{vLines}
					{hLines}
				</g>
				<line x1={0} x2={1920} y1={horizon} y2={horizon} stroke={`rgba(${r},${g},${b},0.10)`} strokeWidth={1} />
			</svg>
			<AbsoluteFill
				style={{
					backgroundImage:
						'repeating-linear-gradient(0deg, rgba(255,255,255,0.022) 0px, rgba(255,255,255,0.022) 1px, transparent 1px, transparent 3px)',
				}}
			/>
			<AbsoluteFill
				style={{background: 'radial-gradient(ellipse 120% 100% at 50% 45%, transparent 55%, rgba(0,0,0,0.65) 100%)'}}
			/>
		</AbsoluteFill>
	);
};
