package com.pruefstein.agent.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

	@Inject
	ObjectMapper objectMapper;

	public void runAll()
	{
		String deviceId = fetchDeviceId();
		String userId = hostname();

		LOG.info("Running compliance checks on device {} (user: {})", deviceId, userId);

		List<CheckItem> checks = client.getChecks();
		if (checks.isEmpty())
		{
			LOG.info("No compliance checks configured on server.");
			return;
		}

		List<ResultPayload> results = new ArrayList<>();
		for (CheckItem check : checks)
		{
			results.add(runCheck(check));
		}

		List<InstalledAppPayload> installedApps = collectInventory();
		LOG.info("Reporting {} installed applications and packages", installedApps.size());

		ReportResponse response = client.pushReport(
			new ReportPayload(deviceId, userId, Instant.now(), results, installedApps));

		long passed = results.stream().filter(ResultPayload::passed).count();
		LOG.info("Done: {}/{} checks passed", passed, results.size());
		LOG.info("View report: {}", response.reportUrl());
	}

	private ResultPayload runCheck(CheckItem check)
	{
		try
		{
			String output = osquery(check.query());
			boolean passed = evaluate(output, check.expectedExpression());
			LOG.info("  [{}] {}", passed ? "PASS" : "FAIL", check.name());
			return new ResultPayload(check.id(), passed, output);
		}
		catch (Exception e)
		{
			LOG.warn("  [ERROR] {}", check.name(), e);
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
