import React from 'react';
import {Easing, interpolate} from 'remotion';
import {C, F} from '../theme';
import {beats, Line} from '../timeline';

const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;
const expoOut = Easing.bezier(0.16, 1, 0.3, 1);

type Word = {text: string; color: string; glow: boolean};

/** `*gold*` and `_green_` in timeline text, split into words so each can animate on its own. */
export const parse = (text: string): Word[] => {
	const words: Word[] = [];
	const re = /\*([^*]+)\*|_([^_]+)_|([^*_]+)/g;
	let m: RegExpExecArray | null;
	while ((m = re.exec(text))) {
		const [color, glow, chunk] = m[1] !== undefined ? [C.coin, true, m[1]] : m[2] !== undefined ? [C.neon, true, m[2]] : [C.text, false, m[3]];
		for (const w of chunk.split(/\s+/).filter(Boolean)) words.push({text: w, color, glow});
	}
	return words;
};

/** One line of display type. Each word rises out of a mask, a few frames after the last. */
export const RevealLine: React.FC<{
	line: Line;
	frame: number;
	size: number;
	align?: 'left' | 'center';
}> = ({line, frame, size, align = 'left'}) => {
	const start = beats(line.at ?? 0);
	const words = parse(line.text);
	return (
		<div
			style={{
				display: 'flex',
				flexWrap: 'wrap',
				justifyContent: align === 'center' ? 'center' : 'flex-start',
				columnGap: size * 0.26,
				fontFamily: F.display,
				fontWeight: 700,
				fontSize: size,
				lineHeight: 1.02,
				letterSpacing: '0.01em',
				textTransform: 'uppercase',
			}}
		>
			{words.map((w, i) => {
				const f = frame - start - i * 3;
				const k = interpolate(f, [0, 22], [0, 1], {...clamp, easing: expoOut});
				return (
					<span key={i} style={{display: 'inline-block', overflow: 'hidden', paddingBottom: size * 0.08, marginBottom: -size * 0.08}}>
						<span
							style={{
								display: 'inline-block',
								transform: `translateY(${(1 - k) * 105}%)`,
								color: w.color,
								textShadow: w.glow ? `0 0 ${size * 0.45}px ${w.color}55` : undefined,
							}}
						>
							{w.text}
						</span>
					</span>
				);
			})}
		</div>
	);
};

/** "STAGE 1 · SAVE", typed out like an arcade attract screen, with a block cursor while it types. */
export const Kicker: React.FC<{text: string; frame: number; color?: string}> = ({text, frame, color = C.coin}) => {
	const shown = Math.max(0, Math.min(text.length, Math.floor(frame / 1.6)));
	const typing = shown < text.length || frame < text.length * 1.6 + 24;
	const cursorOn = Math.floor(frame / 8) % 2 === 0;
	return (
		<div style={{fontFamily: F.mono, fontWeight: 700, fontSize: 26, letterSpacing: '0.2em', color, height: 34}}>
			{text.slice(0, shown)}
			{typing && cursorOn ? <span style={{opacity: 0.9}}>▌</span> : null}
		</div>
	);
};

/** Big enough to wrap each line onto at most `rows` rows, but never splitting a word. */
export const fitSize = (lines: Line[], width: number, max: number, rows = 2) => {
	const plain = lines.map((l) => l.text.replace(/[*_]/g, ''));
	const longestLine = Math.max(...plain.map((t) => t.length));
	const longestWord = Math.max(...plain.flatMap((t) => t.split(/\s+/)).map((w) => w.length));
	return Math.floor(Math.min(max, width / (longestWord * 0.74), (width * rows) / (longestLine * 0.66)));
};

/** Headline block beside the phone: kicker, the lines, a subline, then out before the cut. */
export const Headline: React.FC<{
	frame: number;
	duration: number;
	side: 'left' | 'right';
	kicker?: string;
	lines: Line[];
	sub?: string;
	exitAt?: number;
}> = ({frame, duration, side, kicker, lines, sub, exitAt}) => {
	const width = 740;
	const size = fitSize(lines, width, 124);
	const end = exitAt ?? duration;
	const out = interpolate(frame, [end - 12, end], [0, 1], {...clamp, easing: Easing.in(Easing.cubic)});
	const subStart = beats(Math.max(...lines.map((l) => l.at ?? 0))) + 16;
	const subK = interpolate(frame - subStart, [0, 24], [0, 1], {...clamp, easing: expoOut});
	return (
		<div
			style={{
				position: 'absolute',
				top: 0,
				bottom: 0,
				left: side === 'left' ? 150 : 1920 - 150 - width,
				width,
				display: 'flex',
				flexDirection: 'column',
				justifyContent: 'center',
				gap: 22,
				opacity: 1 - out,
				transform: `translateY(${-out * 24}px)`,
			}}
		>
			{kicker ? <Kicker text={kicker} frame={frame} /> : null}
			<div style={{display: 'flex', flexDirection: 'column', gap: size * 0.06}}>
				{lines.map((l, i) => (
					<RevealLine key={i} line={l} frame={frame} size={size} />
				))}
			</div>
			{sub ? (
				<div
					style={{
						fontFamily: F.body,
						fontSize: 38,
						lineHeight: 1.3,
						color: C.text2,
						maxWidth: 620,
						opacity: subK,
						transform: `translateY(${(1 - subK) * 14}px)`,
					}}
				>
					{sub}
				</div>
			) : null}
		</div>
	);
};

/** Full-frame statement between sections: no phone, just the words. */
export const TitleCard: React.FC<{frame: number; duration: number; lines: Line[]}> = ({frame, duration, lines}) => {
	const size = fitSize(lines, 1560, 118, 1);
	const out = interpolate(frame, [duration - 10, duration], [0, 1], clamp);
	const push = interpolate(frame, [0, duration], [1, 1.05]);
	return (
		<div
			style={{
				position: 'absolute',
				inset: 0,
				display: 'flex',
				flexDirection: 'column',
				justifyContent: 'center',
				alignItems: 'center',
				gap: size * 0.14,
				opacity: 1 - out,
				transform: `scale(${push})`,
			}}
		>
			{lines.map((l, i) => (
				<RevealLine key={i} line={l} frame={frame} size={size} align="center" />
			))}
		</div>
	);
};
