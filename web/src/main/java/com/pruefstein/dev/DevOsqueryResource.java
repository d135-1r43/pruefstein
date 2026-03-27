package com.pruefstein.dev;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.LaunchMode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/dev/osquery")
@Produces(MediaType.APPLICATION_JSON)
public class DevOsqueryResource
{

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
		if (query == null || query.isBlank())
		{
			return OsqueryResult.error("query is required");
		}

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

			String output = new String(process.getInputStream().readAllBytes()).strip();
			int exitCode = process.exitValue();

			if (exitCode != 0)
			{
				return OsqueryResult.error(output.isEmpty() ? "osqueryi exited with code " + exitCode : output);
			}

			return OsqueryResult.ok(output);
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
	}

	public record OsqueryResult(String output, String error)
	{
		static OsqueryResult ok(String output)
		{
			return new OsqueryResult(output, null);
		}

		static OsqueryResult error(String error)
		{
			return new OsqueryResult(null, error);
		}
	}
}
