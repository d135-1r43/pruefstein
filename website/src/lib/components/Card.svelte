<script>
	/**
	 * The app's surface: white plate, 2px rule, hard shadow. `href` turns it
	 * into a link that presses, exactly like the dashboard's KPI tiles.
	 */
	let {
		tag = '',
		title = '',
		href = undefined,
		tone = 'white',
		external = false,
		children,
		...rest
	} = $props();
</script>

<svelte:element
	this={href ? 'a' : 'article'}
	{href}
	target={external ? '_blank' : undefined}
	rel={external ? 'noreferrer noopener' : undefined}
	class="card {tone}"
	class:linked={href}
	{...rest}
>
	{#if tag}<p class="eyebrow tag">{tag}</p>{/if}
	{#if title}<h3>{title}</h3>{/if}
	{#if children}<div class="body">{@render children()}</div>{/if}
</svelte:element>

<style>
	.card {
		display: flex;
		flex-direction: column;
		padding: clamp(1.35rem, 2.4vw, 1.75rem);
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow);
		text-decoration: none;
		color: inherit;
		height: 100%;
	}

	.white {
		background: var(--surface);
	}
	.accent {
		background: var(--accent);
	}
	.dark {
		background: var(--ink);
		color: #fff;
	}

	.linked {
		transition:
			box-shadow 0.1s linear,
			transform 0.1s linear;
	}
	.linked:hover,
	.linked:focus-visible {
		box-shadow: var(--shadow-sm);
		transform: translate(2px, 2px);
	}
	.linked:active {
		box-shadow: none;
		transform: translate(4px, 4px);
	}

	/* The label picks up the accent as the pointer arrives */
	.tag {
		display: inline-block;
		transition:
			background-color 0.12s linear,
			color 0.12s linear,
			box-shadow 0.12s linear;
	}

	.white:hover .tag {
		background: var(--accent);
		color: #422006;
		box-shadow: 0 0 0 4px var(--accent);
	}

	.linked:hover h3 {
		text-decoration: underline;
		text-decoration-thickness: 2px;
		text-underline-offset: 3px;
	}

	@media (prefers-reduced-motion: reduce) {
		.linked,
		.tag {
			transition: none;
		}
	}

	.tag {
		color: var(--muted);
		margin-bottom: 0.85rem;
	}
	.accent .tag {
		color: #713f12;
	}
	.dark .tag {
		color: var(--accent);
	}

	h3 {
		font-size: 1.2rem;
		line-height: 1.15;
	}

	.body {
		margin-top: 0.75rem;
		font-size: 0.94rem;
		color: var(--muted);
		line-height: 1.65;
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	/* Anything a caller marks as a footer line drops to the bottom */
	.body :global(.cta) {
		margin-top: auto;
	}

	.accent .body {
		color: #422006;
	}
	.dark .body {
		color: #d6d3d1;
	}
</style>
