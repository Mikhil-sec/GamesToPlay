import React from 'react';
import {AbsoluteFill, Img, staticFile} from 'remotion';
import {Background} from './components/Background';
import {StatusBar} from './components/ClipOrPlaceholder';
import {Phone} from './components/Phone';
import {parse} from './components/Text';
import {loadFonts} from './fonts';
import {Pose} from './pose';
import {C, F, RGB} from './theme';

loadFonts();

// Devpost gallery and thumbnail images, 3:2 at 1920×1280. Same look as the video. Every phone
// screen is a real capture from the live app.

export type GalleryProps = {id: string};

type PhoneSpec = {img: string; bar: string; pose: Pose};
type Slide = {
	kicker: string;
	title: string[];
	body: string[];
	phones: PhoneSpec[];
	rgb: readonly number[];
	textRight?: boolean;
};

const shot = (img: string, bar: string, pose: Partial<Pose>): PhoneSpec => ({
	img,
	bar,
	pose: {x: 0, y: 40, s: 1.15, ry: 0, rx: 4, rz: 0, ...pose},
});

const SLIDES: Record<string, Slide> = {
	'01_hero': {
		kicker: 'LIVE ON GOOGLE PLAY',
		title: ['THE GAMES YOU STARTED', '_DESERVE AN ENDING._'],
		body: ['A gaming backlog for Android that plays like an arcade cabinet.'],
		rgb: RGB.coin,
		phones: [
			shot('brand/raw_01_pile.png', '#101018', {x: 330, y: 60, s: 1.05, ry: 14, rz: -1.5}),
			shot('brand/raw_03_draw_card.png', '#101018', {x: 640, y: 30, s: 1.15, ry: -12, rz: 1.5}),
		],
	},
	'02_save': {
		kicker: 'SAVE · THE MATCHING ENGINE',
		title: ['SHARE IT FROM', 'ANY APP. *IT KNOWS*', '*THE GAME.*'],
		body: [
			'YouTube and TikTok links, captions, even screenshots.',
			'A 17,095-game index with alternative names matches them on the phone, offline.',
		],
		rgb: RGB.coin,
		phones: [shot('brand/gal_share.png', '#000000', {x: 520, s: 1.15, ry: -12, rz: 1.2})],
	},
	'03_decide': {
		kicker: 'DECIDE',
		title: ['A BACKLOG IS A', '*DECISION* PROBLEM.'],
		body: ['Set time, mood and genre. Pull the lever.', 'Three cards from your own pile, and why each one fits.'],
		rgb: RGB.coin,
		textRight: true,
		phones: [shot('brand/raw_03_draw_card.png', '#101018', {x: -520, s: 1.15, ry: 12, rz: -1.2})],
	},
	'04_continue': {
		kicker: 'CATVERTISING',
		title: ['THE AD *IS*', 'THE COIN.'],
		body: [
			'Every ad is opted into: a coin, or an hour of the real pro entitlement, verified by RevenueCat.',
			'No interstitials. No banners. Ever.',
		],
		rgb: RGB.hot,
		phones: [
			shot('brand/raw_04_gate.png', '#101018', {x: 330, y: 60, s: 1.05, ry: 14, rz: -1.5}),
			shot('brand/gal_freeplay.png', '#101018', {x: 640, y: 30, s: 1.15, ry: -12, rz: 1.5}),
		],
	},
	'05_complete_rate': {
		kicker: 'COMPLETE · RATE',
		title: ['ROLL THE CREDITS.', '*NO STARS.*'],
		body: ['Clearing a game rolls your own stats like film credits.', 'Head-to-head picks place it in your all-time ranking.'],
		rgb: RGB.neon,
		textRight: true,
		phones: [
			shot('brand/gal_credits.png', '#07080b', {x: -640, y: 30, s: 1.15, ry: 12, rz: -1.5}),
			shot('brand/gal_h2h.png', '#07080b', {x: -330, y: 60, s: 1.05, ry: -14, rz: 1.5}),
		],
	},
	'06_share': {
		kicker: 'SHARE · FRIENDS',
		title: ['FOLLOW A FRIEND\'S', '*PILE.* NO ACCOUNTS.'],
		body: [
			'Your whole pile as one signed link. Nothing is stored on a server.',
			'Games you have in common are marked.',
		],
		rgb: RGB.coin,
		phones: [
			shot('brand/raw_05_high_scores.png', '#101018', {x: 330, y: 60, s: 1.05, ry: 14, rz: -1.5}),
			shot('brand/gal_sam.png', '#101018', {x: 640, y: 30, s: 1.15, ry: -12, rz: 1.5}),
		],
	},
};

