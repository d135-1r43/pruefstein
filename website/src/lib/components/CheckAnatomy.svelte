<script>
	import Section from './Section.svelte';
	import CodeBlock from './CodeBlock.svelte';
	import Badge from './Badge.svelte';
	import { examples } from '$lib/data/checks.js';

	let active = $state(0);
	const current = $derived(examples[active]);

	/** Roving tabindex: left/right walk the tabs, home/end jump the ends. */
	function onKey(event) {
		const last = examples.length - 1;
		let next = null;
		if (event.key === 'ArrowRight') next = active === last ? 0 : active + 1;
		else if (event.key === 'ArrowLeft') next = active === 0 ? last : active - 1;
		else if (event.key === 'Home') next = 0;
		else if (event.key === 'End') next = last;
		if (next === null) return;
		event.preventDefault();
		active = next;
		document.getElementById(`tab-${examples[next].id}`)?.focus();
	}
</script>

<Section
	id="anatomy"
	tone="paper"
	eyebrow="Anatomy of a check"
	title="A control is a query and one expression"
	lede="No rule builder, no proprietary policy language, no agent-side magic. A Compliance Item is SQL against the osquery schema plus a JEXL expression that has to come out true. That is the whole abstraction."
>
	<div class="tabs" role="tablist" aria-label="Example checks" onkeydown={onKey}>
		{#each examples as ex, i (ex.id)}
			<button
				role="tab"
				id="tab-{ex.id}"
				aria-selected={active === i}
				aria-controls="panel-{ex.id}"
				tabindex={active === i ? 0 : -1}
				class="tab"
				class:on={active === i}
				onclick={() => (active = i)}
			>
				{ex.label}
			</button>
		{/each}
	</div>

	{#key current.id}
	<div
		class="panel"
		role="tabpanel"
		id="panel-{current.id}"
		aria-labelledby="tab-{current.id}"
		tabindex="0"
	>
		<div class="panel-head">
			<div>
				<p class="eyebrow">{current.group}</p>
				<h3>{current.name}</h3>
			</div>
			<Badge tone={current.passed ? 'pass' : 'fail'}>
				{current.passed ? 'Pass' : 'Fail'}
			</Badge>
		</div>

		<div class="cols">
			<CodeBlock label="1 · osquery SQL" code={current.sql} wrap />
			<CodeBlock label="2 · osqueryi --json returned" code={current.json} tone="dark" wrap />
			<CodeBlock label="3 · JEXL expression, must be true" code={current.jexl} wrap />
		</div>

		<p class="reads">
			<strong>In words:</strong>
			{current.reads}
		</p>
	</div>
	{/key}

	<div class="notes">
		<div class="note">
			<p class="eyebrow">Auditable</p>
			<p>
				Your auditor does not have to trust a vendor's definition of "encrypted". They can read the
				query and the expression and decide for themselves.
			</p>
		</div>
		<div class="note">
			<p class="eyebrow">Evidence kept</p>
			<p>
				The full JSON osquery returned is stored with the result, so a verdict can be re-argued
				months later against what the machine actually said.
			</p>
		</div>
		<div class="note">
			<p class="eyebrow">Yours to change</p>
			<p>
				Every seeded check is an ordinary row. Edit the SQL, loosen the threshold, or delete it.
				Upgrades will not overwrite what you have changed.
			</p>
		</div>
	</div>
</Section>

<style>
	.tabs {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		margin-bottom: 1.5rem;
	}

	.tab {
		font-family: inherit;
		font-size: 10px;
		font-weight: 700;
		letter-spacing: 0.15em;
		text-transform: uppercase;
		padding: 0.55rem 0.9rem;
		border: var(--rule) solid var(--ink);
		background: var(--surface);
		cursor: pointer;
	}

	.tab {
		transition:
			background-color 0.1s linear,
			transform 0.1s linear;
	}

	.tab:hover {
		background: var(--accent-soft);
		transform: translateY(-2px);
	}

	.tab:active {
		transform: translateY(1px);
	}

	.tab.on {
		background: var(--ink);
		color: #fff;
	}

	.panel {
		background: var(--surface);
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow-lg);
		padding: clamp(1.25rem, 3vw, 2rem);
		animation: swap 0.24s ease-out 1;
	}

	@keyframes swap {
		from {
			opacity: 0.35;
			transform: translateX(6px);
		}
		to {
			opacity: 1;
			transform: none;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.panel {
			animation: none;
		}
		.tab {
			transition: none;
		}
		.tab:hover,
		.tab:active {
			transform: none;
		}
	}

	.panel-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		padding-bottom: 1.25rem;
		margin-bottom: 1.5rem;
		border-bottom: var(--rule) solid var(--ink);
	}

	.panel-head h3 {
		font-size: clamp(1.15rem, 2.2vw, 1.5rem);
		margin-top: 0.4rem;
	}

	.cols {
		display: grid;
		gap: 1rem;
		align-items: stretch;
	}

	.reads {
		margin-top: 1.35rem;
		font-size: 0.94rem;
		color: var(--muted);
	}

	.reads strong {
		color: var(--ink);
	}

	.notes {
		display: grid;
		gap: 1.5rem;
		margin-top: 2.5rem;
	}

	.note p:last-child {
		margin-top: 0.6rem;
		font-size: 0.9rem;
		color: var(--muted);
	}

	@media (min-width: 900px) {
		.cols {
			grid-template-columns: 1.2fr 0.9fr 1.1fr;
		}
		.notes {
			grid-template-columns: repeat(3, 1fr);
			gap: 2rem;
		}
	}
</style>
