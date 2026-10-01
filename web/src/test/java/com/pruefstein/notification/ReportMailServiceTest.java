package com.pruefstein.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.osversion.domain.MacOsRelease;
import com.pruefstein.osversion.repository.MacOsReleaseRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.user.domain.AppUser;
import com.pruefstein.user.repository.UserRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ReportMailServiceTest
{
	private static final String ADDRESS = "mail-test@example.com";

	@Inject
	ReportMailService mailService;

	@Inject
	ReportRepository reportRepository;

	@Inject
	ComplianceResultRepository resultRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	UserRepository userRepository;

	@Inject
	MacOsReleaseRepository releaseRepository;

	@Inject
	MockMailbox mailbox;

	@BeforeEach
	void setUp()
	{
		mailbox.clear();
	}

	@Test
	void compliantReportMailSaysEverythingPassed()
	{
		// given
		long reportId = persistReport(ReportStatus.COMPLIANT, null, false);

		// when
		mailService.sendOutcomeMail(reportId);

		// then
		Mail mail = onlyMail();
		assertTrue(mail.getSubject().contains("is compliant"), mail.getSubject());
		assertTrue(mail.getHtml().contains("All checks passed."));
		assertFalse(mail.getHtml().contains("DEADLINE"));
	}

	@Test
	void openReportMailNamesTheFailureAndTheDeadline()
	{
		// given
		Instant deadline = Instant.now().plus(7, ChronoUnit.DAYS);
		long reportId = persistReport(ReportStatus.OPEN, deadline, true);

		// when
		mailService.sendOutcomeMail(reportId);

		// then
		Mail mail = onlyMail();
		assertTrue(mail.getSubject().contains("1 check failed"), mail.getSubject());
		assertTrue(mail.getHtml().contains("ACTION REQUIRED"));
		assertTrue(mail.getHtml().contains("DEADLINE"));
		assertTrue(mail.getHtml().contains("FileVault Enabled"));
		assertTrue(mail.getHtml().contains("Disk encryption not enabled"));
	}

	@Test
	void nonCompliantReportMailSaysTheDeadlinePassed()
	{
		// given
		long reportId = persistReport(ReportStatus.NON_COMPLIANT,
			Instant.now().minus(1, ChronoUnit.DAYS), true);

		// when
		mailService.sendOutcomeMail(reportId);

		// then
		Mail mail = onlyMail();
		assertTrue(mail.getSubject().contains("non-compliant"), mail.getSubject());
		assertTrue(mail.getHtml().contains("The deadline has passed."));
	}

	@Test
	void reminderMailIsUrgentAndCountsTheRemainingDays()
	{
		// given
		long reportId = persistReport(ReportStatus.OPEN, Instant.now().plus(47, ChronoUnit.HOURS), true);

		// when
		QuarkusTransaction.requiringNew()
			.run(() -> mailService.sendDeadlineReminder(reportRepository.findById(reportId)));

		// then
		Mail mail = onlyMail();
		assertTrue(mail.getSubject().contains("2 days left"), mail.getSubject());
		assertTrue(mail.getHtml().contains("URGENT"));
		assertTrue(mail.getHtml().contains("FileVault Enabled"));
	}

	/** A red macOS is listed first among what failed, and counted with it. */
	@Test
	void anOutdatedMacOsIsListedAsAFailure()
	{
		// given — 26.7 while 26.7.1 was the newest, and one failed check
		long reportId = persistReport(ReportStatus.OPEN, Instant.now().plus(7, ChronoUnit.DAYS), true);
		runningMacOs(reportId, "26.7", "26.7.1");

		// when
		mailService.sendOutcomeMail(reportId);

		// then
		Mail mail = onlyMail();
		assertTrue(mail.getSubject().contains("2 checks failed"), mail.getSubject());
		String html = mail.getHtml();
		assertTrue(html.contains("macOS up to date"));
		assertTrue(html.contains("macOS 26.7 is missing 26.7.1."));
		assertTrue(html.indexOf("macOS up to date") < html.indexOf("FileVault Enabled"),
			"the OS should lead the list");
		assertFalse(html.contains("HINT"));
	}

	/** An amber macOS earns a hint, not a failure — even on a clean report. */
	@Test
	void aFullyPatchedOlderMacOsGetsAHint()
	{
		// given — the newest fix of 26 while 27.0 is out
		QuarkusTransaction.requiringNew().run(() -> {
			MacOsRelease release = new MacOsRelease();
			release.setProductVersion("26.7.1");
			release.setBuild("25H17");
			release.setPostingDate(LocalDate.of(2026, 9, 1));
			release.setPublicRelease(true);
			release.setSeenAt(Instant.now());
			releaseRepository.persist(release);
		});
		long reportId = persistReport(ReportStatus.COMPLIANT, null, false);
		runningMacOs(reportId, "26.7.1", "27.0");

		try
		{
			// when
			mailService.sendOutcomeMail(reportId);

			// then
			Mail mail = onlyMail();
			assertTrue(mail.getSubject().contains("is compliant"), mail.getSubject());
			assertTrue(mail.getHtml().contains("HINT"));
			assertTrue(mail.getHtml().contains("macOS 26.7.1 is fully patched, but macOS 27.0 is out."));
			assertFalse(mail.getHtml().contains("WHAT FAILED"));
		}
		finally
		{
			QuarkusTransaction.requiringNew()
				.run(() -> releaseRepository.delete("productVersion = ?1", "26.7.1"));
		}
	}

	@Test
	void reportWithoutMailAddressIsSkipped()
	{
		// given
		Long[] ids = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			Report report = newReport(ReportStatus.COMPLIANT, null);
			reportRepository.persist(report);
			ids[0] = report.id;
		});

		// when
		mailService.sendOutcomeMail(ids[0]);

		// then
		assertEquals(0, mailbox.getTotalMessagesSent());
	}

	private Mail onlyMail()
	{
		List<Mail> sent = mailbox.getMailsSentTo(ADDRESS);
		assertEquals(1, sent.size(), "expected exactly one mail to " + ADDRESS);
		return sent.get(0);
	}

	/**
	 * Persists a report owned by a user with a mail address, optionally with
	 * one failed check attached.
	 */
	private long persistReport(ReportStatus status, Instant deadline, boolean withFailure)
	{
		Long[] ids = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			AppUser user = new AppUser();
			user.setOidcSubject("mail-test-subject-" + status + "-" + System.nanoTime());
			user.setFirstname("Alex");
			user.setLastname("Tester");
			user.setMail(ADDRESS);
			userRepository.persist(user);

			Report report = newReport(status, deadline);
			report.setAppUser(user);
			reportRepository.persist(report);
			ids[0] = report.id;

			if (withFailure)
			{
				ComplianceGroup group = new ComplianceGroup();
				group.setName("Disk");
				groupRepository.persist(group);

				ExpressionCheck item = new ExpressionCheck();
				item.setName("FileVault Enabled");
				item.setGroup(group);
				itemRepository.persist(item);

				ComplianceResult result = new ComplianceResult();
				result.setReport(report);
				result.setItem(item);
				result.setPassed(false);
				result.setAiShortDescription("Disk encryption not enabled");
				resultRepository.persist(result);
			}
		});
		return ids[0];
	}

	private void runningMacOs(long reportId, String version, String latest)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			Report report = reportRepository.findById(reportId);
			report.setOsName("macOS");
			report.setOsVersion(version);
			report.setOsLatestVersion(latest);
		});
	}

	private Report newReport(ReportStatus status, Instant deadline)
	{
		Report report = new Report();
		report.setDeviceId("mail-test-device");
		report.setUserId("mail-test-host");
		report.setKeycloakUser("mail-test-user");
		report.setCheckedAt(Instant.now());
		report.setStatus(status);
		report.setDeadline(deadline);
		return report;
	}
}
