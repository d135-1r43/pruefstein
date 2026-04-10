package com.pruefstein.report.api;

import java.time.Instant;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

@QuarkusTest
class ReportsAccessTest
{
	@Inject
	ReportRepository reportRepository;

	private Long aliceReportId;
	private Long bobReportId;

	@BeforeEach
	void setUp()
	{
		Long[] ids = new Long[2];
		QuarkusTransaction.requiringNew().run(() -> {
			Report aliceReport = new Report();
			aliceReport.setDeviceId("access-test-alice-device");
			aliceReport.setUserId("alice");
			aliceReport.setKeycloakUser("alice");
			aliceReport.setCheckedAt(Instant.now());
			aliceReport.setStatus(ReportStatus.COMPLIANT);
			reportRepository.persist(aliceReport);
			ids[0] = aliceReport.id;

			Report bobReport = new Report();
			bobReport.setDeviceId("access-test-bob-device");
			bobReport.setUserId("bob");
			bobReport.setKeycloakUser("bob");
			bobReport.setCheckedAt(Instant.now());
			bobReport.setStatus(ReportStatus.COMPLIANT);
			reportRepository.persist(bobReport);
			ids[1] = bobReport.id;
		});
		aliceReportId = ids[0];
		bobReportId = ids[1];
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			reportRepository.deleteById(aliceReportId);
			reportRepository.deleteById(bobReportId);
		});
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "alice") })
	void nonAdminSeesOnlyOwnReportsInIndex()
	{
		// given (alice's report and bob's report seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/index")
			.then()
			.statusCode(200)
			.body(containsString("access-test-alice-device"))
			.body(not(containsString("access-test-bob-device")));
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "alice") })
	void nonAdminIsAllowedToSeeOwnReport()
	{
		// given (alice's report seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/show/" + aliceReportId)
			.then()
			.statusCode(200)
			.body(containsString("access-test-alice-device"));
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "alice") })
	void nonAdminIsForbiddenFromOtherUsersReport()
	{
		// given (bob's report seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/show/" + bobReportId)
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "admin") })
	void adminSeesAllReportsInIndex()
	{
		// given (both alice's and bob's reports seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/index")
			.then()
			.statusCode(200)
			.body(containsString("access-test-alice-device"))
			.body(containsString("access-test-bob-device"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "admin") })
	void adminCanSeeAnyReport()
	{
		// given (bob's report seeded in setUp)

		// when / then
		given()
			.when().get("/Reports/show/" + bobReportId)
			.then()
			.statusCode(200)
			.body(containsString("access-test-bob-device"));
	}
}
