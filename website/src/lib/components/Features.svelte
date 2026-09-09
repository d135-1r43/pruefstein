<script>
	import Section from './Section.svelte';
	import Card from './Card.svelte';
	import { features } from '$lib/data/features.js';
</script>

<Section
	id="features"
	tone="white"
	eyebrow="What it does"
	title="Everything the compliance part of an ISMS actually needs"
	lede="Not an MDM, not an EDR, not a policy PDF generator. One narrow job, done end to end: proving that endpoints meet the controls you wrote down."
>
	<div class="grid">
		{#each features as f (f.tag)}
			<Card tag={f.tag} title={f.title} tone={f.accent ? 'accent' : 'white'}>
				{f.body}
			</Card>
		{/each}
	</div>
</Section>

<style>
	.grid {
		display: grid;
		gap: 1.4rem;
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
	}
</style>
