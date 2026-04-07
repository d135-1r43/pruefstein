package com.pruefstein.compliance.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ComplianceEvaluatorTest
{
	@Inject
	ComplianceEvaluator evaluator;

	@Test
	void returnsTrueWhenExpressionPasses() throws Exception
	{
		// given
		String json = "[{\"encrypted\":\"1\"}]";

		// when
		boolean result = evaluator.evaluate(json, "results[0].encrypted == \"1\"");

		// then
		assertTrue(result);
	}

	@Test
	void returnsFalseWhenExpressionFails() throws Exception
	{
		// given
		String json = "[{\"encrypted\":\"0\"}]";

		// when
		boolean result = evaluator.evaluate(json, "results[0].encrypted == \"1\"");

		// then
		assertFalse(result);
	}

	@Test
	void evaluatesSizeExpression() throws Exception
	{
		// given
		String json = "[{\"pid\":\"1\"},{\"pid\":\"2\"}]";

		// when
		boolean result = evaluator.evaluate(json, "results.size() == 2");

		// then
		assertTrue(result);
	}

	@Test
	void emptyResultsEvaluatesCorrectly() throws Exception
	{
		// given
		String json = "[]";

		// when
		boolean result = evaluator.evaluate(json, "results.isEmpty()");

		// then
		assertTrue(result);
	}

	@Test
	void combinedExpressionWithMultipleRows() throws Exception
	{
		// given
		String json = "[{\"status\":\"on\"},{\"status\":\"on\"}]";

		// when
		boolean result = evaluator.evaluate(json, "results.size() > 0 && results[0].status == \"on\"");

		// then
		assertTrue(result);
	}

	@Test
	void throwsWhenExpressionReturnsNonBoolean() throws Exception
	{
		// given
		String json = "[{\"count\":\"5\"}]";

		// when / then
		assertThrows(IllegalArgumentException.class,
			() -> evaluator.evaluate(json, "results.size()"));
	}

	@Test
	void throwsOnInvalidJson()
	{
		// given
		String invalidJson = "not-json";

		// when / then
		assertThrows(Exception.class,
			() -> evaluator.evaluate(invalidJson, "results.isEmpty()"));
	}
}
