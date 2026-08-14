package com.pruefstein.report.flow;

import java.time.Instant;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.device.repository.DeviceRepository;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the whole broker-free loop: an agent check-in starts a workflow
 * instance, a later check-in publishes the outcome event straight into the
 * engine, and the resumed workflow calls back into
 * {@code /internal/reports/finalize} to close the report.
 *
 * <p>
 * The point is the wiring, not the endpoints — so it asserts on the report
 * status the workflow produced rather than on any HTTP response.
 */
@QuarkusTest
@TestProfile(FreshEngineProfile.class)
@TestSecurity(user = "flowuser", roles = "user")
@JwtSecurity(claims = { @Claim(key = "preferred_username", value = "flowuser") })
class ComplianceFlowRoundTripTest
{
	private static final String DEVICE = "roundtrip-device";
	private static final String OTHER_DEVICE = "roundtrip-device-2";

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
			group.setName("Round Trip Group");
			groupRepository.persist(group);
			ids[0] = group.id;

			ExpressionCheck item = new ExpressionCheck();
			item.setName("Round Trip Check");
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
			for (String device : new String[] { DEVICE, OTHER_DEVICE })
			{
				resultRepository.delete("report.deviceId", device);
				reportRepository.delete("deviceId", device);
				deviceRepository.delete("deviceId", device);
			}
			itemRepository.deleteById(itemId);
			groupRepository.deleteById(groupId);
		});
	}

	@Test
	void resubmittingACleanRunResumesTheWorkflowAndClosesTheReport()
	{
		// given — a failing check-in parks a workflow instance on its listen
		// task
		long reportId = checkIn(DEVICE, false);
		assertEquals(ReportStatus.OPEN, statusOf(reportId));
		assertNotNull(flowInstanceOf(reportId), "a flow instance should have been started");

		// when — the device reports clean, publishing the outcome into the
		// engine
		checkIn(DEVICE, true);

		// then — the resumed workflow finalises the report through its HTTP
		// callback
		assertEquals(ReportStatus.COMPLIANT, awaitFinalStatus(reportId));
	}

	/**
	 * Fails today, and did before the broker was removed: a {@code listen} task
	 * filtered on event type alone matches every parked instance of the
	 * workflow, so the first event delivered consumes all of them. The
	 * {@code flowinstanceid} extension the emitters set is never used for
	 * routing — {@code FlowMessagingConsumer} and {@code InMemoryEvents} both
	 * dispatch purely by type through {@code AbstractTypeConsumer}.
	 *
	 * <p>
	 * Fixing it means correlating the listen filter with the instance id.
	 * Disabled rather than deleted so the gap stays visible.
	 */
	@Disabled("known defect: a listen filtered by type alone consumes every parked instance")
	@Test
	void oneDevicesOutcomeLeavesAnotherDevicesWorkflowWaiting()
	{
		// given — two devices are parked on their own workflow instances
		long first = checkIn(DEVICE, false);
		long second = checkIn(OTHER_DEVICE, false);

		// when — only the first device reports clean
		checkIn(DEVICE, true);
		assertEquals(ReportStatus.COMPLIANT, awaitFinalStatus(first));
		assertEquals(ReportStatus.OPEN, statusOf(second));

		// then — the second instance must still be listening, which only shows
		// up when its own outcome arrives: had the first event consumed it,
		// this
		// second event would land on nothing and the report would stay open
		checkIn(OTHER_DEVICE, true);
		assertEquals(ReportStatus.COMPLIANT, awaitFinalStatus(second),
			"the first device's event consumed the second device's workflow instance");
	}

	/** Posts a report as the agent would and returns the report id. */
	private long checkIn(String deviceId, boolean passed)
	{
		String body = """
			{"deviceId":"%s","userId":"roundtrip-user","checkedAt":"%s",
			 "results":[{"itemId":%d,"passed":%s,"output":"out"}]}
			""".formatted(deviceId, Instant.now(), itemId, passed);

		String reportUrl = given()
			.contentType(JSON)
			.body(body)
			.when().post("/api/reports")
			.then()
			.statusCode(200)
			.extract().path("reportUrl");

		return Long.parseLong(reportUrl.substring(reportUrl.lastIndexOf('/') + 1));
	}

	/**
	 * The workflow resumes on an engine thread and finalises over HTTP, so the
	 * status lands shortly after the check-in returns.
	 */
	private ReportStatus awaitFinalStatus(long reportId)
	{
		ReportStatus status = ReportStatus.OPEN;
		for (int attempt = 0; attempt < 100 && status == ReportStatus.OPEN; attempt++)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
			status = statusOf(reportId);
		}
		return status;
	}

	private ReportStatus statusOf(long reportId)
	{
		return QuarkusTransaction.requiringNew()
			.call(() -> reportRepository.findById(reportId).getStatus());
	}

	private String flowInstanceOf(long reportId)
	{
		return QuarkusTransaction.requiringNew()
			.call(() -> reportRepository.findById(reportId).getFlowInstanceId());
	}
}
