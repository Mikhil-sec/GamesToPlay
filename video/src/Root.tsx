import React from 'react';
import {Composition} from 'remotion';
import {Thumbnail} from './Thumbnail';
import {FPS, TOTAL_FRAMES} from './timeline';
import {Video} from './Video';

export const RemotionRoot: React.FC = () => (
	<>
		<Composition id="Continue" component={Video} durationInFrames={TOTAL_FRAMES} fps={FPS} width={1920} height={1080} />
		<Composition id="Thumbnail" component={Thumbnail} durationInFrames={1} fps={FPS} width={1920} height={1080} />
	</>
);
