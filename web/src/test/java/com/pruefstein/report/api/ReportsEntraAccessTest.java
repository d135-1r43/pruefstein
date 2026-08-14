package com.pruefstein.report.api;

import java.time.Instant;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.user.web.CurrentUserBean;
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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Entra hands out only the claims the scopes ask for, so a token can arrive
 * carrying nothing friendlier than the subject. That must not read as "this
 * user owns nothing in particular" — which is how a null owner filter behaves.
 */
@QuarkusTest
class ReportsEntraAccessTest
{
	private static final String SUBJECT = "PHpuC2008idxQO2Ie5EqlZ0gFVRQzSzxUAkagU24WcE";

	@Inject
	ReportRepository reportRepository;

	@Inject
	CurrentUserBean currentUser;

	@BeforeEach
	void setUp()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			Report someoneElse = new Report();
			someoneElse.setDeviceId("entra-access-other-device");
			someoneElse.setUserId("someone-else");
			someoneElse.setKeycloakUser("someone-else");
			someoneElse.setCheckedAt(Instant.now());
			someoneElse.setStatus(ReportStatus.COMPLIANT);
			reportRepository.persist(someoneElse);
		});
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew()
			.run(() -> reportRepository.delete("deviceId", "entra-access-other-device"));
	}

	@Test
	@TestSecurity(user = SUBJECT, roles = "user")
	@JwtSecurity(claims = { @Claim(key = "sub", value = SUBJECT) })
	void aTokenWithoutAUsernameClaimFallsBackToTheSubject()
	{
		assertEquals(SUBJECT, currentUser.getUsername(),
			"a missing preferred_username must not leave the username null");
	}

	@Test
	@TestSecurity(user = SUBJECT, roles = "user")
	@JwtSecurity(claims = { @Claim(key = "sub", value = SUBJECT) })
	void aUserWithoutAUsernameClaimSeesNobodyElsesReports()
	{
		given()
			.when().get("/Reports/index")
			.then()
			.statusCode(200)
			.body(not(containsString("entra-access-other-device")));
	}

	@Test
	@TestSecurity(user = "someone-else", roles = "user")
	@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "someone-else") })
	void theOwnerStillSeesTheirOwnReports()
	{
		given()
			.when().get("/Reports/index")
			.then()
			.statusCode(200)
			.body(containsString("entra-access-other-device"));
	}
}
