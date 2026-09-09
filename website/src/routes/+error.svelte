<script>
	import { page } from '$app/state';
	import { base } from '$app/paths';
	import Button from '$lib/components/Button.svelte';
</script>

<svelte:head><title>Not found · Prüfstein</title></svelte:head>

<section class="wrap">
	<div class="shell inner">
		<p class="eyebrow">Error {page.status}</p>
		<h1>{page.status === 404 ? 'No such page.' : 'Something went wrong.'}</h1>
		<p class="lede">
			{page.status === 404
				? 'That address is not part of this site. The front page has everything.'
				: page.error?.message}
		</p>
		<div class="act"><Button href="{base}/">Back to the front page</Button></div>
	</div>
</section>

<style>
	.wrap {
		border-top: var(--rule) solid var(--ink);
	}
	.inner {
		padding-block: clamp(4rem, 14vw, 9rem);
		max-width: 60ch;
	}
	h1 {
		margin-top: 0.9rem;
		font-size: clamp(2rem, 6vw, 3.25rem);
	}
	.lede {
		margin-top: 1.1rem;
		color: var(--muted);
	}
	.act {
		margin-top: 2rem;
	}
</style>
