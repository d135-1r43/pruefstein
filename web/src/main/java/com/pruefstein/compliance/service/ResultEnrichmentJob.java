package com.pruefstein.compliance.service;

import java.util.List;

import com.pruefstein.compliance.service.ComplianceResultEnricher.EnrichmentRequest;
import com.pruefstein.notification.ReportMailDispatcher;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Explains failed checks after the fact, rather than while an agent waits.
 *
 * <p>
 * Enrichment used to run inside {@code POST /api/reports}, one model call per
 * failed check, in sequence: a device with eight failures made the agent wait
 * for eight round trips and blew through its 30-second read timeout, at which
 * point the report was stored anyway — with no explanations and nothing to ever
 * retry them. Moving the calls here decouples the two: the push is a database
 * write, and the explanations catch up within a cycle.
 */
@ApplicationScoped
class ResultEnrichmentJob
{
	private static final Logger LOG = LoggerFactory.getLogger(ResultEnrichmentJob.class);

	/**
	 * A bound per cycle rather than per report: a backlog is worked through
	 * over several passes instead of one pass holding the model for minutes.
	 */
	private static final int BATCH = 20;

	@Inject
	ComplianceResultEnricher enricher;

	@Inject
	ComplianceResultAiService aiService;

	@Inject
	ReportMailDispatcher mailDispatcher;

	@Scheduled(every = "{pruefstein.compliance.enrichment-interval}", concurrentExecution = ConcurrentExecution.SKIP)
	void explainFailedChecks()
	{
		List<EnrichmentRequest> pending;
		try
		{
			pending = enricher.findPending(BATCH);
		}
		catch (Exception e)
		{
			// A scheduled method that throws is only a log line, so say
			// something useful rather than letting the scheduler report it.
			LOG.warn("Could not read the checks waiting for an explanation.", e);
			return;
		}
		if (!pending.isEmpty())
		{
			LOG.debug("Explaining {} failed check(s)", pending.size());
			for (EnrichmentRequest request : pending)
			{
				explain(request);
			}
		}

		// Every cycle, not only the ones that explained something: a mail held
		// back for checks nothing can explain is released by its grace period,
		// and nothing else would ever notice that it had run out.
		try
		{
			mailDispatcher.sendReady();
		}
		catch (Exception e)
		{
			LOG.warn("Could not send the outcome mails that were waiting for explanations.", e);
		}
	}

	/**
	 * One failure does not cost the rest of the batch: a model that rejects one
	 * output, or a check whose result vanished with its report, leaves the row
	 * unexplained and the next cycle picks it up again.
	 */
	private void explain(EnrichmentRequest request)
	{
		try
		{
			ComplianceResultExplanation explanation = request.blacklist()
				? aiService.explainBlacklist(request.output(), request.reasons())
				: aiService.explain(request.checkName(), request.query(), request.expression(),
					request.output());
			enricher.save(request.resultId(), explanation);
		}
		catch (Exception e)
		{
			LOG.warn("Could not explain the failed check '{}'.", request.checkName(), e);
		}
	}
}
