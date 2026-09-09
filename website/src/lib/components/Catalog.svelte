<script>
	import Section from './Section.svelte';
	import Stone from './Stone.svelte';
	import { catalog } from '$lib/data/checks.js';
	import { prefersReducedMotion, onceVisible } from '$lib/motion.js';

	const total = catalog.reduce((n, g) => n + g.items.length, 0);

	/** Starts at the real number so the prerendered heading is already true. */
	let count = $state(total);
	let counter = $state(null);

	$effect(() => {
		if (!counter || prefersReducedMotion()) return;
		count = 0;
		let timers = [];
		const stop = onceVisible(
			counter,
			() => {
				for (let n = 1; n <= total; n++) {
					timers.push(setTimeout(() => (count = n), 60 + n * 55));
				}
			},
			0.5
		);
		return () => {
			stop();
			for (const t of timers) clearTimeout(t);
		};
	});
</script>

<div class="wrap" bind:this={counter}>
	<Stone n={4} side="right" width="26vw" bottom="-10%" opacity={0.45} />

	<Section
		id="catalog"
		tone="paper"
		eyebrow="Batteries included"
		title="{count} checks are already there when you start"
		lede="A fresh deployment seeds its own catalog against permanent keys, mapped to the Annex A families they serve. Nothing here is a placeholder. This is what the first dashboard shows."
	>
		<div class="groups">
			{#each catalog as group (group.group)}
				<section class="group">
					<h3 class="eyebrow gname">{group.group}</h3>
					<ul>
						{#each group.items as item (item)}
							<li>{item}</li>
						{/each}
					</ul>
					{#if group.note}
						<p class="note">{group.note}</p>
					{/if}
				</section>
			{/each}
		</div>

		<p class="caveat">
			<strong>Straight answer:</strong> the seeded catalog is macOS to the last row. The agent runs
			anywhere osquery runs, and the model has nothing macOS-specific in it. But the Windows and
			Linux equivalents are not written yet. That is
			<a href="#contribute">the most useful thing you could contribute</a>.
		</p>
	</Section>
</div>

<style>
	.wrap {
		position: relative;
		overflow: hidden;
	}

	/* Columns rather than a grid: the groups are different lengths and a grid
	   row would leave a hole under the short ones. */
	.groups {
		columns: 1;
		column-gap: 1.4rem;
		position: relative;
		z-index: 1;
	}

	.group {
		background: var(--surface);
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow);
		padding: 1.4rem;
		break-inside: avoid;
		margin-bottom: 1.4rem;
	}

	.gname {
		color: var(--ink);
		padding-bottom: 0.85rem;
		margin-bottom: 0.85rem;
		border-bottom: var(--rule) solid var(--ink);
	}

	ul {
		list-style: none;
		margin: 0;
		padding: 0;
		display: grid;
		gap: 0.5rem;
	}

	li {
		font-size: 0.9rem;
		padding-left: 1.2rem;
		position: relative;
		color: var(--muted);
	}

	li::before {
		content: '';
		position: absolute;
		left: 0;
		top: 0.5em;
		width: 8px;
		height: 8px;
		background: var(--accent);
		border: 1px solid var(--ink);
	}

	.note {
		margin-top: 0.9rem;
		font-size: 0.8rem;
		color: var(--faint);
	}

	.caveat {
		position: relative;
		z-index: 1;
		margin-top: 2rem;
		max-width: 70ch;
		font-size: 0.94rem;
		color: var(--muted);
		border-left: var(--rule) solid var(--ink);
		padding-left: 1rem;
	}

	.caveat strong {
		color: var(--ink);
	}

	@media (min-width: 700px) {
		.groups {
			columns: 2;
		}
	}

	@media (min-width: 1050px) {
		.groups {
			columns: 3;
		}
	}
</style>
