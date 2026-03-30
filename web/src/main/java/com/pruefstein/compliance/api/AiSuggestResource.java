package com.pruefstein.compliance.api;

import com.pruefstein.compliance.service.ComplianceItemAiService;
import com.pruefstein.compliance.service.ComplianceItemSuggestion;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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

	public record SuggestRequest(String description)
	{
	}

	@POST
	@Path("/suggest-check")
	@Consumes(MediaType.APPLICATION_JSON)
	public ComplianceItemSuggestion suggest(SuggestRequest request)
	{
		return aiService.suggest(request.description());
	}
}
