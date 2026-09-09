/**
 * One place to ask whether we are allowed to move things.
 *
 * Everything animated on this site is decoration over content that is already
 * correct without it, so the honest answer to `prefers-reduced-motion` is to
 * jump straight to the finished state rather than to a shorter animation.
 */
export function prefersReducedMotion() {
	if (typeof window === 'undefined' || !window.matchMedia) return false;
	return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Runs `fn` once, the first time `node` is meaningfully on screen. Falls back
 * to running immediately where IntersectionObserver is missing, because a
 * missing animation is worse than an unobserved one.
 *
 * @param {Element} node
 * @param {() => void} fn
 * @param {number} [ratio]
 * @returns {() => void} cleanup
 */
export function onceVisible(node, fn, ratio = 0.35) {
	if (typeof IntersectionObserver === 'undefined') {
		fn();
		return () => {};
	}

	const io = new IntersectionObserver(
		(entries) => {
			for (const entry of entries) {
				if (entry.isIntersecting) {
					io.disconnect();
					fn();
				}
			}
		},
		{ threshold: ratio }
	);

	io.observe(node);
	return () => io.disconnect();
}
