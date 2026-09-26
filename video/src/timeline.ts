import raw from '../timeline.json';

export type Layout = 'cold' | 'left' | 'right' | 'center' | 'title' | 'camera' | 'end';

export type Line = {text: string; at?: number};
export type Clip = {file: string; from?: number; beats?: number; freezeAt?: number; bar?: string};
export type Sfx = {file: string; beat: number; volume?: number};
export type Push = {beat: number; scale: number; focusY: number; center?: boolean};
export type Overlay = {image: string; beat: number; beats: number};

export type Shot = {
	id: string;
	bars: number;
	music: string;
	layout: Layout;
	clips?: Clip[];
	clipVolume?: number;
	kicker?: string;
	lines?: Line[];
	sub?: string;
	push?: Push;
	sfx?: Sfx[];
	overlay?: Overlay;
	note?: string;
};

export type EndCardText = {tagline: string; store: string; repo: string; credits: string};

type Timeline = {
	bpm: number;
	fps: number;
	phoneAspect: number;
	shots: Shot[];
	endCard: EndCardText;
};

export const TL = raw as unknown as Timeline;
export const FPS = TL.fps;
/** Frames per beat. Not an integer (60 fps at 104 bpm is 34.6), so always round at the use site. */
export const BEAT = (FPS * 60) / TL.bpm;
export const beats = (b: number) => Math.round(b * BEAT);

export type PlacedShot = Shot & {index: number; start: number; duration: number; startBeat: number};

export const SHOTS: PlacedShot[] = (() => {
	let beat = 0;
	return TL.shots.map((s, index) => {
		const placed = {
			...s,
			index,
			startBeat: beat,
			start: beats(beat),
			duration: beats(beat + s.bars * 4) - beats(beat),
		};
		beat += s.bars * 4;
		return placed;
	});
})();

/** The score runs ~3 s past the last bar so the final chord can ring out under the end card. */
export const TOTAL_FRAMES = SHOTS[SHOTS.length - 1].start + SHOTS[SHOTS.length - 1].duration + FPS;

export const shotAt = (frame: number): PlacedShot => {
	for (let i = SHOTS.length - 1; i >= 0; i--) {
		if (frame >= SHOTS[i].start) return SHOTS[i];
	}
	return SHOTS[0];
};

/** Clips inside a shot, each with its frame offset and length. Clips without `beats` share the rest. */
export const placeClips = (shot: PlacedShot) => {
	const clips = shot.clips ?? [];
	const fixed = clips.reduce((n, c) => n + (c.beats ?? 0), 0);
	const flexible = clips.filter((c) => c.beats === undefined).length;
	const each = flexible ? (shot.bars * 4 - fixed) / flexible : 0;
	let beat = 0;
	return clips.map((c) => {
		const len = c.beats ?? each;
		const placed = {...c, offset: beats(beat), length: beats(beat + len) - beats(beat), beatsLong: len};
		beat += len;
		return placed;
	});
};

export const PHONE_VISIBLE: Record<Layout, boolean> = {
	cold: true, left: true, right: true, center: true, title: false, camera: false, end: false,
};

/** Sections with drums, where the background pulses on the beat. */
export const DRUMS = new Set(['drop', 'drop2', 'groove', 'free', 'victory', 'continue']);
