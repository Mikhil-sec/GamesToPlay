import React from 'react';
import {Easing, getStaticFiles, Img, interpolate, staticFile} from 'remotion';
import {C, F} from '../theme';
import {beats, EndCardText} from '../timeline';

const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;
const expoOut = Easing.bezier(0.16, 1, 0.3, 1);
const BADGE = 'brand/google-play-badge.png';

const fadeUp = (frame: number, at: number) => {
	const k = interpolate(frame - at, [0, 24], [0, 1], {...clamp, easing: expoOut});
	return {opacity: k, transform: `translateY(${(1 - k) * 18}px)`};
};

/**
 * The store's feature graphic brought to life: CONTINUE? types on with a CRT flicker, the coin
 * falls into the slot on the score's final chord (beat 4), then the tagline and where to get it.
 */
export const EndCard: React.FC<{frame: number; text: EndCardText}> = ({frame, text}) => {
	const land = beats(4);
	const title = 'CONTINUE';
	const shown = Math.min(title.length, Math.floor(frame / 2.2));
	const flicker = frame < 26 ? (Math.floor(frame / 2) % 3 === 0 ? 0.55 : 1) : 1;
	const q = interpolate(frame, [title.length * 2.2, title.length * 2.2 + 10], [0, 1], clamp);

	// Coin: falls from above the frame and disappears into the slot on the downbeat.
	const fallStart = land - 26;
	const fall = interpolate(frame, [fallStart, land], [0, 1], {...clamp, easing: Easing.in(Easing.quad)});
	const coinY = -520 + fall * 580;
	const flash = interpolate(frame - land, [0, 4, 40], [0, 1, 0], clamp);
	const hasBadge = getStaticFiles().some((f) => f.name === BADGE);

	return (
		<div style={{position: 'absolute', inset: 0}}>
			<div style={{position: 'absolute', left: 190, top: 250, display: 'flex', flexDirection: 'column', gap: 26}}>
				<div
					style={{
						fontFamily: F.display,
						fontWeight: 700,
						fontSize: 190,
						lineHeight: 0.9,
						letterSpacing: '0.03em',
						color: C.text,
						opacity: flicker,
						// Chromatic aberration: the one place the design system allows it.
						textShadow: '-2px 0 rgba(255,61,127,0.55), 2px 0 rgba(0,229,160,0.45)',
					}}
				>
					{title.slice(0, shown)}
					<span style={{color: C.coin, opacity: q, textShadow: `0 0 ${40 + flash * 60}px rgba(247,201,72,${0.45 + flash * 0.4})`}}>?</span>
				</div>
				<div
					style={{
						height: 6,
						width: interpolate(frame - land, [0, 30], [0, 190], {...clamp, easing: expoOut}),
						background: C.coin,
					}}
				/>
				<div style={{...fadeUp(frame, land + 4), fontFamily: F.display, fontWeight: 600, fontSize: 40, letterSpacing: '0.14em', lineHeight: 1.35}}>
					<div style={{color: C.text2}}>THE GAMES YOU STARTED</div>
					<div style={{color: C.neon}}>DESERVE AN ENDING</div>
				</div>
			</div>

			{/* Coin and slot, as in the app icon */}
			<div style={{position: 'absolute', left: 1400, top: 520, width: 300, height: 200}}>
				<div
					style={{
						position: 'absolute',
						left: 0,
						right: 0,
						top: -700,
						height: 700 + 99, // clipped at the slot's mouth; the slot is drawn over the rest
						overflow: 'hidden',
					}}
				>
					<div
						style={{
							position: 'absolute',
							left: 150 - 85,
							top: 700 + coinY,
							width: 170,
							height: 170,
							borderRadius: '50%',
							background: 'radial-gradient(circle at 38% 32%, #FFE08A 0%, #F7C948 45%, #D9A92C 100%)',
							boxShadow: '0 0 0 5px rgba(120,90,20,0.8), 0 0 60px rgba(247,201,72,0.45)',
						}}
					>
						<div style={{position: 'absolute', inset: 48, borderRadius: '50%', background: 'rgba(190,140,30,0.55)'}} />
					</div>
				</div>
				<div
					style={{
						position: 'absolute',
						left: 0,
						top: 44,
						width: 300,
						height: 110,
						borderRadius: 26,
						background: C.raised,
						boxShadow: `inset 0 0 0 2px ${C.outline}, 0 0 ${flash * 90}px rgba(247,201,72,${flash * 0.6})`,
					}}
				>
					<div
						style={{
							position: 'absolute',
							left: 75,
							right: 75,
							top: 40,
							height: 30,
							borderRadius: 8,
							background: '#050608',
							boxShadow: `0 0 ${flash * 30}px rgba(247,201,72,${flash})`,
						}}
					/>
				</div>
			</div>

			<div
				style={{
					...fadeUp(frame, land + 26),
					position: 'absolute',
					left: 190,
					right: 190,
					bottom: 110,
					display: 'flex',
					alignItems: 'center',
					gap: 36,
				}}
			>
				{hasBadge ? (
					<Img src={staticFile(BADGE)} style={{height: 84}} />
				) : (
					<div style={{fontFamily: F.display, fontWeight: 700, fontSize: 34, letterSpacing: '0.08em', color: C.text}}>
						{text.store.toUpperCase()}
					</div>
				)}
				<div style={{fontFamily: F.mono, fontSize: 26, color: C.text2}}>{text.repo}</div>
			</div>
			<div
				style={{
					...fadeUp(frame, land + 40),
					position: 'absolute',
					left: 190,
					bottom: 58,
					fontFamily: F.body,
					fontSize: 19,
					color: C.text3,
					letterSpacing: '0.02em',
				}}
			>
				{text.credits}
			</div>
		</div>
	);
};