const Lines: React.FC<{lines: string[]; size: number}> = ({lines, size}) => (
	<div style={{fontFamily: F.display, fontWeight: 700, fontSize: size, lineHeight: 1.02, textTransform: 'uppercase'}}>
		{lines.map((l, i) => (
			<div key={i}>
				{parse(l).map((w, j) => (
					<span key={j} style={{color: w.color, textShadow: w.glow ? `0 0 ${size * 0.4}px ${w.color}55` : undefined}}>
						{w.text}{' '}
					</span>
				))}
			</div>
		))}
	</div>
);

const SlideView: React.FC<{s: Slide}> = ({s}) => {
	const right = !!s.textRight;
	const glowX = right ? 480 : 1440;
	return (
		<AbsoluteFill>
			<Background frame={0} glowX={glowX} rgb={s.rgb} pulse={0.5} gridSpeed={0} />
			<AbsoluteFill style={{transform: 'translateY(100px)'}}>
				{s.phones.map((p, i) => (
					<Phone key={i} pose={p.pose}>
						<Img src={staticFile(p.img)} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
						<StatusBar background={p.bar} />
					</Phone>
				))}
			</AbsoluteFill>
			<div
				style={{
					position: 'absolute',
					top: 0,
					bottom: 0,
					left: right ? 1000 : 110,
					width: 820,
					display: 'flex',
					flexDirection: 'column',
					justifyContent: 'center',
					gap: 34,
				}}
			>
				<div style={{fontFamily: F.mono, fontWeight: 700, fontSize: 30, letterSpacing: '0.2em', color: C.coin}}>{s.kicker}</div>
				<Lines lines={s.title} size={92} />
				<div style={{fontFamily: F.body, fontSize: 34, lineHeight: 1.4, color: C.text2, maxWidth: 760, display: 'flex', flexDirection: 'column', gap: 14}}>
					{s.body.map((b, i) => (
						<div key={i}>{b}</div>
					))}
				</div>
			</div>
			<div style={{position: 'absolute', left: right ? 1000 : 110, bottom: 60, fontFamily: F.display, fontWeight: 700, fontSize: 34, color: C.text3, letterSpacing: '0.08em'}}>
				CONTINUE<span style={{color: C.coin}}>?</span>
			</div>
		</AbsoluteFill>
	);
};

