<script>
	/**
	 * The app's button: 2px rule, hard shadow, presses into it on hover.
	 * Renders an <a> when given an href, a <button> otherwise.
	 */
	let {
		href = undefined,
		variant = 'accent',
		size = 'md',
		external = false,
		children,
		...rest
	} = $props();

	const target = $derived(external ? '_blank' : undefined);
	const rel = $derived(external ? 'noreferrer noopener' : undefined);
</script>

{#if href}
	<a {href} {target} {rel} class="btn {variant} {size}" {...rest}>
		{@render children()}
	</a>
{:else}
	<button type="button" class="btn {variant} {size}" {...rest}>
		{@render children()}
	</button>
{/if}

<style>
	.btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		gap: 0.55rem;
		font-family: inherit;
		font-weight: 700;
		letter-spacing: 0.02em;
		text-decoration: none;
		cursor: pointer;
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow);
		background: var(--surface);
		color: var(--ink);
		transition:
			box-shadow 0.08s linear,
			transform 0.08s linear,
			background-color 0.08s linear;
	}

	.btn:hover,
	.btn:focus-visible {
		box-shadow: var(--shadow-sm);
		transform: translate(2px, 2px);
	}

	/* All the way down on the click itself */
	.btn:active {
		box-shadow: none;
		transform: translate(4px, 4px);
	}

	@media (prefers-reduced-motion: reduce) {
		.btn {
			transition: none;
		}
	}

	.md {
		padding: 0.75rem 1.35rem;
		font-size: 0.95rem;
	}

	.sm {
		padding: 0.45rem 0.9rem;
		font-size: 0.8rem;
	}

	.accent {
		background: var(--accent);
	}

	.dark {
		background: var(--ink);
		color: #fff;
	}
	.dark:hover {
		background: #1c1917;
	}

	.plain:hover {
		background: var(--accent);
	}
</style>
