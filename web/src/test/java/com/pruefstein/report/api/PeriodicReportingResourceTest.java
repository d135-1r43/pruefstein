package com.pruefstein.report.api;

import java.time.Instant;

import com.pruefstein.device.domain.Device;
import com.pruefstein.device.repository.DeviceRepository;
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
class PeriodicReportingResourceTest
{
	@Inject
	DeviceRepository deviceRepository;

	@Inject
	ReportRepository reportRepository;

	@BeforeEach
	void setUp()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			Device device = new Device();
			device.setDeviceId("cycle-test-device");
			device.setUserId("cycle-test-user");
			device.setKeycloakUser("cycle-test-user");
			device.setLastReportAt(Instant.now().minusSeconds(3600));
			deviceRepository.persist(device);
		});
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			reportRepository.delete("deviceId", "cycle-test-device");
			deviceRepository.delete("deviceId", "cycle-test-device");
		});
	}

	@Test
	void cycleWithReportedFalseCreatesMissingReport()
	{
		// given
		String body = """
			{"deviceId":"cycle-test-device","reported":false}
			""";

		// when
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/internal/reporting/cycle")
			.then()
			.statusCode(200);

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			long count = reportRepository.count("deviceId = ?1 and status = ?2",
				"cycle-test-device", ReportStatus.MISSING);
			assertEquals(1L, count);
		});
	}

	@Test
	void cycleWithReportedFalseResetsLastReportAt()
	{
		// given
		Instant before = Instant.now().minusSeconds(3600);
		String body = """
			{"deviceId":"cycle-test-device","reported":false}
			""";

		// when
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/internal/reporting/cycle")
			.then()
			.statusCode(200);

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			Device device = deviceRepository.findByDeviceId("cycle-test-device").orElseThrow();
			assertTrue(device.getLastReportAt().isAfter(before));
		});
	}

	@Test
	void cycleWithReportedTrueDoesNotCreateReport()
	{
		// given
		String body = """
			{"deviceId":"cycle-test-device","reported":true}
			""";

		// when
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/internal/reporting/cycle")
			.then()
			.statusCode(200);

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			long count = reportRepository.count("deviceId", "cycle-test-device");
			assertEquals(0L, count);
		});
	}

	@Test
	void cycleReturns404ForUnknownDevice()
	{
		// given
		String body = """
			{"deviceId":"unknown-device","reported":false}
			""";

		// when / then
		given()
			.contentType(JSON)
			.body(body)
			.when().post("/internal/reporting/cycle")
			.then()
			.statusCode(404);
	}
}
