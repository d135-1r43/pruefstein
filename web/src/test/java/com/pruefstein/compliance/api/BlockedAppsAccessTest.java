package com.pruefstein.compliance.api;

import java.util.List;

import com.pruefstein.compliance.domain.AppMatcher;
import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.MatcherType;
import com.pruefstein.compliance.repository.BlockedAppRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThan;

@QuarkusTest
class BlockedAppsAccessTest
{
	@Inject
	BlockedAppRepository repository;

	private Long blockedAppId;

	@BeforeEach
	void setUp()
	{
		Long[] id = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			BlockedApp app = new BlockedApp();
			app.setLabel("ZZ Access Test App");
			app.setEnabled(true);
			app.setMatchers(List.of(new AppMatcher(MatcherType.HOMEBREW, "accesstest")));
			repository.persist(app);
			id[0] = app.id;
		});
		blockedAppId = id[0];
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> repository.delete("label like ?1", "ZZ Access Test%"));
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCanViewTheBlacklist()
	{
		// given (regular user without admin role)

		// when / then — the list is readable by anyone signed in
		given()
			.when().get("/BlockedApps/index")
			.then()
			.statusCode(lessThan(400));
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotBlockAnApp()
	{
		// given (regular user without admin role)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("label", "ZZ Access Test Forbidden")
			.formParam("homebrewNames", "forbidden")
			.when().post("/BlockedApps/create")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotUpdateABlockedApp()
	{
		// given (rule seeded in setUp)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", blockedAppId)
			.formParam("label", "ZZ Access Test Renamed")
			.when().post("/BlockedApps/update")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotUnblockAnApp()
	{
		// given (rule seeded in setUp)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", blockedAppId)
			.when().post("/BlockedApps/delete")
			.then()
			.statusCode(403);
	}
}
