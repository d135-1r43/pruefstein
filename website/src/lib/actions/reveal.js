import { prefersReducedMotion, onceVisible } from '$lib/motion.js';

/**
 * Svelte action: adds `is-in` the first time the node is on screen, so CSS can
 * settle it into place. Deliberately one-way: content that fades back out on
 * the way up is a nuisance, not an effect.
 *
 * The node starts visible and is only hidden once JavaScript has taken
 * responsibility for showing it again. Without JS, or with reduced motion, the
 * page simply renders finished.
 *
 * @param {HTMLElement} node
 * @param {{ delay?: number }} [options]
 */
export function reveal(node, options = {}) {
	if (prefersReducedMotion()) {
		node.classList.add('is-in');
		return {};
	}

	node.classList.add('reveal');
	if (options.delay) node.style.setProperty('--reveal-delay', `${options.delay}ms`);

	const stop = onceVisible(node, () => node.classList.add('is-in'), 0.08);

	return { destroy: stop };
}

/**
 * Svelte action: marks which nav link belongs to the section currently under
 * the reader, the way the application's sidebar marks the active page.
 *
 * @param {HTMLElement} node container holding the anchors
 */
export function scrollSpy(node) {
	let io = null;
	let cancelled = false;

	/**
	 * The nav mounts before the page it links into, so the sections do not
	 * exist yet on the first pass. Resolve on the next frame, and give it one
	 * more try after that before accepting there is nothing to track.
	 */
	function setup(attempt = 0) {
		if (cancelled) return;

		const links = Array.from(node.querySelectorAll('a[href*="#"]'));
		const targets = links
			.map((link) => {
				const id = (link.getAttribute('href') || '').split('#')[1];
				const el = id ? document.getElementById(id) : null;
				return el ? { link, el } : null;
			})
			.filter(Boolean);

		if (!targets.length) {
			if (attempt < 2) setTimeout(() => setup(attempt + 1), 120);
			return;
		}

		let active = null;
		const visible = new Set();

		function mark(link) {
			if (active === link) return;
			if (active) active.classList.remove('current');
			active = link;
			if (active) active.classList.add('current');
		}

		io = new IntersectionObserver(
			(entries) => {
				for (const entry of entries) {
					const hit = targets.find((t) => t.el === entry.target);
					if (!hit) continue;
					if (entry.isIntersecting) visible.add(hit);
					else visible.delete(hit);
				}
				// The topmost section still on screen is the one being read.
				const ordered = targets.filter((t) => visible.has(t));
				mark(ordered.length ? ordered[0].link : null);
			},
			{ rootMargin: '-72px 0px -55% 0px', threshold: 0 }
		);

		for (const t of targets) io.observe(t.el);
	}

	if (typeof IntersectionObserver === 'undefined') return {};

	requestAnimationFrame(() => setup());

	return {
		destroy() {
			cancelled = true;
			io?.disconnect();
		}
	};
}