/** How the app, the Worker and the stores fit together. Drawn, not screenshotted. */
const Architecture: React.FC = () => {
	const box = (x: number, y: number, w: number, h: number, title: string, lines: string[], color: string) => (
		<div
			style={{
				position: 'absolute',
				left: x,
				top: y,
				width: w,
				height: h,
				borderRadius: 22,
				background: C.cabinet,
				boxShadow: `inset 0 0 0 2px ${color}66, 0 0 60px ${color}14`,
				padding: '26px 30px',
				display: 'flex',
				flexDirection: 'column',
				gap: 10,
			}}
		>
			<div style={{fontFamily: F.display, fontWeight: 700, fontSize: 34, color}}>{title}</div>
			{lines.map((l, i) => (
				<div key={i} style={{fontFamily: F.body, fontSize: 24, lineHeight: 1.35, color: C.text2}}>{l}</div>
			))}
		</div>
	);
	const arrow = (x1: number, y1: number, x2: number, y2: number, label: string) => {
		const mx = (x1 + x2) / 2;
		const my = (y1 + y2) / 2;
		return (
			<g>
				<line x1={x1} y1={y1} x2={x2} y2={y2} stroke={C.text3} strokeWidth={3} markerEnd="url(#ah)" />
				<text x={mx} y={my - 14} fill={C.text2} fontFamily="Inter" fontSize={22} textAnchor="middle">{label}</text>
			</g>
		);
	};
	return (
		<AbsoluteFill>
			<Background frame={0} glowX={960} rgb={RGB.coin} pulse={0.3} gridSpeed={0} />
			<div style={{position: 'absolute', left: 110, top: 80}}>
				<div style={{fontFamily: F.mono, fontWeight: 700, fontSize: 30, letterSpacing: '0.2em', color: C.coin}}>HOW IT'S BUILT</div>
				<Lines lines={['OFFLINE-FIRST. *NO SECRETS*', '*IN THE APP.*']} size={76} />
			</div>
			<svg width={1920} height={1280} style={{position: 'absolute', inset: 0}}>
				<defs>
					<marker id="ah" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
						<path d="M0,0 L12,6 L0,12 z" fill={C.text3} />
					</marker>
				</defs>
				{arrow(770, 640, 1090, 640, 'game data (cached)')}
				{arrow(1520, 870, 1520, 960, '')}
				{arrow(450, 870, 450, 960, '')}
			</svg>
			{box(110, 380, 660, 490, 'ANDROID APP', [
				'Kotlin · Jetpack Compose · Hilt',
				'Room is the source of truth: the pile,',
				'DRAW and matching work with no network',
				'17,095-game offline index + on-device OCR',
				'Share target · signed pile links (ECDSA)',
				'Only public-by-design keys in the APK',
			], C.coin)}
			{box(1090, 380, 720, 490, 'CLOUDFLARE WORKER', [
				'TypeScript · KV cache',
				'The only thing that talks to IGDB',
				'3 rate limiters: 120/min per IP, 20/min',
				'  resolve, 600/min global (under IGDB)',
				'SSRF-safe host allowlist · input caps',
				'Secrets live here only',
			], C.cool)}
			{box(110, 960, 660, 220, 'REVENUECAT', ['pro entitlement · COIN currency', 'Rewarded-ad verification + ad revenue'], C.neon)}
			{box(1090, 960, 720, 220, 'IGDB', ['Games, covers, time-to-beat', 'Data dumps build the offline index'], C.hot)}
		</AbsoluteFill>
	);
};

/** The Devpost card thumbnail: small on the gallery grid, so one idea, big type. */
const Thumb: React.FC = () => (
	<AbsoluteFill>
		<Background frame={0} glowX={1420} rgb={RGB.hot} pulse={0.6} gridSpeed={0} />
		<Phone pose={{x: 470, y: 80, s: 1.2, ry: -18, rx: 5, rz: 1.5}}>
			<Img src={staticFile('brand/raw_04_gate.png')} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
			<StatusBar background="#101018" />
		</Phone>
		<div style={{position: 'absolute', left: 110, top: 360, width: 1040}}>
			<div
				style={{
					fontFamily: F.display,
					fontWeight: 700,
					fontSize: 210,
					lineHeight: 0.9,
					color: C.text,
					textShadow: '-3px 0 rgba(255,61,127,0.6), 3px 0 rgba(0,229,160,0.5)',
				}}
			>
				CONTINUE<span style={{color: C.coin, textShadow: '0 0 60px rgba(247,201,72,0.6)'}}>?</span>
			</div>
			<div style={{height: 8, width: 220, background: C.coin, margin: '44px 0 36px'}} />
			<div style={{fontFamily: F.display, fontWeight: 700, fontSize: 66, lineHeight: 1.1, letterSpacing: '0.04em'}}>
				<div style={{color: C.text}}>YOUR GAME BACKLOG,</div>
				<div style={{color: C.coin}}>AS AN ARCADE.</div>
			</div>
		</div>
	</AbsoluteFill>
);

export const GalleryImage: React.FC<GalleryProps> = ({id}) => {
	if (id === '00_thumbnail') return <Thumb />;
	if (id === '07_architecture') return <Architecture />;
	return <SlideView s={SLIDES[id]} />;
};

export const GALLERY_IDS = ['00_thumbnail', ...Object.keys(SLIDES), '07_architecture'];
