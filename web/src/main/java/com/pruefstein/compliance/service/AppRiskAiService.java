package com.pruefstein.compliance.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface AppRiskAiService
{
	@SystemMessage(fromResource = "prompts/analyze-apps-system.txt")
	@UserMessage("""
		Installed applications, one per line as: number | source | name | identifier

		{apps}
		""")
	AppRiskAnalysis analyze(String apps);
}
