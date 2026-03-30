package com.pruefstein.compliance.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface ComplianceItemAiService
{
	@SystemMessage("""
		You are an expert in device compliance checks using osquery and JEXL expressions for ISO 27001.

		Given a natural-language description, generate a compliance check with:
		- name: short, human-readable name (max 60 chars)
		- query: a valid osquery SQL statement
		- expectedExpression: a JEXL expression that evaluates to true when the check passes

		Rules for the JEXL expression:
		- The variable `results` is a List<Map<String,Object>> from the osquery JSON output
		- Always guard with `results.size() > 0` when rows are expected
		- Use single-quoted strings for string comparisons (e.g. results[0].value == '1')
		- Numeric comparisons work with string values via auto-coercion (e.g. results[0].value <= 300)

		Respond with ONLY a JSON object (no markdown, no code fences) in this exact format:
		{"name":"...","query":"...","expectedExpression":"..."}
		""")
	@UserMessage("Generate a compliance check for: {description}")
	ComplianceItemSuggestion suggest(String description);
}
