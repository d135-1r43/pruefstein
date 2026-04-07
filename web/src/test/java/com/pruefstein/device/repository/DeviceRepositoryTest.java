package com.pruefstein.device.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.pruefstein.device.domain.Device;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class DeviceRepositoryTest
{
	@Inject
	DeviceRepository deviceRepository;

	@Test
	void findByDeviceIdReturnsDevice()
	{
		// given
		deviceRepository.persist(device("dev-1", Instant.now(), null));

		// when
		Optional<Device> found = deviceRepository.findByDeviceId("dev-1");

		// then
		assertTrue(found.isPresent());
		assertEquals("dev-1", found.get().getDeviceId());
	}

	@Test
	void findByDeviceIdReturnsEmptyForUnknownId()
	{
		// given (empty DB)

		// when
		Optional<Device> found = deviceRepository.findByDeviceId("unknown-device");

		// then
		assertFalse(found.isPresent());
	}

	@Test
	void findOverdueReturnsDeviceWithOldReportAndFlowInstanceId()
	{
		// given
		Instant oldReport = Instant.now().minusSeconds(3600);
		deviceRepository.persist(device("overdue-dev", oldReport, "flow-123"));

		// when
		List<Device> overdue = deviceRepository.findOverdue(Instant.now().minusSeconds(60));

		// then
		assertTrue(overdue.stream().anyMatch(d -> "overdue-dev".equals(d.getDeviceId())));
	}

	@Test
	void findOverdueExcludesDeviceWithNullFlowInstanceId()
	{
		// given
		Instant oldReport = Instant.now().minusSeconds(3600);
		deviceRepository.persist(device("no-flow-dev", oldReport, null));

		// when
		List<Device> overdue = deviceRepository.findOverdue(Instant.now().minusSeconds(60));

		// then
		assertFalse(overdue.stream().anyMatch(d -> "no-flow-dev".equals(d.getDeviceId())));
	}

	@Test
	void findOverdueExcludesDeviceWithRecentReport()
	{
		// given
		Instant recentReport = Instant.now();
		deviceRepository.persist(device("fresh-dev", recentReport, "flow-456"));

		// when
		List<Device> overdue = deviceRepository.findOverdue(Instant.now().minusSeconds(60));

		// then
		assertFalse(overdue.stream().anyMatch(d -> "fresh-dev".equals(d.getDeviceId())));
	}

	// ── helpers
	// ───────────────────────────────────────────────────────────────

	private Device device(String deviceId, Instant lastReportAt, String flowInstanceId)
	{
		Device d = new Device();
		d.setDeviceId(deviceId);
		d.setUserId("user");
		d.setLastReportAt(lastReportAt);
		d.setPeriodicFlowInstanceId(flowInstanceId);
		return d;
	}
}
