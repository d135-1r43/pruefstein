package com.pruefstein.compliance.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

@ApplicationScoped
public class ComplianceEvaluator
{
	private static final JexlEngine JEXL = new JexlBuilder().strict(true).silent(false).create();

	@Inject
	ObjectMapper objectMapper;

	public boolean evaluate(String jsonOutput, String expression) throws Exception
	{
		List<Map<String, Object>> results = objectMapper.readValue(
			jsonOutput,
			new TypeReference<List<Map<String, Object>>>()
			{
			});

		JexlContext context = new MapContext();
		context.set("results", results);

		JexlExpression expr = JEXL.createExpression(expression);
		Object result = expr.evaluate(context);

		if (result instanceof Boolean b)
		{
			return b;
		}
		throw new IllegalArgumentException("Expression did not return a boolean: " + result);
	}
}
