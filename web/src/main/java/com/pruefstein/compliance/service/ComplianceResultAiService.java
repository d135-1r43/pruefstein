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

	@SystemMessage(fromResource = "prompts/blacklist-explanation-system.txt")
	@UserMessage("""
		Forbidden applications detected on the device (osquery JSON):
		{output}

		Why these applications are not permitted:
		{reasons}
		""")
	ComplianceResultExplanation explainBlacklist(String output, String reasons);
}
