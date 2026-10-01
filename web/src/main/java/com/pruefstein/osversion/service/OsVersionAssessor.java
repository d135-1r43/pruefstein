package com.pruefstein.osversion.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;

import com.pruefstein.osversion.domain.MacOsVersion;
import com.pruefstein.osversion.domain.OsVersionStanding;
import com.pruefstein.report.domain.Report;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Decides what a report says about its device's operating system.
 * <p>
 * The comparison is against the newest release that existed <em>when the report
 * was filed</em>, stamped onto the report at the time. A report is a statement
 * about a machine on a day, and re-judging it every time Apple ships something
 * would turn a clean report red months later without anything having happened.
 * Reports filed before this was recorded fall back to today's newest release,
 * which is the best that can be said about them.
 */
@ApplicationScoped
public class OsVersionAssessor
{
	@Inject
	MacOsReleaseCatalog catalog;

	public OsVersionAssessment assess(Report report)
	{
		if (report == null)
		{
			return OsVersionAssessment.unknown();
		}
		return assess(report.getOsName(), report.getOsVersion(), report.getOsBuild(), latestFor(report),
			filedOn(report));
	}

	/**
	 * Judges a version no report holds yet, against what Apple has published
	 * now — what a report filed at this moment would say. The agent asks this
	 * before anyone decides to report, and the upload asks it again to decide
	 * whether the run passed.
	 */
	public OsVersionAssessment assessNow(String name, String version, String build)
	{
		return assess(name, version, build, catalog.latestPublicVersion().orElse(null),
			LocalDate.now(ZoneOffset.UTC));
	}

	private OsVersionAssessment assess(String name, String version, String build, MacOsVersion latest,
		LocalDate filedOn)
	{
		Optional<MacOsVersion> reported = MacOsVersion.parse(version);
		if (reported.isEmpty())
		{
			return OsVersionAssessment.unknown();
		}
		if (latest == null)
		{
			// The version is worth showing even with nothing to measure it
			// against; it simply gets no colour.
			return new OsVersionAssessment(name, version, build, null, null, OsVersionStanding.CURRENT, 0);
		}

		MacOsVersion latestOfTrain = catalog.latestPublicPerTrain(filedOn).get(reported.get().major());
		OsVersionStanding standing = reported.get().standingAgainst(latest, latestOfTrain);
		int years = 0;
		if (standing == OsVersionStanding.OLDER_TRAIN_UNPATCHED || standing == OsVersionStanding.UNSUPPORTED_TRAIN)
		{
			OptionalInt age = reported.get().trainAgeInYears(LocalDate.now(ZoneOffset.UTC));
			years = age.orElse(0);
		}
		return new OsVersionAssessment(name, version, build, latest.toString(),
			latestOfTrain != null ? latestOfTrain.toString() : null, standing, years);
	}

	/**
	 * The day the report was filed, which bounds what counts as the newest fix
	 * of the device's own train: a fix Apple shipped afterwards was not missing
	 * then.
	 */
	private static LocalDate filedOn(Report report)
	{
		return report.getCheckedAt() != null
			? LocalDate.ofInstant(report.getCheckedAt(), ZoneOffset.UTC)
			: LocalDate.now(ZoneOffset.UTC);
	}

	private MacOsVersion latestFor(Report report)
	{
		if (report.getOsVersion() == null)
		{
			return null;
		}
		return MacOsVersion.parse(report.getOsLatestVersion())
			.or(() -> catalog.latestPublicVersion())
			.orElse(null);
	}
}
