<script>
	/**
	 * The status pill from the reports table: tiny, all-caps, hard-bordered.
	 * Colours are the dashboard's: emerald for pass, red for fail.
	 */
	let { tone = 'neutral', children } = $props();
</script>

<span class="badge {tone}">{@render children()}</span>

<style>
	.badge {
		display: inline-block;
		padding: 0.25rem 0.55rem;
		border: var(--rule) solid var(--ink);
		font-size: 10px;
		font-weight: 700;
		letter-spacing: 0.15em;
		text-transform: uppercase;
		line-height: 1.4;
		white-space: nowrap;
	}

	.pass {
		background: var(--ok);
		color: var(--ok-ink);
	}
	.fail {
		background: var(--bad);
		color: var(--bad-ink);
	}
	.wait {
		background: var(--wait);
	}
	.accent {
		background: var(--accent);
	}
	.neutral {
		background: var(--surface);
	}
	.invert {
		background: var(--ink);
		color: #fff;
	}
</style>
