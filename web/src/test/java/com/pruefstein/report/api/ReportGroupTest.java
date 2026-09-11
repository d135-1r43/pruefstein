package com.pruefstein.report.api;

import java.time.Instant;
import java.util.List;

import com.pruefstein.report.domain.Report;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGroupTest
{
	private static final Instant T0 = Instant.parse("2026-09-01T10:00:00Z");

	private static Report report(long id, String user, Instant checkedAt)
	{
		Report report = new Report();
		report.id = id;
		report.setKeycloakUser(user);
		report.setDeviceId("device-" + id);
		report.setCheckedAt(checkedAt);
		return report;
	}

	@Test
	void aSingleReportPerUserStaysASingleRow()
	{
		// given — two users, one report each
		Report anna = report(1, "anna", T0);
		Report bert = report(2, "bert", T0.minusSeconds(60));

		// when
		List<ReportGroup> groups = ReportGroup.group(List.of(anna, bert));

		// then — nothing is folded away, and nothing grows a toggle
		assertEquals(2, groups.size());
		assertSame(anna, groups.get(0).latest());
		assertSame(bert, groups.get(1).latest());
		assertFalse(groups.get(0).hasOlder());
		assertFalse(groups.get(1).hasOlder());
	}

	@Test
	void repeatRunsOfOneUserCollapseOntoTheLatest()
	{
		// given — three runs from anna, the newest in the middle of the list
		Report old = report(1, "anna", T0.minusSeconds(7200));
		Report newest = report(2, "anna", T0);
		Report middle = report(3, "anna", T0.minusSeconds(3600));

		// when
		List<ReportGroup> groups = ReportGroup.group(List.of(old, newest, middle));

		// then — one row, showing the latest run, with the rest behind it
		assertEquals(1, groups.size());
		ReportGroup group = groups.get(0);
		assertSame(newest, group.latest());
		assertTrue(group.hasOlder());
		assertEquals(2, group.getOlderCount());
	}

	@Test
	void theHiddenRunsAreOrderedNewestFirst()
	{
		// given
		Report oldest = report(1, "anna", T0.minusSeconds(7200));
		Report newest = report(2, "anna", T0);
		Report middle = report(3, "anna", T0.minusSeconds(3600));

		// when
		ReportGroup group = ReportGroup.group(List.of(oldest, newest, middle)).get(0);

		// then — expanding a group reads like the table itself does
		assertEquals(List.of(middle, oldest), group.older());
	}

	@Test
	void theOrderOfTheVisibleRowsIsTheOrderTheyArrivedIn()
	{
		// given — the repository has already sorted; grouping must not resort.
		// bert's only report sits between anna's two, so anna's group takes
		// the position of the row that stays on screen — its latest.
		Report annaOld = report(1, "anna", T0.minusSeconds(7200));
		Report bert = report(2, "bert", T0.minusSeconds(3600));
		Report annaNew = report(3, "anna", T0);

		// when
		List<ReportGroup> groups = ReportGroup.group(List.of(annaOld, bert, annaNew));

		// then — bert first, because anna's visible row is the last of the
		// three
		assertEquals(2, groups.size());
		assertSame(bert, groups.get(0).latest());
		assertSame(annaNew, groups.get(1).latest());
	}

	@Test
	void reportsWithoutAUserAreNeverGroupedTogether()
	{
		// given — two runs no user could be resolved for. They are not "one
		// user's repeats", they are two unattributed runs.
		Report first = report(1, null, T0);
		Report second = report(2, null, T0.minusSeconds(60));

		// when
		List<ReportGroup> groups = ReportGroup.group(List.of(first, second));

		// then
		assertEquals(2, groups.size());
		assertFalse(groups.get(0).hasOlder());
		assertFalse(groups.get(1).hasOlder());
	}

	@Test
	void aTieOnTheTimestampIsBrokenByTheNewerRow()
	{
		// given — two runs stamped the same second, as a re-submission can be
		Report earlier = report(1, "anna", T0);
		Report later = report(2, "anna", T0);

		// when
		ReportGroup group = ReportGroup.group(List.of(earlier, later)).get(0);

		// then — the higher id wins, so the choice is at least deterministic
		assertSame(later, group.latest());
		assertEquals(List.of(earlier), group.older());
	}

	@Test
	void aMissingTimestampNeverOutranksARealOne()
	{
		// given
		Report undated = report(1, "anna", null);
		Report dated = report(2, "anna", T0.minusSeconds(7200));

		// when
		ReportGroup group = ReportGroup.group(List.of(undated, dated)).get(0);

		// then
		assertSame(dated, group.latest());
	}

	@Test
	void anEmptyListGroupsToNothing()
	{
		// when / then — the template's empty state still has to trigger
		assertTrue(ReportGroup.group(List.of()).isEmpty());
	}
}
