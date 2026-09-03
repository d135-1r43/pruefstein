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

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

	@Inject
	ResultEnrichmentJob enrichmentJob;

	private Long itemId;
	private Long groupId;

	@BeforeEach
	void setUp()
	{
		when(aiService.explain(any(), any(), any(), any()))
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

	/**
	 * The push itself must not wait for the model: the agent gets a 200 as soon
	 * as the results are stored, with the explanation still missing.
	 */
	@Test
	void pushingAFailedResultDoesNotCallTheModel()
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
		verify(aiService, never()).explain(any(), any(), any(), any());
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceResult failed = failedResult();
			assertNull(failed.getAiShortDescription(), "the request path should not have explained anything");
		});
	}

	@Test
	void theJobExplainsAFailedResult()
	{
		// given — a stored report the request path left unexplained
		push(false);

		// when
		enrichmentJob.explainFailedChecks();

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceResult failed = failedResult();
			assertEquals("Disk encryption not enabled", failed.getAiShortDescription());
			assertNotNull(failed.getAiLongExplanation());
			assertTrue(failed.getAiLongExplanation().contains("FileVault"));
		});
	}

	/**
	 * A result the model could not explain — it was unreachable, or it rejected
	 * the output — stays in the queue rather than staying blank for good.
	 */
	@Test
	void aResultTheModelRefusedIsRetriedOnTheNextPass()
	{
		// given
		push(false);
		// doThrow, not when(...).thenThrow: re-stubbing through when() calls
		// the
		// mock, which would throw during the stubbing itself.
		doThrow(new RuntimeException("model unreachable"))
			.when(aiService).explain(any(), any(), any(), any());

		// when — the failing pass must not propagate
		enrichmentJob.explainFailedChecks();

		// then — this device's result is still unexplained, and a later pass
		// picks it up. Scoped to the device on purpose: findPending reads the
		// whole table, so counting it would depend on every other test's rows.
		QuarkusTransaction.requiringNew().run(
			() -> assertNull(failedResult().getAiShortDescription(), "a refused result stays pending"));

		doReturn(new ComplianceResultExplanation("Disk encryption not enabled", "FileVault is disabled."))
			.when(aiService).explain(any(), any(), any(), any());
		enrichmentJob.explainFailedChecks();

		QuarkusTransaction.requiringNew().run(
			() -> assertEquals("Disk encryption not enabled", failedResult().getAiShortDescription(),
				"the retry should have explained it"));
	}

	@Test
	void passedResultIsNeverExplained()
	{
		// given
		push(true);

		// when
		enrichmentJob.explainFailedChecks();

		// then — its own row is untouched. Not verify(never()) on the mock: the
		// job reads every pending row in the database, so another test's
		// leftovers would make that assertion about them rather than this one.
		QuarkusTransaction.requiringNew().run(() -> {
			List<ComplianceResult> results = resultRepository.list("report.deviceId", "ai-test-device");
			assertFalse(results.isEmpty());
			assertNull(results.getFirst().getAiShortDescription());
			assertNull(results.getFirst().getAiLongExplanation());
		});
	}

	private void push(boolean passed)
	{
		String body = """
			{"deviceId":"ai-test-device","userId":"ai-test-user","checkedAt":"%s",
			 "results":[{"itemId":%d,"passed":%b,"output":"[{\\"encrypted\\":\\"0\\"}]"}]}
			""".formatted(Instant.now(), itemId, passed);

		given()
			.contentType(JSON)
			.body(body)
			.when().post("/api/reports")
			.then()
			.statusCode(200);
	}

	private ComplianceResult failedResult()
	{
		return resultRepository.list("report.deviceId", "ai-test-device").stream()
			.filter(result -> !result.isPassed())
			.findFirst()
			.orElseThrow();
	}
}
