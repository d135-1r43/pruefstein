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
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class ReportsTest
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
			report.setDeviceId("reports-test-device");
			report.setUserId("reports-test-user");
			report.setCheckedAt(Instant.now());
			report.setStatus(ReportStatus.COMPLIANT);
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
	void indexReturns200()
	{
		// given (report seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/index")
			.then()
			.statusCode(200)
			.contentType(containsString("text/html"));
	}

	@Test
	void indexContainsReport()
	{
		// given (report seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/index")
			.then()
			.statusCode(200)
			.body(containsString("reports-test-device"));
	}

	@Test
	void showReturns200ForExistingReport()
	{
		// given (report seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/show/" + reportId)
			.then()
			.statusCode(200)
			.contentType(containsString("text/html"))
			.body(containsString("reports-test-device"));
	}

	@Test
	void showDisplaysCompliantStatus()
	{
		// given (report seeded in setUp with COMPLIANT status)

		// when / then
		given()
			.when().get("/Reports/show/" + reportId)
			.then()
			.statusCode(200)
			.body(containsString("COMPLIANT"));
	}

	@Test
	void showReturns404ForUnknownReport()
	{
		// given
		long unknownId = Long.MAX_VALUE;

		// when / then
		given()
			.when().get("/Reports/show/" + unknownId)
			.then()
			.statusCode(404);
	}
}
