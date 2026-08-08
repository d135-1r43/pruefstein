package com.pruefstein.compliance.api;

import java.util.List;

import com.pruefstein.compliance.domain.AppBlacklistCheck;
import com.pruefstein.compliance.domain.AppMatcher;
import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.domain.MatcherType;
import com.pruefstein.compliance.repository.BlockedAppRepository;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.service.CheckResolver;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BlockedAppsTest
{
	@Inject
	BlockedAppRepository blockedAppRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	CheckResolver checkResolver;

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			blockedAppRepository.delete("label like ?1", "ZZ Test%");
			itemRepository.delete("name", "ZZ Test Blacklist Check");
		});
	}

	private Long seedBlockedApp()
	{
		Long[] id = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			BlockedApp app = new BlockedApp();
			app.setLabel("ZZ Test Nextcloud");
			app.setReason("Data must stay in the approved tenant");
			app.setEnabled(true);
			app.setMatchers(List.of(
				new AppMatcher(MatcherType.BUNDLE_ID, "com.nextcloud.%"),
				new AppMatcher(MatcherType.HOMEBREW, "nextcloud")));
			blockedAppRepository.persist(app);
			id[0] = app.id;
		});
		return id[0];
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void createParsesOneMatcherPerLinePerType()
	{
		// given / when
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("label", "ZZ Test Multi")
			.formParam("reason", "Not approved")
			.formParam("bundleIds", "com.example.one\ncom.example.two")
			.formParam("homebrewNames", "examplebrew")
			.formParam("appNames", "")
			.when().post("/BlockedApps/create")
			.then()
			.statusCode(lessThan(400));

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			BlockedApp app = blockedAppRepository.find("label", "ZZ Test Multi").firstResult();
			assertNotNull(app);
			assertEquals(2, app.matchersOf(MatcherType.BUNDLE_ID).size());
			assertEquals(1, app.matchersOf(MatcherType.HOMEBREW).size());
			assertTrue(app.matchersOf(MatcherType.APP_NAME).isEmpty());
		});
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void updateReplacesMatchersRatherThanAppendingThem()
	{
		// given
		Long id = seedBlockedApp();

		// when
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", id)
			.formParam("label", "ZZ Test Nextcloud")
			.formParam("reason", "Still not approved")
			.formParam("bundleIds", "com.nextcloud.desktopclient")
			.formParam("homebrewNames", "")
			.formParam("appNames", "")
			.formParam("enabled", "true")
			.when().post("/BlockedApps/update")
			.then()
			.statusCode(lessThan(400));

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			BlockedApp app = blockedAppRepository.findById(id);
			assertEquals(1, app.getMatchers().size());
			assertEquals("com.nextcloud.desktopclient", app.getMatchers().getFirst().getPattern());
		});
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void uncheckedEnabledBoxDisablesTheRule()
	{
		// given
		Long id = seedBlockedApp();

		// when — an unchecked checkbox submits no value at all
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", id)
			.formParam("label", "ZZ Test Nextcloud")
			.formParam("bundleIds", "com.nextcloud.%")
			.when().post("/BlockedApps/update")
			.then()
			.statusCode(lessThan(400));

		// then
		QuarkusTransaction.requiringNew()
			.run(() -> assertFalse(blockedAppRepository.findById(id).isEnabled()));
	}

	@Test
	@TestSecurity(user = "agent", roles = { "user" })
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "agent") })
	void checksEndpointServesGeneratedSqlForBlacklistItems()
	{
		// given
		seedBlockedApp();
		QuarkusTransaction.requiringNew().run(() -> {
			AppBlacklistCheck item = new AppBlacklistCheck();
			item.setName("ZZ Test Blacklist Check");
			itemRepository.persist(item);
		});

		// when / then — the item stores no SQL; the server renders it per
		// request
		given()
			.when().get("/api/checks")
			.then()
			.statusCode(200)
			.body("find { it.name == 'ZZ Test Blacklist Check' }.query",
				allOf(containsString("FROM apps"), containsString("FROM homebrew_packages"),
					containsString("com.nextcloud.%")))
			.body("find { it.name == 'ZZ Test Blacklist Check' }.expectedExpression",
				equalTo("results.size() == 0"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void groupPageNeverListsTheGeneratedCheck()
	{
		// given — even if a blacklist check were assigned to a group, it
		// belongs
		// on the Blocked Apps screen, not under Groups & Items
		seedBlockedApp();
		Long[] groupId = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceGroup group = new ComplianceGroup();
			group.setName("ZZ Test Group");
			groupRepository.persist(group);
			groupId[0] = group.id;

			AppBlacklistCheck item = new AppBlacklistCheck();
			item.setName("ZZ Test Blacklist Check");
			item.setGroup(group);
			itemRepository.persist(item);
		});

		// when / then
		given()
			.when().get("/ComplianceGroups/show/" + groupId[0])
			.then()
			.statusCode(200)
			.body(not(containsString("ZZ Test Blacklist Check")));

		// cleanup
		QuarkusTransaction.requiringNew().run(() -> {
			itemRepository.delete("group.id", groupId[0]);
			groupRepository.deleteById(groupId[0]);
		});
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void blockedAppsPageCarriesTheCheckAndItsGeneratedSql()
	{
		// given
		seedBlockedApp();
		QuarkusTransaction.requiringNew().run(() -> {
			AppBlacklistCheck item = new AppBlacklistCheck();
			item.setName("ZZ Test Blacklist Check");
			itemRepository.persist(item);
		});

		// when / then — the check and its SQL live here now
		given()
			.when().get("/BlockedApps/index")
			.then()
			.statusCode(200)
			.body(allOf(
				containsString("ZZ Test Blacklist Check"),
				containsString("homebrew_packages"),
				containsString("ZZ Test Nextcloud")));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void checksLoadBackAsTheirOwnTypeAndResolveAccordingly()
	{
		// given — two checks of different types in the same table
		seedBlockedApp();
		Long[] ids = new Long[2];
		QuarkusTransaction.requiringNew().run(() -> {
			AppBlacklistCheck generated = new AppBlacklistCheck();
			generated.setName("ZZ Test Blacklist Check");
			itemRepository.persist(generated);
			ids[0] = generated.id;

			ExpressionCheck authored = new ExpressionCheck();
			authored.setName("ZZ Test Authored Check");
			authored.setQuery("SELECT 1;");
			authored.setExpectedExpression("results.size() > 0");
			itemRepository.persist(authored);
			ids[1] = authored.id;
		});

		// when / then — the discriminator brings each row back as its subclass
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceItem generated = itemRepository.findById(ids[0]);
			ComplianceItem authored = itemRepository.findById(ids[1]);

			assertInstanceOf(AppBlacklistCheck.class, generated);
			assertInstanceOf(ExpressionCheck.class, authored);
			assertFalse(generated.isEditable());
			assertTrue(authored.isEditable());

			assertTrue(checkResolver.resolve(generated).query().contains("homebrew_packages"));
			assertEquals("SELECT 1;", checkResolver.resolve(authored).query());
		});

		// cleanup
		QuarkusTransaction.requiringNew().run(() -> itemRepository.delete("name", "ZZ Test Authored Check"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void generatedChecksCannotBeEditedThroughTheItemForm()
	{
		// given
		Long[] id = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			AppBlacklistCheck check = new AppBlacklistCheck();
			check.setName("ZZ Test Blacklist Check");
			itemRepository.persist(check);
			id[0] = check.id;
		});

		// when / then — the UI hides this, but the endpoint must refuse it too
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", id[0])
			.formParam("name", "Hijacked")
			.formParam("query", "SELECT 1;")
			.formParam("expectedExpression", "true")
			.when().post("/ComplianceGroups/updateItem")
			.then()
			.statusCode(400);

		QuarkusTransaction.requiringNew()
			.run(() -> assertEquals("ZZ Test Blacklist Check", itemRepository.findById(id[0]).getName()));
	}

	@Test
	@TestSecurity(user = "agent", roles = { "user" })
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "agent") })
	void disabledRulesAreLeftOutOfTheGeneratedSql()
	{
		// given
		Long id = seedBlockedApp();
		QuarkusTransaction.requiringNew().run(() -> {
			blockedAppRepository.findById(id).setEnabled(false);
			AppBlacklistCheck item = new AppBlacklistCheck();
			item.setName("ZZ Test Blacklist Check");
			itemRepository.persist(item);
		});

		// when / then
		given()
			.when().get("/api/checks")
			.then()
			.statusCode(200)
			.body("find { it.name == 'ZZ Test Blacklist Check' }.query", not(containsString("com.nextcloud")));
	}
}
