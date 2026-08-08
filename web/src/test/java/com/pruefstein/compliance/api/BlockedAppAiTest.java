package com.pruefstein.compliance.api;

import java.util.List;

import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.MatcherType;
import com.pruefstein.compliance.repository.BlockedAppRepository;
import com.pruefstein.compliance.service.BlockedAppAiService;
import com.pruefstein.compliance.service.BlockedAppSuggestion;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class BlockedAppAiTest
{
	@InjectMock
	BlockedAppAiService aiService;

	@Inject
	BlockedAppRepository repository;

	@BeforeEach
	void setUp()
	{
		when(aiService.suggest(any(), any())).thenReturn(new BlockedAppSuggestion(
			"Nextcloud Desktop",
			"Company data must stay in the approved tenant.",
			List.of("com.nextcloud.desktopclient"),
			List.of("Nextcloud.app"),
			List.of("nextcloud")));
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> repository.delete("label like ?1", "ZZ Ai%"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void suggestEndpointReturnsEveryInstallRoute()
	{
		// given / when / then
		given()
			.contentType(JSON)
			.body("{\"description\":\"Nextcloud desktop sync client\"}")
			.when().post("/ai/suggest-blocked-app")
			.then()
			.statusCode(200)
			.body("label", equalTo("Nextcloud Desktop"))
			.body("bundleIds", hasItem("com.nextcloud.desktopclient"))
			.body("homebrewNames", hasItem("nextcloud"))
			.body("appNames", hasItem("Nextcloud.app"));
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotUseTheSuggestEndpoint()
	{
		// given / when / then
		given()
			.contentType(JSON)
			.body("{\"description\":\"anything\"}")
			.when().post("/ai/suggest-blocked-app")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void blockingWithAiExpandsTheObservedMatcherToEveryInstallRoute()
	{
		// given — the report only ever sees one install route
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("label", "ZZ Ai Nextcloud")
			.formParam("matcherType", "BUNDLE_ID")
			.formParam("pattern", "com.nextcloud.desktopclient")
			.formParam("useAi", "true")
			.when().post("/BlockedApps/blockFromReport")
			.then()
			.statusCode(lessThan(400));

		// then — the AI's other routes are merged in, without duplicating the
		// observed one
		QuarkusTransaction.requiringNew().run(() -> {
			BlockedApp app = repository.find("label", "ZZ Ai Nextcloud").firstResult();
			assertNotNull(app);
			assertEquals(1, app.matchersOf(MatcherType.BUNDLE_ID).size());
			assertEquals(1, app.matchersOf(MatcherType.HOMEBREW).size());
			assertEquals(1, app.matchersOf(MatcherType.APP_NAME).size());
			assertEquals("nextcloud", app.matchersOf(MatcherType.HOMEBREW).getFirst().getPattern());
			assertNotNull(app.getReason());
		});
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void blockingWithoutAiKeepsOnlyTheObservedMatcher()
	{
		// given / when
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("label", "ZZ Ai Plain")
			.formParam("matcherType", "BUNDLE_ID")
			.formParam("pattern", "com.example.plain")
			.when().post("/BlockedApps/blockFromReport")
			.then()
			.statusCode(lessThan(400));

		// then
		verify(aiService, never()).suggest(any(), any());
		QuarkusTransaction.requiringNew().run(() -> {
			BlockedApp app = repository.find("label", "ZZ Ai Plain").firstResult();
			assertEquals(1, app.getMatchers().size());
		});
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void anAiFailureStillLeavesAWorkingRule()
	{
		// given — the model is unreachable
		when(aiService.suggest(any(), any())).thenThrow(new RuntimeException("model unavailable"));

		// when
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("label", "ZZ Ai Fallback")
			.formParam("matcherType", "HOMEBREW")
			.formParam("pattern", "sometool")
			.formParam("useAi", "true")
			.when().post("/BlockedApps/blockFromReport")
			.then()
			.statusCode(lessThan(400));

		// then — the observed matcher survives on its own
		QuarkusTransaction.requiringNew().run(() -> {
			BlockedApp app = repository.find("label", "ZZ Ai Fallback").firstResult();
			assertNotNull(app);
			assertEquals(1, app.getMatchers().size());
			assertEquals("sometool", app.getMatchers().getFirst().getPattern());
		});
	}
}
