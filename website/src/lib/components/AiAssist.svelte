<script>
	import Section from './Section.svelte';
	import CodeBlock from './CodeBlock.svelte';
	import Badge from './Badge.svelte';
	import { aiJobs } from '$lib/data/ai.js';

	const config = `# Any OpenAI-compatible endpoint. Your key, your model.
OPENAI_API_KEY=sk-…
QUARKUS_LANGCHAIN4J_OPENAI_CHAT_MODEL_MODEL_NAME=gpt-4o-mini

# Self-hosted instead? Point it somewhere else.
QUARKUS_LANGCHAIN4J_OPENAI_BASE_URL=http://ollama.internal:11434/v1
QUARKUS_LANGCHAIN4J_OPENAI_CHAT_MODEL_MODEL_NAME=llama3.1`;
</script>

<Section
	id="ai"
	tone="white"
	eyebrow="AI assistance · optional"
	title="It writes the check, and it explains the failure"
	lede="Two jobs at opposite ends of the same problem: getting a control written in the first place, and telling somebody in plain words how to fix the one that just went red. Both are optional, and both run against a model you choose."
>
	<div class="jobs">
		{#each aiJobs as job (job.tag)}
			<article class="job">
				<div class="job-head">
					<Badge tone={job.tag === 'Create' ? 'accent' : 'pass'}>{job.tag}</Badge>
					<h3>{job.title}</h3>
				</div>
				<p class="job-body">{job.body}</p>
				<ul>
					{#each job.points as point (point)}
						<li>{point}</li>
					{/each}
				</ul>
			</article>
		{/each}
	</div>

	<div class="byo">
		<div class="byo-copy">
			<p class="eyebrow">Bring your own model</p>
			<h3>No model ships with it, and none is assumed</h3>
			<p>
				Prüfstein talks to an OpenAI-compatible endpoint through LangChain4j. Set the key, name the
				model, and if you are not using OpenAI point the base URL wherever you like: a hosted
				provider, vLLM in your own VPC, Ollama on a workstation. Nothing about the fleet leaves your
				network unless you decide it does.
			</p>
			<p class="off">
				Set no key and the feature simply stays dark. Explanations never appear; reports, mails,
				cycles and the dashboard carry on exactly as before. The AI is a convenience on top of the
				product, never a dependency inside it.
			</p>
		</div>

		<CodeBlock label="Configuration" code={config} tone="dark" wrap />
	</div>
</Section>

<style>
	.jobs {
		display: grid;
		gap: 1.5rem;
	}

	.job {
		background: var(--surface);
		border: var(--rule) solid var(--ink);
		box-shadow: var(--shadow);
		padding: clamp(1.4rem, 2.6vw, 1.9rem);
	}

	.job-head {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
		gap: 0.85rem;
		padding-bottom: 1.1rem;
		margin-bottom: 1.1rem;
		border-bottom: var(--rule) solid var(--ink);
	}

	h3 {
		font-size: clamp(1.15rem, 2.2vw, 1.4rem);
	}

	.job-body {
		font-size: 0.95rem;
		color: var(--muted);
	}

	.job ul {
		list-style: none;
		margin: 1.25rem 0 0;
		padding: 0;
		display: grid;
		gap: 0.7rem;
	}

	.job li {
		position: relative;
		padding-left: 1.3rem;
		font-size: 0.89rem;
		line-height: 1.6;
		color: var(--muted);
	}

	.job li::before {
		content: '';
		position: absolute;
		left: 0;
		top: 0.5em;
		width: 8px;
		height: 8px;
		background: var(--accent);
		border: 1px solid var(--ink);
	}

	.byo {
		margin-top: 2.5rem;
		padding-top: 2.5rem;
		border-top: var(--rule) solid var(--ink);
		display: grid;
		gap: 2rem;
		align-items: start;
	}

	.byo-copy .eyebrow {
		color: var(--muted);
		margin-bottom: 0.9rem;
	}

	.byo-copy p:not(.eyebrow) {
		margin-top: 0.9rem;
		font-size: 0.95rem;
		color: var(--muted);
		max-width: 52ch;
	}

	.off {
		border-left: var(--rule) solid var(--ink);
		padding-left: 1rem;
	}

	@media (min-width: 860px) {
		.jobs {
			grid-template-columns: repeat(2, 1fr);
		}
		.byo {
			grid-template-columns: 1fr 1fr;
			gap: 3rem;
		}
	}
</style>
