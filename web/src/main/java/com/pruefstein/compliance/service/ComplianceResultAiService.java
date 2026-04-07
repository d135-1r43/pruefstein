package com.pruefstein.compliance.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface ComplianceResultAiService
{
	@SystemMessage(fromResource = "prompts/result-explanation-system.txt")
	@UserMessage("""
		Compliance check "{checkName}" failed.
		osquery query: {query}
		Expected to pass when: {expectedExpression}
		Actual output: {output}
		""")
	ComplianceResultExplanation explain(String checkName, String query,
		String expectedExpression, String output);
}
