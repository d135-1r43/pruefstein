package com.pruefstein.report.repository;

import java.time.Instant;
import java.util.List;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class ReportRepositoryFilterTest
{
	@Inject
	ReportRepository reportRepository;

	@Test
	void listFilteredWithNoFiltersReturnsAll()
	{
		// given
		reportRepository.persist(report("filter-dev-a", "user-a", ReportStatus.COMPLIANT));
		reportRepository.persist(report("filter-dev-b", "user-b", ReportStatus.NON_COMPLIANT));

		// when
		List<Report> results = reportRepository.listFiltered(null, null, null, null);

		// then
		assertTrue(results.stream().anyMatch(r -> "filter-dev-a".equals(r.getDeviceId())));
		assertTrue(results.stream().anyMatch(r -> "filter-dev-b".equals(r.getDeviceId())));
	}

	@Test
	void listFilteredByStatusReturnsOnlyMatchingStatus()
	{
		// given
		reportRepository.persist(report("filter-compliant-dev", "user", ReportStatus.COMPLIANT));
		reportRepository.persist(report("filter-missing-dev", "user", ReportStatus.MISSING));

		// when
		List<Report> results = reportRepository.listFiltered(ReportStatus.COMPLIANT, null, null, null);

		// then
		assertTrue(results.stream().anyMatch(r -> "filter-compliant-dev".equals(r.getDeviceId())));
		assertFalse(results.stream().anyMatch(r -> "filter-missing-dev".equals(r.getDeviceId())));
	}

	@Test
	void listFilteredBySearchMatchesDeviceId()
	{
		// given
		reportRepository.persist(report("xray-device-42", "user", ReportStatus.OPEN));
		reportRepository.persist(report("unrelated-device", "user", ReportStatus.OPEN));

		// when
		List<Report> results = reportRepository.listFiltered(null, "xray", null, null);

		// then
		assertTrue(results.stream().anyMatch(r -> "xray-device-42".equals(r.getDeviceId())));
		assertFalse(results.stream().anyMatch(r -> "unrelated-device".equals(r.getDeviceId())));
	}

	@Test
	void listFilteredBySearchMatchesUserId()
	{
		// given
		reportRepository.persist(report("search-dev", "searchable-user", ReportStatus.OPEN));
		reportRepository.persist(report("other-dev", "different-user", ReportStatus.OPEN));

		// when
		List<Report> results = reportRepository.listFiltered(null, "searchable", null, null);

		// then
		assertTrue(results.stream().anyMatch(r -> "search-dev".equals(r.getDeviceId())));
		assertFalse(results.stream().anyMatch(r -> "other-dev".equals(r.getDeviceId())));
	}

	@Test
	void listFilteredSortsByCheckedAtAscending()
	{
		// given
		Report older = report("sort-old-dev", "user", ReportStatus.OPEN);
		older.setCheckedAt(Instant.now().minusSeconds(3600));
		Report newer = report("sort-new-dev", "user", ReportStatus.OPEN);
		newer.setCheckedAt(Instant.now());
		reportRepository.persist(older);
		reportRepository.persist(newer);

		// when
		List<Report> results = reportRepository.listFiltered(null, "sort-", null, "asc");

		// then
		int oldIdx = -1, newIdx = -1;
		for (int i = 0; i < results.size(); i++)
		{
			if ("sort-old-dev".equals(results.get(i).getDeviceId())) oldIdx = i;
			if ("sort-new-dev".equals(results.get(i).getDeviceId())) newIdx = i;
		}
		assertTrue(oldIdx >= 0 && newIdx >= 0, "Both reports should appear in results");
		assertTrue(oldIdx < newIdx, "Older report should appear before newer in ascending sort");
	}

	// ── helpers ──────────────────────────────────────────────────────────

	private Report report(String deviceId, String userId, ReportStatus status)
	{
		Report report = new Report();
		report.setDeviceId(deviceId);
		report.setUserId(userId);
		report.setCheckedAt(Instant.now());
		report.setStatus(status);
		return report;
	}
}
