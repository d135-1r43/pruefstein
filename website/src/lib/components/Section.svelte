<script>
	import { reveal } from '$lib/actions/reveal.js';

	/**
	 * Page section: optional eyebrow / heading / lede, then whatever the caller
	 * renders. `tone` swaps the band background; every band keeps the black rule
	 * between it and the next so the page reads as stacked plates.
	 */
	let { id = undefined, eyebrow = '', title = '', lede = '', tone = 'paper', children } = $props();
</script>

<section {id} class="band {tone}">
	<div class="shell inner">
		{#if eyebrow || title || lede}
			<header class="head" use:reveal>
				{#if eyebrow}<p class="eyebrow tag">{eyebrow}</p>{/if}
				{#if title}<h2>{title}</h2>{/if}
				{#if lede}<p class="lede">{lede}</p>{/if}
			</header>
		{/if}
		<div class="content" use:reveal={{ delay: 90 }}>
			{@render children()}
		</div>
	</div>
</section>

<style>
	.band {
		border-top: var(--rule) solid var(--ink);
		position: relative;
	}

	.paper {
		background: var(--paper);
	}
	.white {
		background: var(--surface);
	}
	.dark {
		background: var(--ink);
		color: #fff;
	}
	.accent {
		background: var(--accent);
	}

	.inner {
		padding-block: clamp(3.5rem, 8vw, 6.5rem);
		position: relative;
		z-index: 1;
	}

	.head {
		max-width: var(--measure);
		margin-bottom: clamp(2rem, 4vw, 3.25rem);
	}

	.tag {
		color: var(--muted);
		margin-bottom: 0.9rem;
	}

	.dark .tag {
		color: var(--accent);
	}

	.accent .tag {
		color: #7c4a05;
	}

	h2 {
		font-size: clamp(1.9rem, 4.2vw, 3rem);
	}

	.lede {
		margin-top: 1.1rem;
		font-size: clamp(1rem, 1.6vw, 1.15rem);
		color: var(--muted);
		max-width: 58ch;
	}

	.dark .lede {
		color: #d6d3d1;
	}

	.accent .lede {
		color: #4a2f06;
	}
</style>
