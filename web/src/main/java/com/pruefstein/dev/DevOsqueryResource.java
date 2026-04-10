package com.pruefstein.dev;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.pruefstein.compliance.service.ComplianceEvaluator;
import io.quarkus.runtime.LaunchMode;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/dev/osquery")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("${pruefstein.security.admin-role:admin}")
public class DevOsqueryResource
{
	@Inject
	ComplianceEvaluator evaluator;

	@POST
	@Path("/run")
	@Consumes(MediaType.APPLICATION_JSON)
	public OsqueryResult run(JsonNode body)
	{
		if (LaunchMode.current() != LaunchMode.DEVELOPMENT)
		{
			throw new NotFoundException();
		}

		String query = body != null && body.has("query") ? body.get("query").asText() : null;
		String expression = body != null && body.has("expression") ? body.get("expression").asText() : null;

		if (query == null || query.isBlank())
		{
			return OsqueryResult.error("query is required");
		}

		String output;
		try
		{
			ProcessBuilder pb = new ProcessBuilder("osqueryi", "--json", query);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			boolean finished = process.waitFor(10, TimeUnit.SECONDS);
			if (!finished)
			{
				process.destroyForcibly();
				return OsqueryResult.error("osqueryi timed out after 10 seconds");
			}

			output = new String(process.getInputStream().readAllBytes()).strip();
			int exitCode = process.exitValue();

			if (exitCode != 0)
			{
				return OsqueryResult.error(output.isEmpty() ? "osqueryi exited with code " + exitCode : output);
			}
		}
		catch (IOException e)
		{
			String msg = e.getMessage();
			if (msg != null && msg.contains("No such file"))
			{
				return OsqueryResult.error("osqueryi not found — is osquery installed and on PATH?");
			}
			return OsqueryResult.error(msg != null ? msg : "Unknown IO error");
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return OsqueryResult.error("Interrupted");
		}

		if (expression == null || expression.isBlank())
		{
			return OsqueryResult.withOutput(output, null);
		}

		try
		{
			boolean passed = evaluator.evaluate(output, expression);
			return OsqueryResult.withOutput(output, passed);
		}
		catch (Exception e)
		{
			return OsqueryResult.withOutput(output, null).withExpressionError(e.getMessage());
		}
	}

	public record OsqueryResult(String output, Boolean passed, String error, String expressionError)
	{
		static OsqueryResult withOutput(String output, Boolean passed)
		{
			return new OsqueryResult(output, passed, null, null);
		}

		OsqueryResult withExpressionError(String msg)
		{
			return new OsqueryResult(this.output, null, null, msg);
		}

		static OsqueryResult error(String error)
		{
			return new OsqueryResult(null, null, error, null);
		}
	}
}
