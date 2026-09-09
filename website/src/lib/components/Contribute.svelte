<script>
	import Section from './Section.svelte';
	import Card from './Card.svelte';
	import Button from './Button.svelte';
	import Stone from './Stone.svelte';
	import { lanes } from '$lib/data/contribute.js';
	import { site } from '$lib/data/site.js';
</script>

<div class="wrap">
	<Stone n={2} side="left" width="22vw" bottom="-12%" opacity={0.4} />

	<Section
		id="contribute"
		tone="white"
		eyebrow="Contribute"
		title="The best contribution is a check you already know"
		lede="Prüfstein is small and early, and the gaps are not secret. Here is what would help, roughly in order of how far it gets on how little effort."
	>
		<div class="grid">
			{#each lanes as lane (lane.title)}
				<Card
					tag={lane.tag}
					title={lane.title}
					href={lane.href}
					external
					tone={lane.accent ? 'accent' : 'white'}
				>
					{lane.body}
					<span class="cta">{lane.cta} ↗</span>
				</Card>
			{/each}
		</div>

		<div class="call">
			<div>
				<h3>No contribution is too small to matter here</h3>
				<p>
					A typo in an expression, a check that is wrong on the newest macOS, a paragraph of setup
					notes. All of it lands. Open an issue before a large pull request so nobody builds the
					same thing twice.
				</p>
			</div>
			<div class="call-actions">
				<Button href={site.issues} external variant="accent">Browse open issues ↗</Button>
				<Button href={site.newIssue} external variant="plain">Open an issue ↗</Button>
			</div>
		</div>
	</Section>
</div>

<style>
	.wrap {
		position: relative;
		overflow: hidden;
	}

	.grid {
		display: grid;
		gap: 1.4rem;
		position: relative;
		z-index: 1;
	}

	.cta {
		display: block;
		margin-top: 1rem;
		font-size: 10px;
		font-weight: 700;
		letter-spacing: 0.15em;
		text-transform: uppercase;
		color: var(--ink);
	}

	.call {
		position: relative;
		z-index: 1;
		margin-top: 2.5rem;
		padding: clamp(1.5rem, 3vw, 2.25rem);
		background: var(--paper);
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow);
		display: grid;
		gap: 1.5rem;
		align-items: center;
	}

	.call h3 {
		font-size: clamp(1.2rem, 2.4vw, 1.6rem);
	}

	.call p {
		margin-top: 0.75rem;
		color: var(--muted);
		font-size: 0.95rem;
		max-width: 58ch;
	}

	.call-actions {
		display: flex;
		flex-wrap: wrap;
		gap: 0.85rem;
	}

	@media (min-width: 700px) {
		.grid {
			grid-template-columns: repeat(2, 1fr);
		}
	}

	@media (min-width: 1050px) {
		.grid {
			grid-template-columns: repeat(3, 1fr);
		}
		.call {
			grid-template-columns: 1.4fr auto;
		}
	}
</style>
