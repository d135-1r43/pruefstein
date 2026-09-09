<script>
	import Section from './Section.svelte';
	import Stone from './Stone.svelte';
	import { steps } from '$lib/data/steps.js';
</script>

<div class="wrap">
	<Stone n={1} side="left" width="24vw" bottom="-8%" opacity={0.5} flip />

	<Section
		id="how"
		tone="white"
		eyebrow="How it works"
		title="Four moves, and none of them is a spreadsheet"
		lede="A Prüfstein deployment is a web app your admins use and a command your colleagues run. Nothing sits resident on anybody's laptop."
	>
		<ol class="steps">
			{#each steps as step (step.n)}
				<li class="step">
					<div class="num">{step.n}</div>
					<div class="text">
						<h3>{step.title}</h3>
						<p>{step.body}</p>
						<p class="meta">{step.meta}</p>
					</div>
				</li>
			{/each}
		</ol>
	</Section>
</div>

<style>
	.wrap {
		position: relative;
		overflow: hidden;
	}

	.steps {
		list-style: none;
		margin: 0;
		padding: 0;
		display: grid;
		gap: 1.25rem;
		position: relative;
		z-index: 1;
	}

	.step {
		display: grid;
		grid-template-columns: auto 1fr;
		gap: 1.25rem;
		align-items: start;
		background: var(--surface);
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow);
		padding: clamp(1.25rem, 2.5vw, 1.75rem);
		transition:
			box-shadow 0.12s linear,
			transform 0.12s linear;
	}

	.step:hover {
		box-shadow: var(--shadow-lg);
		transform: translate(-2px, -2px);
	}

	.num {
		font-family: var(--mono);
		font-weight: 700;
		font-size: 1.05rem;
		line-height: 1;
		padding: 0.5rem 0.6rem;
		background: var(--accent);
		border: var(--rule) solid var(--ink);
		transition:
			background-color 0.12s linear,
			color 0.12s linear;
	}

	.step:hover .num {
		background: var(--ink);
		color: var(--accent);
	}

	@media (prefers-reduced-motion: reduce) {
		.step,
		.num {
			transition: none;
		}
		.step:hover {
			transform: none;
		}
	}

	h3 {
		font-size: 1.15rem;
	}

	.text p {
		margin-top: 0.55rem;
		color: var(--muted);
		font-size: 0.94rem;
		max-width: 62ch;
	}

	.meta {
		font-family: var(--mono);
		font-size: 0.75rem !important;
		color: var(--faint) !important;
		margin-top: 0.75rem !important;
	}

	@media (min-width: 760px) {
		.steps {
			grid-template-columns: repeat(2, 1fr);
			gap: 1.5rem;
		}
	}
</style>
