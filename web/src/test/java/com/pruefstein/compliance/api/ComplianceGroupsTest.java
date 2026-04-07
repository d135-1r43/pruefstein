package com.pruefstein.compliance.api;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class ComplianceGroupsTest
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
			group.setName("A.10 Cryptography");
			groupRepository.persist(group);
			ids[0] = group.id;

			ComplianceItem item = new ComplianceItem();
			item.setName("Disk encryption enabled");
			item.setQuery("SELECT encrypted FROM mounts WHERE path = '/';");
			item.setExpectedExpression("results[0].encrypted == \"1\"");
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
			itemRepository.deleteById(itemId);
			groupRepository.deleteById(groupId);
		});
	}

	@Test
	void testIndexReturns200()
	{
		// given (group seeded in setUp)

		// when / then
		given()
			.when().get("/ComplianceGroups/index")
			.then()
			.statusCode(200)
			.contentType(containsString("text/html"))
			.body(containsString("Compliance Groups"));
	}

	@Test
	void testIndexContainsExistingGroup()
	{
		// given (group seeded in setUp)

		// when / then
		given()
			.when().get("/ComplianceGroups/index")
			.then()
			.statusCode(200)
			.body(containsString("A.10 Cryptography"));
	}

	@Test
	void testShowReturns200ForExistingGroup()
	{
		// given (group seeded in setUp)

		// when / then
		given()
			.when().get("/ComplianceGroups/show/" + groupId)
			.then()
			.statusCode(200)
			.contentType(containsString("text/html"))
			.body(containsString("A.10 Cryptography"));
	}

	@Test
	void testShowContainsItem()
	{
		// given (group and item seeded in setUp)

		// when / then
		given()
			.when().get("/ComplianceGroups/show/" + groupId)
			.then()
			.statusCode(200)
			.body(containsString("Disk encryption enabled"))
			.body(containsString("SELECT encrypted FROM mounts"));
	}

	@Test
	void testShowReturns404ForUnknownGroup()
	{
		// given
		long unknownId = Long.MAX_VALUE;

		// when / then
		given()
			.when().get("/ComplianceGroups/show/" + unknownId)
			.then()
			.statusCode(404);
	}
}
