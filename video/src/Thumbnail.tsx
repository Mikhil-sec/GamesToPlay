import React from 'react';
import {AbsoluteFill, Img, staticFile} from 'remotion';
import {Background} from './components/Background';
import {StatusBar} from './components/ClipOrPlaceholder';
import {Phone} from './components/Phone';
import {loadFonts} from './fonts';
import {C, F, RGB} from './theme';

loadFonts();

/**
 * The YouTube thumbnail (render at 1920×1080, upload as-is; YouTube scales it). No game art, per
 * the Devpost rules: the phone shows the app's own CONTINUE? gate.
 */
export const Thumbnail: React.FC = () => (
	<AbsoluteFill>
		<Background frame={0} glowX={1420} rgb={RGB.hot} pulse={0.6} gridSpeed={0} />
		<Phone pose={{x: 470, y: 30, s: 1.08, ry: -18, rx: 5, rz: 1.5}}>
			<Img src={staticFile('brand/thumb_gate.png')} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
			<StatusBar background="#101018" />
		</Phone>
		<div style={{position: 'absolute', left: 120, top: 250, width: 1000}}>
			<div
				style={{
					fontFamily: F.display,
					fontWeight: 700,
					fontSize: 200,
					lineHeight: 0.9,
					color: C.text,
					textShadow: '-3px 0 rgba(255,61,127,0.6), 3px 0 rgba(0,229,160,0.5)',
				}}
			>
				CONTINUE<span style={{color: C.coin, textShadow: '0 0 60px rgba(247,201,72,0.6)'}}>?</span>
			</div>
			<div style={{height: 8, width: 220, background: C.coin, margin: '44px 0 36px'}} />
			<div style={{fontFamily: F.display, fontWeight: 700, fontSize: 64, lineHeight: 1.1, letterSpacing: '0.04em'}}>
				<div style={{color: C.text}}>YOUR GAME BACKLOG,</div>
				<div style={{color: C.coin}}>AS AN ARCADE.</div>
			</div>
		</div>
	</AbsoluteFill>
);
