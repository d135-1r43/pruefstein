package com.pruefstein.agent;

import java.time.Instant;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.device.repository.DeviceRepository;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestSecurity(user = "testuser", roles = "user")
@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "testuser") })
class AgentResourceTest
{
	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceResultRepository resultRepository;

	@Inject
	ReportRepository reportRepository;

	@Inject
	DeviceRepository deviceRepository;

	private Long itemId;
	private Long groupId;

	@BeforeEach
	void setUp()
	{
		Long[] ids = new Long[2];
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceGroup group = new ComplianceGroup();
			group.setName("Test Group");
			groupRepository.persist(group);
			ids[0] = group.id;

			ExpressionCheck item = new ExpressionCheck();
			item.setName("Test Check");
			item.setQuery("SELECT 1;");
			item.setExpectedExpression("results[0] == 1");
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
			resultRepository.delete("report.deviceId", "test-device");
			reportRepository.delete("deviceId", "test-device");
			deviceRepository.delete("deviceId", "test-device");
			itemRepository.deleteById(itemId);
			groupRepository.deleteById(groupId);
		});
	}

	@Test
	void pushCompliantReportReturnsReportUrl()
	{
		// given
		String body = """
			{"deviceId":"test-device","userId":"test-user","checkedAt":"%s",
			 "results":[{"itemId":%d,"passed":true,"output":"ok"}]}
			""".formatted(Instant.now(), itemId);

		// when
		String reportUrl = given()
			.contentType(JSON)
			.body(body)
			.when().post("/api/reports")
			.then()
			.statusCode(200)
			.extract().path("reportUrl");

		// then
		assertNotNull(reportUrl);
		assertTrue(reportUrl.contains("/Reports/show/"), "reportUrl should contain /Reports/show/");
	}

	@Test
	void pushNonCompliantReportStartsFlowAndReturnsReportUrl()
	{
		// given
		String body = """
			{"deviceId":"test-device","userId":"test-user","checkedAt":"%s",
			 "results":[{"itemId":%d,"passed":false,"output":"fail"}]}
			""".formatted(Instant.now(), itemId);

		// when
		String reportUrl = given()
			.contentType(JSON)
			.body(body)
			.when().post("/api/reports")
			.then()
			.statusCode(200)
			.extract().path("reportUrl");

		// then
		assertNotNull(reportUrl);
		assertTrue(reportUrl.contains("/Reports/show/"), "reportUrl should contain /Reports/show/");
	}
}
