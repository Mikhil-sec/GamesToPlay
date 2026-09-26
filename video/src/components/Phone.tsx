import React from 'react';
import {Pose, PHONE_H} from '../pose';
import {TL} from '../timeline';

/**
 * A generic phone, drawn in CSS: no manufacturer's shape or logo, so no one else's trademark.
 * Whatever is passed as children fills the screen.
 */
export const Phone: React.FC<{pose: Pose; children: React.ReactNode}> = ({pose, children}) => {
	const H = PHONE_H;
	const W = Math.round(H * TL.phoneAspect);
	const bezel = 11;
	return (
		<div style={{position: 'absolute', inset: 0, perspective: 2600, perspectiveOrigin: '50% 45%'}}>
			<div
				style={{
					position: 'absolute',
					left: 960 - W / 2,
					top: 540 - H / 2,
					width: W,
					height: H,
					transform: `translate3d(${pose.x}px, ${pose.y}px, 0) scale(${pose.s}) rotateY(${pose.ry}deg) rotateX(${pose.rx}deg) rotateZ(${pose.rz}deg)`,
					transformStyle: 'preserve-3d',
				}}
			>
				{/* Frame: dark metal with a hairline highlight, light from above like the app's cards. */}
				<div
					style={{
						position: 'absolute',
						inset: 0,
						borderRadius: 58,
						background: 'linear-gradient(160deg, #2b2f3a 0%, #121419 35%, #0b0c10 70%, #1d2029 100%)',
						boxShadow:
							'0 70px 140px rgba(0,0,0,0.7), 0 20px 50px rgba(0,0,0,0.5), inset 0 0 0 1.5px rgba(255,255,255,0.10), inset 0 2px 0 rgba(255,255,255,0.12)',
					}}
				/>
				{/* Side buttons */}
				<div style={{position: 'absolute', right: -3, top: H * 0.2, width: 4, height: 70, borderRadius: 2, background: '#23262e'}} />
				<div style={{position: 'absolute', right: -3, top: H * 0.31, width: 4, height: 110, borderRadius: 2, background: '#23262e'}} />
				<div
					style={{
						position: 'absolute',
						inset: bezel,
						borderRadius: 48,
						overflow: 'hidden',
						background: '#000',
						transform: 'translateZ(0)',
					}}
				>
					{children}
					{/* Glass: a soft diagonal sheen, very low so it never fights the content. */}
					<div
						style={{
							position: 'absolute',
							inset: 0,
							background: 'linear-gradient(118deg, rgba(255,255,255,0.07) 0%, rgba(255,255,255,0.015) 38%, transparent 55%)',
							pointerEvents: 'none',
						}}
					/>
					<div
						style={{
							position: 'absolute',
							top: 16,
							left: '50%',
							width: 15,
							height: 15,
							marginLeft: -7.5,
							borderRadius: '50%',
							background: '#050507',
							boxShadow: 'inset 0 0 0 2px #15171c',
						}}
					/>
				</div>
			</div>
		</div>
	);
};
