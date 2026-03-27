package com.pruefstein.agent.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "pruefstein-api")
@Produces(MediaType.APPLICATION_JSON)
public interface PruefsteinClient
{

	@GET
	@Path("/api/checks")
	List<CheckItem> getChecks();

	@POST
	@Path("/api/reports")
	@Consumes(MediaType.APPLICATION_JSON)
	void pushReport(ReportPayload report);
}
