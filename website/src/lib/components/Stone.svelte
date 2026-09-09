<script>
	import { base } from '$app/paths';
	import { prefersReducedMotion } from '$lib/motion.js';

	/**
	 * The app's decorative monolith, placed deliberately instead of randomly.
	 * `mix-blend-mode: multiply` drops the artwork's white ground onto whatever
	 * band it sits in, which is how the application uses it too.
	 *
	 * Purely ornamental: hidden from assistive tech, hidden on small screens,
	 * and never in the way of a pointer.
	 */
	let {
		n = 1,
		side = 'right',
		width = '32vw',
		top = 'auto',
		bottom = '0',
		opacity = 0.9,
		flip = false,
		eager = false,
		parallax = 40
	} = $props();

	let node = $state(null);
	let shift = $state(0);

	/**
	 * Moves the monolith against the scroll, a little. Reads are throttled onto
	 * an animation frame so a scroll never waits on layout, and the whole thing
	 * is skipped when the element is off screen or motion is unwelcome.
	 */
	$effect(() => {
		if (!node || prefersReducedMotion()) return;

		let ticking = false;
		let visible = false;

		const io =
			typeof IntersectionObserver === 'undefined'
				? null
				: new IntersectionObserver((entries) => {
						visible = entries.some((e) => e.isIntersecting);
					});
		io?.observe(node);

		function update() {
			ticking = false;
			if (!visible) return;
			const rect = node.getBoundingClientRect();
			// -1 above the fold, +1 below it
			const progress = (rect.top + rect.height / 2 - window.innerHeight / 2) / window.innerHeight;
			shift = Math.max(-1, Math.min(1, progress)) * parallax;
		}

		function onScroll() {
			if (ticking) return;
			ticking = true;
			requestAnimationFrame(update);
		}

		window.addEventListener('scroll', onScroll, { passive: true });
		window.addEventListener('resize', onScroll, { passive: true });
		update();

		return () => {
			window.removeEventListener('scroll', onScroll);
			window.removeEventListener('resize', onScroll);
			io?.disconnect();
		};
	});
</script>

<picture
	class="stone {side}"
	style:width
	style:top
	style:bottom
	style:opacity
	style:--flip={flip ? -1 : 1}
	style:--shift="{shift}px"
	bind:this={node}
	aria-hidden="true"
>
	<source srcset="{base}/art/stone-{n}.webp" type="image/webp" />
	<img
		src="{base}/art/stone-{n}.jpg"
		alt=""
		loading={eager ? 'eager' : 'lazy'}
		decoding="async"
		width="1200"
		height="1200"
	/>
</picture>

<style>
	.stone {
		position: absolute;
		display: none;
		pointer-events: none;
		user-select: none;
		mix-blend-mode: multiply;
		z-index: 0;
		transform: translateY(var(--shift, 0px)) scaleX(var(--flip, 1));
		will-change: transform;
	}

	.right {
		right: -4%;
	}
	.left {
		left: -6%;
	}

	.stone img {
		width: 100%;
		height: auto;
	}

	@media (min-width: 1024px) {
		.stone {
			display: block;
		}
	}
</style>
