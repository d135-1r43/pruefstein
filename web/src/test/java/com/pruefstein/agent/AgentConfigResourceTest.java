package com.pruefstein.agent;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AgentConfigResourceTest
{
	/**
	 * Unauthenticated on purpose: the agent asks for this before it has a
	 * token, so an authenticated endpoint would be a chicken-and-egg problem.
	 */
	@Test
	void bootstrapIsReadableWithoutATokenAndAsksForRefreshableCredentials()
	{
		given()
			.when().get("/internal/agent-config")
			.then()
			.statusCode(200)
			.contentType(JSON)
			.body("clientId", notNullValue())
			.body("scopes", containsString("offline_access"));
	}
}
