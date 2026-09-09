<script>
	import LogoMark from './LogoMark.svelte';
	import ExpLogo from './ExpLogo.svelte';
	import { base } from '$app/paths';
	import { site, company } from '$lib/data/site.js';

	const year = new Date().getFullYear();
</script>

<footer>
	<div class="shell inner">
		<div class="brand">
			<LogoMark size={54} />
			<div>
				<p class="name">{site.name}</p>
				<p class="gloss">
					<em>Prüfstein</em>, German for touchstone: the dark stone a jeweller rubs gold against to see
					whether it is what it claims to be.
				</p>
			</div>
		</div>

		<nav aria-label="Footer">
			<div class="col">
				<p class="eyebrow">Project</p>
				<a href={site.repo} target="_blank" rel="noreferrer noopener">Source on GitHub</a>
				<a href={site.issues} target="_blank" rel="noreferrer noopener">Issues</a>
				<a href={site.discussions} target="_blank" rel="noreferrer noopener">Discussions</a>
				<a href={site.licenseUrl} target="_blank" rel="noreferrer noopener">Apache-2.0 licence</a>
			</div>
			<div class="col">
				<p class="eyebrow">Built on</p>
				<a href="https://osquery.io/" target="_blank" rel="noreferrer noopener">osquery</a>
				<a href="https://quarkus.io/" target="_blank" rel="noreferrer noopener">Quarkus</a>
				<a
					href="https://commons.apache.org/proper/commons-jexl/"
					target="_blank"
					rel="noreferrer noopener">Apache JEXL</a
				>
				<a href="https://www.postgresql.org/" target="_blank" rel="noreferrer noopener">PostgreSQL</a
				>
			</div>
		</nav>
	</div>

	<!-- ── Who builds it ──────────────────────────────────────────── -->
	<div class="shell maker">
		<div class="maker-brand">
			<p class="eyebrow">Built and maintained by</p>
			<a href={company.site} target="_blank" rel="noreferrer noopener" class="exp">
				<ExpLogo height={26} />
			</a>
		</div>

		<address>
			<span class="legal-name">{company.name}</span>
			{company.street} · {company.postcode}
			{company.city} · {company.country}<br />
			<a href="tel:{company.phoneHref}">{company.phone}</a> ·
			<a href="mailto:{company.email}">{company.email}</a> ·
			<a href={company.site} target="_blank" rel="noreferrer noopener">{company.siteLabel}</a>
		</address>
	</div>

	<div class="shell bottom">
		<p>© {year} {company.name} and the Prüfstein contributors · Apache-2.0</p>
		<nav class="legal" aria-label="Legal">
			<a href="{base}/impressum/">Impressum</a>
			<a href={company.privacyUrl} target="_blank" rel="noreferrer noopener">Datenschutz</a>
		</nav>
		<p class="colophon">
			Set in Space Grotesk and Space Mono, both self-hosted. This site makes no third-party requests
			and sets no cookies.
		</p>
	</div>
</footer>

<style>
	footer {
		background: var(--ink);
		color: #d6d3d1;
		border-top: var(--rule) solid var(--ink);
	}

	.inner {
		display: grid;
		gap: 2.5rem;
		padding-block: clamp(2.5rem, 6vw, 4rem) 2.5rem;
	}

	.brand {
		display: flex;
		gap: 1rem;
		align-items: flex-start;
	}

	/* currentColor, so the mark inverts to sit on the black band */
	.brand :global(svg) {
		color: #fafaf9;
		flex: none;
	}

	.name {
		font-weight: 700;
		font-size: 1.4rem;
		color: #fff;
		text-transform: uppercase;
		letter-spacing: 0.055em;
	}

	.gloss {
		margin-top: 0.5rem;
		font-size: 0.85rem;
		color: #a8a29e;
		max-width: 42ch;
	}

	nav {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 2rem;
	}

	.col {
		display: flex;
		flex-direction: column;
		gap: 0.55rem;
	}

	.col .eyebrow {
		color: var(--accent);
		margin-bottom: 0.35rem;
	}

	.col a {
		font-size: 0.9rem;
		text-decoration: none;
		color: #d6d3d1;
		width: fit-content;
		border-bottom: 1px solid transparent;
	}

	.col a:hover {
		color: var(--accent);
		border-bottom-color: var(--accent);
	}

	/* --- Maker band ------------------------------------------------------- */

	.maker {
		border-top: 1px solid #44403c;
		padding-block: 1.75rem;
		display: grid;
		gap: 1.25rem;
		align-items: center;
	}

	.maker-brand .eyebrow {
		color: #78716c;
		margin-bottom: 0.7rem;
	}

	.exp {
		display: inline-block;
		width: fit-content;
	}

	.exp:hover {
		opacity: 0.8;
	}

	address {
		font-style: normal;
		font-size: 0.82rem;
		line-height: 1.85;
		color: #a8a29e;
	}

	.legal-name {
		display: block;
		color: #fff;
		font-weight: 700;
		font-size: 0.9rem;
	}

	address a {
		color: #a8a29e;
		text-decoration: none;
		border-bottom: 1px solid #44403c;
	}

	address a:hover {
		color: var(--accent);
		border-bottom-color: var(--accent);
	}

	/* --- Bottom bar ------------------------------------------------------- */

	.bottom {
		border-top: 1px solid #44403c;
		padding-block: 1.5rem;
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem 2rem;
		align-items: baseline;
		font-size: 0.78rem;
		color: #a8a29e;
	}

	.legal {
		display: flex;
		gap: 1.1rem;
	}

	.legal a {
		color: #d6d3d1;
		text-decoration: none;
		font-weight: 700;
		border-bottom: 1px solid #44403c;
	}

	.legal a:hover {
		color: var(--accent);
		border-bottom-color: var(--accent);
	}

	.colophon {
		max-width: 52ch;
	}

	@media (min-width: 720px) {
		.maker {
			grid-template-columns: auto 1fr;
			gap: 2.5rem;
		}
	}

	@media (min-width: 860px) {
		.inner {
			grid-template-columns: 1.2fr 1fr;
			gap: 4rem;
		}
		.bottom {
			justify-content: space-between;
		}
		.colophon {
			margin-left: auto;
			text-align: right;
		}
	}
</style>
