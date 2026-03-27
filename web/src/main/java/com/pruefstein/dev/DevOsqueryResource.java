package com.pruefstein.dev;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.LaunchMode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/dev/osquery")
@Produces(MediaType.APPLICATION_JSON)
public class DevOsqueryResource
{

	@POST
	@Path("/run")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response run(OsqueryRequest request)
	{
		if (LaunchMode.current() != LaunchMode.DEVELOPMENT)
		{
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		try
		{
			ProcessBuilder pb = new ProcessBuilder("osqueryi", "--json", request.query());
			pb.redirectErrorStream(true);
			Process process = pb.start();

			boolean finished = process.waitFor(10, TimeUnit.SECONDS);
			if (!finished)
			{
				process.destroyForcibly();
				return Response.ok(Map.of("error", "osqueryi timed out after 10 seconds")).build();
			}

			String output = new String(process.getInputStream().readAllBytes()).strip();
			int exitCode = process.exitValue();

			if (exitCode != 0)
			{
				return Response.ok(Map.of("error", output.isEmpty() ? "osqueryi exited with code " + exitCode : output)).build();
			}

			return Response.ok(Map.of("output", output)).build();
		}
		catch (IOException e)
		{
			String message = e.getMessage() != null && e.getMessage().contains("No such file")
				? "osqueryi not found — is osquery installed and on PATH?"
				: e.getMessage();
			return Response.ok(Map.of("error", message)).build();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return Response.ok(Map.of("error", "Interrupted")).build();
		}
	}

	public record OsqueryRequest(String query)
	{
	}
}
