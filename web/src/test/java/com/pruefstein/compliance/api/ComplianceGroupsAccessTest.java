package com.pruefstein.compliance.api;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
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
class ComplianceGroupsAccessTest
{
	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	private Long groupId;
	private Long itemId;

	@BeforeEach
	void setUp()
	{
		Long[] ids = new Long[2];
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceGroup group = new ComplianceGroup();
			group.setName("Access Test Group");
			groupRepository.persist(group);
			ids[0] = group.id;

			ExpressionCheck item = new ExpressionCheck();
			item.setName("Access Test Item");
			item.setQuery("SELECT 1;");
			item.setExpectedExpression("results.size() > 0");
			item.setGroup(group);
			itemRepository.persist(item);
			ids[1] = item.id;
		});
		groupId = ids[0];
		itemId = ids[1];
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			itemRepository.delete("group.id", groupId);
			groupRepository.deleteById(groupId);
		});
	}

	// ── Groups ───────────────────────────────────────────────────────────────

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotCreateGroup()
	{
		// given (regular user without admin role)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("name", "Forbidden Group")
			.when().post("/ComplianceGroups/create")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotUpdateGroup()
	{
		// given (group seeded in setUp)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", groupId)
			.formParam("name", "Renamed Group")
			.when().post("/ComplianceGroups/update")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotDeleteGroup()
	{
		// given (group seeded in setUp)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", groupId)
			.when().post("/ComplianceGroups/delete")
			.then()
			.statusCode(403);
	}

	// ── Items ────────────────────────────────────────────────────────────────

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotCreateItem()
	{
		// given (group seeded in setUp)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("groupId", groupId)
			.formParam("name", "Forbidden Item")
			.formParam("query", "SELECT 1;")
			.formParam("expectedExpression", "true")
			.when().post("/ComplianceGroups/createItem")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotUpdateItem()
	{
		// given (item seeded in setUp)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", itemId)
			.formParam("name", "Changed Name")
			.formParam("query", "SELECT 2;")
			.formParam("expectedExpression", "true")
			.when().post("/ComplianceGroups/updateItem")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotDeleteItem()
	{
		// given (item seeded in setUp)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", itemId)
			.when().post("/ComplianceGroups/deleteItem")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void adminCanCreateGroup()
	{
		// given (admin user)

		// when / then — admin is not blocked (200 or 302, not 403)
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("name", "Admin Created Group")
			.when().post("/ComplianceGroups/create")
			.then()
			.statusCode(lessThan(400));

		// cleanup
		QuarkusTransaction.requiringNew().run(() -> groupRepository.delete("name", "Admin Created Group"));
	}
}
