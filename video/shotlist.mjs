// Writes SHOTLIST.md from timeline.json: every clip to record, what to do, and how long it must
// run. Re-run (`npm run shotlist`) after editing the timeline so the list never drifts from the edit.
import fs from 'node:fs';

const tl = JSON.parse(fs.readFileSync(new URL('./timeline.json', import.meta.url), 'utf8'));
const spb = 60 / tl.bpm;
const have = new Set(fs.existsSync('public/clips') ? fs.readdirSync('public/clips') : []);

const rows = [];
let t = 0;
for (const shot of tl.shots) {
	const clips = shot.clips ?? [];
	const fixed = clips.reduce((n, c) => n + (c.beats ?? 0), 0);
	const flex = clips.filter((c) => c.beats === undefined).length;
	for (const c of clips) {
		const beats = c.beats ?? (shot.bars * 4 - fixed) / flex;
		const secs = beats * spb;
		rows.push(
			`| ${have.has(c.file) ? '✅' : '☐'} | \`${c.file}\` | ${shot.layout === 'camera' ? '📷 camera' : 'scrcpy'} | ` +
				`${Math.floor(t / 60)}:${(t % 60).toFixed(0).padStart(2, '0')} | **${Math.ceil(secs + 2)} s** | ${shot.note ?? ''} |`,
		);
	}
	t += shot.bars * 4 * spb;
}

const md = `# Shot list (generated from timeline.json: edit that, then \`npm run shotlist\`)

Total length: ${Math.floor(t / 60)}:${(t % 60).toFixed(1).padStart(4, '0')}. "Min length" is what the edit uses plus 2 s of
handles. Record each one longer than that, and do 3 takes. Put the chosen take in \`public/clips/\` under
exactly this name. If the good part starts later in the clip, set that clip's \`from\` (seconds) in timeline.json.

| | File | Capture | In video at | Min length | What to do |
|---|---|---|---|---|---|
${rows.join('\n')}
`;
fs.writeFileSync('SHOTLIST.md', md);
console.log(`SHOTLIST.md: ${rows.length} clips, ${rows.filter((r) => r.startsWith('| ✅')).length} recorded`);
