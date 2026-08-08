package com.pruefstein.report.api;

import java.time.Instant;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ReportFinalizeResourceTest
{
	@Inject
	ReportRepository reportRepository;

	private Long reportId;

	@BeforeEach
	void setUp()
	{
		Long[] ids = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			Report report = new Report();
			report.setDeviceId("finalize-test-device");
			report.setUserId("finalize-test-user");
			report.setCheckedAt(Instant.now());
			report.setStatus(ReportStatus.OPEN);
			reportRepository.persist(report);
			ids[0] = report.id;
		});
		reportId = ids[0];
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> reportRepository.deleteById(reportId));
	}

	@Test
	void finalizeSetsCompliantWhenAllPassed()
	{
		// given
		String body = """
			{"reportId":%d,"allPassed":true}
			""".formatted(reportId);

		// when
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/internal/reports/finalize")
			.then()
			.statusCode(200);

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			Report report = reportRepository.findById(reportId);
			assertEquals(ReportStatus.COMPLIANT, report.getStatus());
			assertNotNull(report.getFinalizedAt());
		});
	}

	@Test
	void finalizeSetsNonCompliantWhenNotAllPassed()
	{
		// given
		String body = """
			{"reportId":%d,"allPassed":false}
			""".formatted(reportId);

		// when
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/internal/reports/finalize")
			.then()
			.statusCode(200);

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			Report report = reportRepository.findById(reportId);
			assertEquals(ReportStatus.NON_COMPLIANT, report.getStatus());
			assertNotNull(report.getFinalizedAt());
		});
	}

	@Test
	void finalizeReturns404ForUnknownReport()
	{
		// given
		String body = """
			{"reportId":%d,"allPassed":true}
			""".formatted(Long.MAX_VALUE);

		// when / then
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/internal/reports/finalize")
			.then()
			.statusCode(404);
	}
}
