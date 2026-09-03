package com.pruefstein.agent.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruefstein.agent.client.CheckItem;
import com.pruefstein.agent.client.InstalledAppPayload;
import com.pruefstein.agent.client.PruefsteinClient;
import com.pruefstein.agent.client.ReportPayload;
import com.pruefstein.agent.client.ReportResponse;
import com.pruefstein.agent.client.ResultPayload;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ComplianceRunner
{
	private static final Logger LOG = LoggerFactory.getLogger(ComplianceRunner.class);
	private static final JexlEngine JEXL = new JexlBuilder().strict(true).silent(false).create();

	/**
	 * Scoped to software a person installed, not everything on disk. The
	 * unfiltered {@code apps} table also returns OS components under
	 * {@code /System} and helper bundles nested inside other apps — roughly
	 * three quarters of the rows on a normal Mac, none of it meaningfully
	 * blockable. The blacklist check itself still scans the whole table, so
	 * narrowing this list costs no detection coverage.
	 */
	private static final String INVENTORY_QUERY = """
		SELECT 'app' AS source, name, bundle_identifier AS identifier, bundle_short_version AS version, path
		  FROM apps
		 WHERE path NOT LIKE '%.app/Contents/%'
		   AND (path LIKE '/Applications/%' OR path LIKE '/Users/%/Applications/%')
		UNION ALL
		SELECT 'brew:' || type AS source, name, name AS identifier, version, path FROM homebrew_packages;
		""";

	@RestClient
	PruefsteinClient client;

	/**
	 * How many checks may be in flight at once. This caps {@code osqueryi}
	 * processes, not just threads, which is why it is worth being able to turn
	 * down on a machine that has to stay responsive while the agent runs.
	 */
	@ConfigProperty(name = "pruefstein.agent.check-parallelism", defaultValue = "100")
	int parallelism;

	@Inject
	ObjectMapper objectMapper;

	/**
	 * Runs every check and takes stock of the machine, and tells the server
	 * none of it. Downloading the checks is all this needs the server for, so a
	 * run can be repeated as often as someone likes while they fix what it
	 * found — nothing is on record until {@link #submit} is called.
	 *
	 * @return what a report of this run would say, or empty when the server has
	 *         no checks configured and there is nothing to report
	 */
	public Optional<ReportPayload> check()
	{
		String deviceId = fetchDeviceId();
		String userId = hostname();

		LOG.info("Running compliance checks on device {} (user: {})", deviceId, userId);

		List<CheckItem> checks = client.getChecks();
		if (checks.isEmpty())
		{
			LOG.info("No compliance checks configured on server.");
			return Optional.empty();
		}

		List<ResultPayload> results = runChecks(checks, this::runCheck);
		List<InstalledAppPayload> installedApps = collectInventory();

		long passed = results.stream().filter(ResultPayload::passed).count();
		LOG.info(ConsoleStyle.rule());
		LOG.info(ConsoleStyle.summary(passed, results.size()));

		// Stamped here rather than at submission: this is when the machine
		// looked like this, and someone may sit on the question for a while.
		return Optional.of(new ReportPayload(deviceId, userId, Instant.now(), results, installedApps));
	}

	/** Files a run that has already happened. The report exists from here on. */
	public void submit(ReportPayload run)
	{
		LOG.info("Reporting {} installed applications and packages", run.installedApps().size());
		ReportResponse response = client.pushReport(run);
		LOG.info("View report: {}", response.reportUrl());
	}

	/**
	 * Runs the checks concurrently. Each one is a separate short-lived
	 * {@code osqueryi} process that spends around 250 ms of its life starting
	 * up, so in a row they cost roughly the number of checks times that
	 * startup: 12 invocations measured 3.2 s sequentially against 0.31 s at
	 * once. Nothing serialises them — {@code osqueryi} keeps its database in
	 * memory, unlike {@code osqueryd}, so concurrent instances do not contend
	 * for a RocksDB lock.
	 * <p>
	 * The returned list keeps the order the server sent, whichever check
	 * happened to finish first; only the progress lines arrive in completion
	 * order.
	 *
	 * @param work
	 *            how to run one check, taken as an argument so the concurrency
	 *            can be tested without an osquery installation
	 */
	List<ResultPayload> runChecks(List<CheckItem> checks, Function<CheckItem, ResultPayload> work)
	{
		int threads = Math.max(1, Math.min(parallelism, checks.size()));
		try (ExecutorService executor = Executors.newFixedThreadPool(threads))
		{
			List<Future<ResultPayload>> pending = new ArrayList<>(checks.size());
			for (CheckItem check : checks)
			{
				pending.add(executor.submit(() -> work.apply(check)));
			}
			return pending.stream().map(ComplianceRunner::join).toList();
		}
	}

	private static ResultPayload join(Future<ResultPayload> pending)
	{
		try
		{
			return pending.get();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for a check to finish", e);
		}
		catch (ExecutionException e)
		{
			// runCheck turns a failing check into a failed result itself, so
			// reaching here means the task broke, not the check.
			throw new IllegalStateException("A compliance check ended abnormally", e.getCause());
		}
	}

	private ResultPayload runCheck(CheckItem check)
	{
		try
		{
			String output = osquery(check.query());
			boolean passed = evaluate(output, check.expectedExpression());
			LOG.info("  {} {}", ConsoleStyle.verdict(passed), check.name());
			return new ResultPayload(check.id(), passed, output);
		}
		catch (Exception e)
		{
			LOG.warn("  {} {}", ConsoleStyle.errorTag(), check.name(), e);
			return new ResultPayload(check.id(), false, null);
		}
	}

	/**
	 * Every application bundle and Homebrew package on the machine. The server
	 * needs the whole list — not just the forbidden ones — so a report can show
	 * what is installed and let an admin block anything from it.
	 */
	private List<InstalledAppPayload> collectInventory()
	{
		try
		{
			String output = osquery(INVENTORY_QUERY);
			List<Map<String, Object>> rows = objectMapper.readValue(
				output, new TypeReference<>()
				{
				});
			return rows.stream()
				.map(row -> new InstalledAppPayload(
					string(row.get("source")), string(row.get("name")), string(row.get("identifier")),
					string(row.get("version")), string(row.get("path"))))
				.toList();
		}
		catch (Exception e)
		{
			// A missing inventory must not cost us the compliance results
			LOG.warn("Could not collect installed-app inventory.", e);
			return List.of();
		}
	}

	private static String string(Object value)
	{
		return value == null ? null : value.toString();
	}

	private String osquery(String query) throws IOException, InterruptedException
	{
		ProcessBuilder pb = new ProcessBuilder("osqueryi", "--json", query);
		pb.redirectErrorStream(true);
		Process process = pb.start();
		if (!process.waitFor(10, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new RuntimeException("osqueryi timed out after 10 seconds");
		}
		String output = new String(process.getInputStream().readAllBytes()).strip();
		if (process.exitValue() != 0)
		{
			throw new RuntimeException("osqueryi exited with code " + process.exitValue() + ": " + output);
		}
		return output;
	}

	private boolean evaluate(String jsonOutput, String expression) throws Exception
	{
		List<Map<String, Object>> results = objectMapper.readValue(
			jsonOutput, new TypeReference<List<Map<String, Object>>>()
			{
			});
		JexlContext context = new MapContext();
		context.set("results", results);
		JexlExpression expr = JEXL.createExpression(expression);
		Object result = expr.evaluate(context);
		if (result instanceof Boolean b)
		{
			return b;
		}
		throw new IllegalArgumentException("Expression did not return a boolean: " + result);
	}

	private String fetchDeviceId()
	{
		try
		{
			String output = osquery("SELECT uuid FROM system_info;");
			List<Map<String, Object>> rows = objectMapper.readValue(
				output, new TypeReference<List<Map<String, Object>>>()
				{
				});
			if (!rows.isEmpty() && rows.getFirst().get("uuid") != null)
			{
				return rows.getFirst().get("uuid").toString();
			}
		}
		catch (Exception e)
		{
			LOG.warn("Could not fetch device UUID from osquery, falling back to hostname", e);
		}
		return hostname();
	}

	private String hostname()
	{
		try
		{
			return InetAddress.getLocalHost().getHostName();
		}
		catch (Exception e)
		{
			return "unknown";
		}
	}
}
