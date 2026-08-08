package com.pruefstein.compliance.service;

import java.time.Instant;
import java.util.List;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.device.repository.DeviceRepository;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@TestSecurity(user = "testuser", roles = "user")
@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "testuser") })
class ComplianceResultAiEnrichmentTest
{
	@InjectMock
	ComplianceResultAiService aiService;

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
		Mockito.when(aiService.explain(any(), any(), any(), any()))
			.thenReturn(new ComplianceResultExplanation(
				"Disk encryption not enabled",
				"FileVault is disabled on this device. Enable it via System Settings → Privacy & Security → FileVault → Turn On."));

		Long[] ids = new Long[2];
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceGroup group = new ComplianceGroup();
			group.setName("AI Test Group");
			groupRepository.persist(group);
			ids[0] = group.id;

			ExpressionCheck item = new ExpressionCheck();
			item.setName("FileVault Enabled");
			item.setQuery("SELECT encrypted FROM disk_encryption WHERE name = 'disk0s2';");
			item.setExpectedExpression("results.size() > 0 && results[0].encrypted == '1'");
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
			resultRepository.delete("report.deviceId", "ai-test-device");
			reportRepository.delete("deviceId", "ai-test-device");
			deviceRepository.delete("deviceId", "ai-test-device");
			itemRepository.deleteById(itemId);
			groupRepository.deleteById(groupId);
		});
	}

	@Test
	void failedResultIsEnrichedWithShortDescriptionAndLongExplanation()
	{
		// given
		String body = """
			{"deviceId":"ai-test-device","userId":"ai-test-user","checkedAt":"%s",
			 "results":[{"itemId":%d,"passed":false,"output":"[{\\"encrypted\\":\\"0\\"}]"}]}
			""".formatted(Instant.now(), itemId);

		// when
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/api/reports")
			.then()
			.statusCode(200);

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			List<ComplianceResult> results = resultRepository.list("report.deviceId", "ai-test-device");
			assertFalse(results.isEmpty());
			ComplianceResult failed = results.stream()
				.filter(r -> !r.isPassed())
				.findFirst()
				.orElseThrow();
			assertEquals("Disk encryption not enabled", failed.getAiShortDescription());
			assertNotNull(failed.getAiLongExplanation());
			assertTrue(failed.getAiLongExplanation().contains("FileVault"));
		});
	}

	@Test
	void passedResultIsNotEnrichedByAi()
	{
		// given
		String body = """
			{"deviceId":"ai-test-device","userId":"ai-test-user","checkedAt":"%s",
			 "results":[{"itemId":%d,"passed":true,"output":"[{\\"encrypted\\":\\"1\\"}]"}]}
			""".formatted(Instant.now(), itemId);

		// when
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/api/reports")
			.then()
			.statusCode(200);

		// then — AI was never called and fields are null
		Mockito.verify(aiService, Mockito.never()).explain(any(), any(), any(), any());
		QuarkusTransaction.requiringNew().run(() -> {
			List<ComplianceResult> results = resultRepository.list("report.deviceId", "ai-test-device");
			assertFalse(results.isEmpty());
			assertNull(results.getFirst().getAiShortDescription());
			assertNull(results.getFirst().getAiLongExplanation());
		});
	}
}
