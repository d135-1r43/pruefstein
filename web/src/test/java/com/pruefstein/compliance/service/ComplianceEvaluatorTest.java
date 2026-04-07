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
		String json = "[{\"encrypted\":\"1\"}]";
		assertTrue(evaluator.evaluate(json, "results[0].encrypted == \"1\""));
	}

	@Test
	void returnsFalseWhenExpressionFails() throws Exception
	{
		String json = "[{\"encrypted\":\"0\"}]";
		assertFalse(evaluator.evaluate(json, "results[0].encrypted == \"1\""));
	}

	@Test
	void evaluatesSizeExpression() throws Exception
	{
		String json = "[{\"pid\":\"1\"},{\"pid\":\"2\"}]";
		assertTrue(evaluator.evaluate(json, "results.size() == 2"));
	}

	@Test
	void emptyResultsEvaluatesCorrectly() throws Exception
	{
		String json = "[]";
		assertTrue(evaluator.evaluate(json, "results.isEmpty()"));
	}

	@Test
	void combinedExpressionWithMultipleRows() throws Exception
	{
		String json = "[{\"status\":\"on\"},{\"status\":\"on\"}]";
		assertTrue(evaluator.evaluate(json, "results.size() > 0 && results[0].status == \"on\""));
	}

	@Test
	void throwsWhenExpressionReturnsNonBoolean() throws Exception
	{
		String json = "[{\"count\":\"5\"}]";
		assertThrows(IllegalArgumentException.class,
			() -> evaluator.evaluate(json, "results.size()"));
	}

	@Test
	void throwsOnInvalidJson()
	{
		assertThrows(Exception.class,
			() -> evaluator.evaluate("not-json", "results.isEmpty()"));
	}
}
