package com.pruefstein.compliance.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.pruefstein.compliance.service.ComplianceItemAiService;
import com.pruefstein.compliance.service.ComplianceItemSuggestion;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ai")
@Produces(MediaType.APPLICATION_JSON)
public class AiSuggestResource
{
	@Inject
	ComplianceItemAiService aiService;

	private String schema;

	public record SuggestRequest(String description)
	{
	}

	@PostConstruct
	void loadSchema()
	{
		try (InputStream in = Thread.currentThread().getContextClassLoader()
			.getResourceAsStream("osquery/compliance-schema.txt"))
		{
			if (in == null)
			{
				throw new IllegalStateException("osquery/compliance-schema.txt not found on classpath");
			}
			schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Failed to load osquery schema", e);
		}
	}

	@POST
	@Path("/suggest-check")
	@Consumes(MediaType.APPLICATION_JSON)
	public ComplianceItemSuggestion suggest(SuggestRequest request)
	{
		if (request == null || request.description() == null || request.description().isBlank())
		{
			throw new InternalServerErrorException("description is required");
		}
		return aiService.suggest(request.description(), schema);
	}
}
