package com.pruefstein.compliance.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface ComplianceItemAiService
{
	@SystemMessage(fromResource = "prompts/suggest-check-system.txt")
	@UserMessage("Generate a compliance check for: {description}")
	ComplianceItemSuggestion suggest(String description, String schema);
}
