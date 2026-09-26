import React from 'react';
import {AbsoluteFill, Freeze, getStaticFiles, OffthreadVideo, staticFile, useCurrentFrame} from 'remotion';
import {C, F} from '../theme';
import {FPS} from '../timeline';

export const hasClip = (file: string) => getStaticFiles().some((f) => f.name === `clips/${file}`);

/**
 * A clean status bar painted over the recording's own (which shows the real time, notification
 * icons from other apps and the phone's battery). Same height as Android's, on the colour the
 * app draws behind it in that clip, so it reads as the phone's.
 */
export const StatusBar: React.FC<{background: string}> = ({background}) => (
	<div
		style={{
			position: 'absolute',
			left: 0,
			right: 0,
			top: 0,
			height: '4.4%',
			background,
			display: 'flex',
			alignItems: 'center',
			justifyContent: 'space-between',
			padding: '0 7% 0 8%',
			color: '#F2F4F8',
			fontFamily: F.body,
			fontWeight: 600,
			fontSize: 12.5,
		}}
	>
		<span>9:41</span>
		<svg width={52} height={12} viewBox="0 0 52 12" fill="#F2F4F8">
			{/* signal */}
			<rect x={0} y={8} width={2.4} height={4} rx={0.6} />
			<rect x={3.6} y={5.5} width={2.4} height={6.5} rx={0.6} />
			<rect x={7.2} y={3} width={2.4} height={9} rx={0.6} />
			<rect x={10.8} y={0.5} width={2.4} height={11.5} rx={0.6} />
			{/* wifi */}
			<path d="M22 11.5 L19.2 8.4 A4 4 0 0 1 24.8 8.4 Z" />
			<path d="M17.4 6.5 A6.6 6.6 0 0 1 26.6 6.5 L25.5 7.6 A5 5 0 0 0 18.5 7.6 Z" />
			<path d="M15.6 4.6 A9.2 9.2 0 0 1 28.4 4.6 L27.3 5.7 A7.6 7.6 0 0 0 16.7 5.7 Z" />
			{/* battery */}
			<rect x={33} y={1.5} width={16} height={9} rx={2} fill="none" stroke="#F2F4F8" strokeWidth={1.2} />
			<rect x={34.6} y={3.1} width={12.8} height={5.8} rx={1} />
			<rect x={49.6} y={4.2} width={1.6} height={3.6} rx={0.6} />
		</svg>
	</div>
);

/**
 * A recorded clip if it's in public/clips, otherwise a card saying exactly what to record and
 * for how long, so the draft video doubles as the shot list.
 */
export const ClipOrPlaceholder: React.FC<{
	file: string;
	from: number;
	seconds: number;
	volume: number;
	label: string;
	note?: string;
	fit?: 'cover' | 'contain';
	camera?: boolean;
	freezeAt?: number;
	bar?: string;
}> = ({file, from, seconds, volume, label, note, fit = 'cover', camera, freezeAt, bar}) => {
	const frame = useCurrentFrame();
	if (hasClip(file)) {
		const video = (
			<OffthreadVideo
				src={staticFile(`clips/${file}`)}
				trimBefore={Math.round(from * FPS)}
				volume={volume}
				style={{width: '100%', height: '100%', objectFit: fit}}
			/>
		);
		const hold = freezeAt === undefined ? null : Math.round(freezeAt * FPS);
		return (
			<AbsoluteFill>
				{hold !== null && frame >= hold ? <Freeze frame={hold}>{video}</Freeze> : video}
				{camera ? null : <StatusBar background={bar ?? '#101018'} />}
			</AbsoluteFill>
		);
	}
	const progress = Math.min(1, frame / (seconds * FPS));
	const pad = camera ? 120 : 36;
	return (
		<AbsoluteFill
			style={{
				background: `repeating-linear-gradient(135deg, ${C.cabinet} 0 22px, #0d0f14 22px 44px)`,
				padding: pad,
				justifyContent: 'center',
				gap: camera ? 28 : 18,
				color: C.text,
			}}
		>
			<div style={{fontFamily: F.mono, fontSize: camera ? 30 : 17, color: C.coin, letterSpacing: '0.12em'}}>
				{camera ? '📷 CAMERA SHOT · ' : 'RECORD · '}
				{label}
			</div>
			<div style={{fontFamily: F.display, fontWeight: 700, fontSize: camera ? 64 : 24, lineHeight: 1.05, wordBreak: 'break-all'}}>{file}</div>
			<div style={{fontFamily: F.mono, fontSize: camera ? 26 : 15, color: C.text2}}>
				needs {seconds.toFixed(1)} s from {from.toFixed(1)} s
			</div>
			{note ? (
				<div style={{fontFamily: F.body, fontSize: camera ? 30 : 16, lineHeight: 1.4, color: C.text2}}>{note}</div>
			) : null}
			<div style={{height: 4, background: C.outline, borderRadius: 2, marginTop: 8}}>
				<div style={{height: 4, width: `${progress * 100}%`, background: C.coin, borderRadius: 2}} />
			</div>
		</AbsoluteFill>
	);
};
