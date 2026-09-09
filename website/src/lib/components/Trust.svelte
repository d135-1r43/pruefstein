<script>
	import Section from './Section.svelte';
	import { trust } from '$lib/data/trust.js';
</script>

<Section
	id="trust"
	tone="accent"
	eyebrow="Trust, not surveillance"
	title="It assumes your colleagues are not the threat"
	lede="Most endpoint tooling is written as though the person holding the laptop is the adversary. Prüfstein starts from the opposite premise: people want their machine to be compliant, and mostly need to be told what is wrong and how to fix it."
>
	<ul class="items">
		{#each trust as item (item.title)}
			<li>
				<h3>{item.title}</h3>
				<p>{item.body}</p>
			</li>
		{/each}
	</ul>

	<p class="honest">
		<strong>Trust is not the absence of accountability.</strong> Someone can decline to report, and
		if the reporting window closes with nothing filed, the server records a
		<span class="tag">MISSING</span> report for that device by itself and opens the next cycle. The
		deadline does the enforcing, so the tool never has to watch anybody.
	</p>
</Section>

<style>
	.items {
		list-style: none;
		margin: 0;
		padding: 0;
		display: grid;
		gap: var(--rule);
		border: var(--rule) solid var(--ink);
		background: var(--ink);
	}

	li {
		background: var(--accent);
		padding: clamp(1.35rem, 2.5vw, 1.75rem);
		transition:
			background-color 0.12s linear,
			color 0.12s linear;
	}

	/* Inverts under the pointer, so the yellow grid reads as a keyboard */
	li:hover {
		background: var(--ink);
		color: #fff;
	}

	li:hover p {
		color: #d6d3d1;
	}

	@media (prefers-reduced-motion: reduce) {
		li {
			transition: none;
		}
	}

	h3 {
		font-size: 1.1rem;
		line-height: 1.2;
	}

	li p {
		margin-top: 0.6rem;
		font-size: 0.92rem;
		line-height: 1.6;
		color: #4a2f06;
	}

	.honest {
		margin-top: 2.25rem;
		max-width: 74ch;
		font-size: 0.98rem;
		color: #4a2f06;
		border-left: var(--rule) solid var(--ink);
		padding-left: 1.1rem;
	}

	.honest strong {
		color: var(--ink);
	}

	.tag {
		font-family: var(--mono);
		font-size: 0.78rem;
		font-weight: 700;
		background: var(--ink);
		color: var(--accent);
		padding: 0.1rem 0.4rem;
		white-space: nowrap;
	}

	/* The 2px gaps are the black grid showing through between the tiles */
	@media (min-width: 700px) {
		.items {
			grid-template-columns: repeat(2, 1fr);
		}
	}

	@media (min-width: 1050px) {
		.items {
			grid-template-columns: repeat(3, 1fr);
		}
	}
</style>
