import {continueRender, delayRender, staticFile} from 'remotion';

// The exact font files the app bundles (app/src/main/res/font), so type in the video matches the
// type on the phone.
const FACES: [string, string, string][] = [
	['Chakra Petch', 'chakrapetch_regular.ttf', '400'],
	['Chakra Petch', 'chakrapetch_semibold.ttf', '600'],
	['Chakra Petch', 'chakrapetch_bold.ttf', '700'],
	['Inter', 'inter_variable.ttf', '100 900'],
	['JetBrains Mono', 'jetbrainsmono_variable.ttf', '100 900'],
];

let started = false;

export const loadFonts = () => {
	if (started) return;
	started = true;
	const handle = delayRender('Loading fonts');
	Promise.all(
		FACES.map(([family, file, weight]) =>
			new FontFace(family, `url(${staticFile(`fonts/${file}`)})`, {weight})
				.load()
				.then((face) => document.fonts.add(face)),
		),
	)
		.then(() => continueRender(handle))
		.catch((err) => {
			console.error(err);
			continueRender(handle);
		});
};
