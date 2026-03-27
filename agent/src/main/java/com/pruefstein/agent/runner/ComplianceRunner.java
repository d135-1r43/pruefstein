package com.pruefstein.agent.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruefstein.agent.client.CheckItem;
import com.pruefstein.agent.client.PruefsteinClient;
import com.pruefstein.agent.client.ReportPayload;
import com.pruefstein.agent.client.ResultPayload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ComplianceRunner
{

	private static final Logger LOG = Logger.getLogger(ComplianceRunner.class);
	private static final JexlEngine JEXL = new JexlBuilder().strict(true).silent(false).create();

	@RestClient
	PruefsteinClient client;

	@Inject
	ObjectMapper objectMapper;

	public void runAll()
	{
		String deviceId = fetchDeviceId();
		String userId = hostname();

		LOG.infof("Running compliance checks on device %s (user: %s)", deviceId, userId);

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

		client.pushReport(new ReportPayload(deviceId, userId, Instant.now(), results));

		long passed = results.stream().filter(ResultPayload::passed).count();
		LOG.infof("Done: %d/%d checks passed", passed, results.size());
	}

	private ResultPayload runCheck(CheckItem check)
	{
		try
		{
			String output = osquery(check.query());
			boolean passed = evaluate(output, check.expectedExpression());
			LOG.infof("  [%s] %s", passed ? "PASS" : "FAIL", check.name());
			return new ResultPayload(check.id(), passed, output);
		}
		catch (Exception e)
		{
			LOG.warnf("  [ERROR] %s — %s", check.name(), e.getMessage());
			return new ResultPayload(check.id(), false, null);
		}
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
			if (!rows.isEmpty() && rows.get(0).get("uuid") != null)
			{
				return rows.get(0).get("uuid").toString();
			}
		}
		catch (Exception e)
		{
			LOG.warn("Could not fetch device UUID from osquery, falling back to hostname: " + e.getMessage());
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
