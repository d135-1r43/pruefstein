package com.pruefstein.compliance.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface BlockedAppAiService
{
	@SystemMessage(fromResource = "prompts/suggest-blocked-app-system.txt")
	@UserMessage("""
		Application to block: {description}

		Anything already known about it from the device inventory (may be blank):
		{knownFacts}
		""")
	BlockedAppSuggestion suggest(String description, String knownFacts);
}
