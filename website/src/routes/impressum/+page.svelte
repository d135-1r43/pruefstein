<script>
	import { base } from '$app/paths';
	import Button from '$lib/components/Button.svelte';
	import ExpLogo from '$lib/components/ExpLogo.svelte';
	import { site, company } from '$lib/data/site.js';

	const title = `Impressum · ${site.name}`;
</script>

<svelte:head>
	<title>{title}</title>
	<meta
		name="description"
		content="Impressum und Anbieterkennzeichnung nach § 5 TMG für {site.domain}, {company.name}."
	/>
	<meta name="robots" content="noindex, follow" />
	<link rel="canonical" href="{site.url}/impressum/" />
</svelte:head>

<section class="page">
	<div class="shell inner">
		<header class="head">
			<p class="eyebrow">Anbieterkennzeichnung nach § 5 TMG</p>
			<h1>Impressum</h1>
			<p class="lede">
				{site.domain} ist die Projektseite von Prüfstein, einer quelloffenen Anwendung der
				{company.name}.
			</p>
		</header>

		<div class="plate">
			<a href={company.site} target="_blank" rel="noreferrer noopener" class="mark">
				<ExpLogo height={34} />
			</a>

			<dl>
				<div class="row">
					<dt>Diensteanbieter</dt>
					<dd>
						<address>
							<strong>{company.name}</strong><br />
							{company.street}<br />
							{company.postcode}
							{company.city}<br />
							{company.country}
						</address>
					</dd>
				</div>

				<div class="row">
					<dt>Kontakt</dt>
					<dd>
						Telefon: <a href="tel:{company.phoneHref}">{company.phone}</a><br />
						Telefax: {company.fax}<br />
						E-Mail: <a href="mailto:{company.email}">{company.email}</a><br />
						Web:
						<a href={company.site} target="_blank" rel="noreferrer noopener">{company.siteLabel}</a>
					</dd>
				</div>

				<div class="row">
					<dt>Vertreten durch</dt>
					<dd>Geschäftsführer: {company.managingDirector}</dd>
				</div>

				<div class="row">
					<dt>Registereintrag</dt>
					<dd>
						Eingetragen im Handelsregister.<br />
						Registergericht: {company.court}<br />
						Registernummer: {company.registerNumber}
					</dd>
				</div>

				<div class="row">
					<dt>Umsatzsteuer-ID</dt>
					<dd>
						Umsatzsteuer-Identifikationsnummer nach § 27a Umsatzsteuergesetz:<br />
						{company.vatId}
					</dd>
				</div>

				<div class="row">
					<dt>Inhaltlich verantwortlich</dt>
					<dd>
						Nach § 18 Abs. 2 MStV:<br />
						{company.managingDirector}<br />
						{company.street}, {company.postcode}
						{company.city}
					</dd>
				</div>

				<div class="row">
					<dt>EU-Streitschlichtung</dt>
					<dd>
						Wir sind nicht bereit oder verpflichtet, an Streitbeilegungsverfahren vor einer
						Verbraucherschlichtungsstelle teilzunehmen.
					</dd>
				</div>
			</dl>
		</div>

		<div class="notes">
			<div class="note">
				<p class="eyebrow">Haftung und Urheberrecht</p>
				<p>
					Es gelten die Haftungs- und Urheberrechtshinweise im
					<a href={company.impressumUrl} target="_blank" rel="noreferrer noopener">
						Impressum der {company.name}</a
					>.
				</p>
			</div>
			<div class="note">
				<p class="eyebrow">Datenschutz</p>
				<p>
					Diese Seite setzt keine Cookies, bindet keine Ressourcen Dritter ein und verwendet keine
					Analysewerkzeuge. Im Übrigen gilt die
					<a href={company.privacyUrl} target="_blank" rel="noreferrer noopener"
						>Datenschutzerklärung der {company.name}</a
					>.
				</p>
			</div>
			<div class="note">
				<p class="eyebrow">Software</p>
				<p>
					Prüfstein selbst steht unter der
					<a href={site.licenseUrl} target="_blank" rel="noreferrer noopener">Apache-2.0-Lizenz</a>
					und wird auf
					<a href={site.repo} target="_blank" rel="noreferrer noopener">GitHub</a> entwickelt.
				</p>
			</div>
		</div>

		<div class="back">
			<Button href="{base}/">Zurück zur Startseite</Button>
		</div>
	</div>
</section>

<style>
	.page {
		border-top: var(--rule) solid var(--ink);
		background: var(--paper);
	}

	.inner {
		padding-block: clamp(3rem, 8vw, 5.5rem);
	}

	.head {
		max-width: var(--measure);
		margin-bottom: clamp(2rem, 4vw, 3rem);
	}

	.head .eyebrow {
		color: var(--muted);
		margin-bottom: 0.9rem;
	}

	h1 {
		font-size: clamp(2.2rem, 6vw, 3.4rem);
	}

	.lede {
		margin-top: 1.1rem;
		color: var(--muted);
		max-width: 54ch;
	}

	.plate {
		background: var(--surface);
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow-lg);
		padding: clamp(1.5rem, 3vw, 2.25rem);
		max-width: 62rem;
	}

	.mark {
		display: inline-block;
		width: fit-content;
		padding-bottom: 1.5rem;
		margin-bottom: 1.5rem;
		border-bottom: var(--rule) solid var(--ink);
	}

	dl {
		margin: 0;
		display: grid;
		gap: 0;
	}

	.row {
		display: grid;
		gap: 0.35rem;
		padding-block: 1.1rem;
		border-bottom: 1px solid #e7e5e4;
	}

	.row:last-child {
		border-bottom: 0;
		padding-bottom: 0;
	}

	.row:first-of-type {
		padding-top: 0;
	}

	dt {
		font-size: 10px;
		font-weight: 700;
		letter-spacing: 0.15em;
		text-transform: uppercase;
		color: var(--faint);
	}

	dd {
		margin: 0;
		font-size: 0.95rem;
		line-height: 1.7;
	}

	address {
		font-style: normal;
	}

	dd a,
	.note a {
		color: inherit;
		text-decoration: none;
		border-bottom: 2px solid var(--accent);
	}

	dd a:hover,
	.note a:hover {
		background: var(--accent);
	}

	.notes {
		display: grid;
		gap: 1.75rem;
		margin-top: 2.5rem;
		max-width: 62rem;
	}

	.note p:last-child {
		margin-top: 0.6rem;
		font-size: 0.9rem;
		color: var(--muted);
	}

	.back {
		margin-top: 2.75rem;
	}

	@media (min-width: 640px) {
		.row {
			grid-template-columns: 14rem 1fr;
			gap: 1.5rem;
		}
		dt {
			padding-top: 0.3rem;
		}
	}

	@media (min-width: 900px) {
		.notes {
			grid-template-columns: repeat(3, 1fr);
			gap: 2rem;
		}
	}
</style>
