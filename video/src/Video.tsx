import React from 'react';
import {Audio} from '@remotion/media';
import {AbsoluteFill, interpolate, Sequence, staticFile, useCurrentFrame} from 'remotion';
import {Background} from './components/Background';
import {ClipOrPlaceholder} from './components/ClipOrPlaceholder';
import {EndCard} from './components/EndCard';
import {Phone} from './components/Phone';
import {Headline, TitleCard} from './components/Text';
import {loadFonts} from './fonts';
import {FlyingCard} from './components/FlyingCard';
import {phonePoseAt} from './pose';
import {RGB} from './theme';
import {BEAT, beats, DRUMS, FPS, PHONE_VISIBLE, placeClips, PlacedShot, shotAt, SHOTS, TL, TOTAL_FRAMES} from './timeline';

loadFonts();

const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

/** The app's meaning for each colour: magenta is the CONTINUE? gate, green is a cleared game. */
const glowFor = (s: PlacedShot): readonly number[] =>
	s.music === 'continue' ? RGB.hot : s.music === 'victory' ? RGB.neon : RGB.coin;

const PhoneScreen: React.FC<{shot: PlacedShot}> = ({shot}) => {
	const placed = placeClips(shot);
	const next = SHOTS[shot.index + 1];
	// If the phone flies out after this shot, keep the last clip on screen while it goes.
	const tail = next && !PHONE_VISIBLE[next.layout] ? 24 : 0;
	return (
		<>
			{placed.map((c, i) => (
				<Sequence
					key={c.file}
					from={c.offset}
					durationInFrames={c.length + (i === placed.length - 1 ? tail : 0)}
					layout="absolute-fill"
				>
					<ClipOrPlaceholder
						file={c.file}
						from={c.from ?? 0}
						seconds={c.length / FPS}
						volume={shot.clipVolume ?? 0}
						label={`${shot.id.toUpperCase()}${placed.length > 1 ? ` ${i + 1}/${placed.length}` : ''}`}
						note={placed.length === 1 ? shot.note : undefined}
						freezeAt={c.freezeAt}
						bar={c.bar}
					/>
				</Sequence>
			))}
		</>
	);
};

export const Video: React.FC = () => {
	const frame = useCurrentFrame();
	const shot = shotAt(frame);
	const f = frame - shot.start;
	const prev = shot.index > 0 ? SHOTS[shot.index - 1] : null;
	const pose = phonePoseAt(frame);

	const k = interpolate(f, [0, 30], [0, 1], clamp);
	const to = glowFor(shot);
	const from = prev ? glowFor(prev) : to;
	const rgb = to.map((v, i) => Math.round(from[i] + (v - from[i]) * k));
	const glowX = shot.layout === 'end' ? 1550 : pose ? 960 + pose.x : 960;
	const drums = DRUMS.has(shot.music);
	const pulse = drums ? Math.exp(-(f % BEAT) / 7) : 0;

	const fadeIn = interpolate(frame, [0, 12], [1, 0], clamp);
	const fadeOut = interpolate(frame, [TOTAL_FRAMES - 50, TOTAL_FRAMES - 5], [0, 1], clamp);

	return (
		<AbsoluteFill>
			<Background frame={frame} glowX={glowX} rgb={rgb} pulse={pulse} gridSpeed={drums ? 1 : 0.35} />

			{pose ? (
				<Phone pose={pose}>
					{SHOTS.filter((s) => PHONE_VISIBLE[s.layout]).map((s) => (
						<Sequence key={s.id} from={s.start} durationInFrames={s.duration + 24} layout="absolute-fill">
							{frame < s.start + s.duration || !SHOTS[s.index + 1] || !PHONE_VISIBLE[SHOTS[s.index + 1].layout] ? (
								<PhoneScreen shot={s} />
							) : null}
						</Sequence>
					))}
				</Phone>
			) : null}

			{shot.overlay && pose ? <FlyingCard shot={shot} frame={f} pose={pose} /> : null}

			{(shot.layout === 'left' || shot.layout === 'right') && shot.lines ? (
				<Headline
					frame={f}
					duration={shot.duration}
					side={shot.layout === 'right' ? 'left' : 'right'}
					kicker={shot.kicker}
					lines={shot.lines}
					sub={shot.sub}
					exitAt={shot.push?.center ? beats(shot.push.beat) : undefined}
				/>
			) : null}

			{shot.layout === 'title' && shot.lines ? <TitleCard frame={f} duration={shot.duration} lines={shot.lines} /> : null}

			{SHOTS.filter((s) => s.layout === 'camera').map((s) => (
				<Sequence key={s.id} from={s.start} durationInFrames={s.duration} layout="absolute-fill">
					<AbsoluteFill style={{transform: `scale(${interpolate(frame - s.start, [0, s.duration], [1, 1.06])})`}}>
						<ClipOrPlaceholder
							file={s.clips?.[0]?.file ?? 'missing.mp4'}
							from={s.clips?.[0]?.from ?? 0}
							seconds={s.duration / FPS}
							volume={s.clipVolume ?? 0}
							label={s.id.toUpperCase()}
							note={s.note}
							camera
						/>
					</AbsoluteFill>
					<AbsoluteFill style={{background: 'radial-gradient(ellipse at center, transparent 50%, rgba(0,0,0,0.7) 100%)'}} />
				</Sequence>
			))}

			{shot.layout === 'end' ? <EndCard frame={f} text={TL.endCard} /> : null}

			<AbsoluteFill style={{backgroundColor: '#000', opacity: Math.max(fadeIn, fadeOut), pointerEvents: 'none'}} />

			{/* Score + every shot's sfx, pre-mixed and limited by tools/make_video_score.py (npm run score). */}
			<Audio src={staticFile('audio/mix.wav')} />

		</AbsoluteFill>
	);
};
