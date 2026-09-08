package com.pruefstein.compliance.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.pruefstein.compliance.service.BlockedAppAiService;
import com.pruefstein.compliance.service.BlockedAppSuggestion;
import com.pruefstein.compliance.service.ComplianceItemAiService;
import com.pruefstein.compliance.service.ComplianceItemSuggestion;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/ai")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("${pruefstein.security.admin-role:admin}")
public class AiSuggestResource
{
	@Inject
	ComplianceItemAiService aiService;

	@Inject
	BlockedAppAiService blockedAppAiService;

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
			throw new BadRequestException("description is required");
		}
		return aiService.suggest(request.description(), schema);
	}

	public record BlockedAppRequest(String description, String knownFacts)
	{
	}

	/**
	 * Expands an application name into every identifier that recognises it —
	 * bundle IDs, Homebrew names, bundle filenames — so one rule catches the
	 * app however it was installed.
	 */
	@POST
	@Path("/suggest-blocked-app")
	@Consumes(MediaType.APPLICATION_JSON)
	public BlockedAppSuggestion suggestBlockedApp(BlockedAppRequest request)
	{
		if (request == null || request.description() == null || request.description().isBlank())
		{
			throw new InternalServerErrorException("description is required");
		}
		return blockedAppAiService.suggest(request.description(),
			request.knownFacts() == null ? "" : request.knownFacts());
	}
}
