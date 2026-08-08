package com.pruefstein.shared.bootstrap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.pruefstein.compliance.domain.AppBlacklistCheck;
import com.pruefstein.compliance.domain.AppMatcher;
import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.domain.MatcherType;
import com.pruefstein.compliance.repository.BlockedAppRepository;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.compliance.service.CheckResolver;
import com.pruefstein.compliance.service.CheckResolver.ResolvedCheck;
import com.pruefstein.compliance.service.ComplianceResultAiService;
import com.pruefstein.compliance.service.ComplianceResultExplanation;
import com.pruefstein.device.domain.Device;
import com.pruefstein.device.repository.DeviceRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Startup
{
	private static final Logger LOG = LoggerFactory.getLogger(Startup.class);

	/**
	 * Offline fallbacks used when the AI service is unreachable (e.g. no
	 * {@code OPENAI_API_KEY} in the dev environment), so the seeded UI still
	 * demonstrates what a generated tip looks like.
	 */
	private static final ComplianceResultExplanation FILEVAULT_FALLBACK = new ComplianceResultExplanation(
		"FileVault encryption not enabled",
		"The osquery result returned no rows, which means FileVault is off on this device. "
			+ "Without disk encryption, data on the drive is readable if the device is lost or stolen.\n\n"
			+ "To fix: System Settings → Privacy & Security → FileVault → Turn On FileVault. "
			+ "You will need to restart the device and save the recovery key in a secure location.");

	private static final ComplianceResultExplanation AUTO_UPDATES_FALLBACK = new ComplianceResultExplanation(
		"Automatic software updates disabled",
		"The AutomaticCheckEnabled preference is set to 0, meaning macOS will not check for or install updates automatically. "
			+ "Missing security patches leaves the device exposed to known vulnerabilities.\n\n"
			+ "To fix: System Settings → General → Software Update → Automatic Updates → enable all options. "
			+ "Alternatively, run: sudo defaults write /Library/Preferences/com.apple.SoftwareUpdate AutomaticCheckEnabled -bool true");

	@Inject
	ComplianceResultAiService aiService;

	@Inject
	CheckResolver checkResolver;

	@Inject
	BlockedAppRepository blockedAppRepository;

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

	@Transactional
	public void start(@Observes StartupEvent evt)
	{
		if (LaunchMode.current() == LaunchMode.DEVELOPMENT)
		{
			seedCompliance();
		}
	}

	private void seedCompliance()
	{
		ComplianceGroup cryptography = new ComplianceGroup();
		cryptography.setName("A.10 Cryptography");
		groupRepository.persist(cryptography);

		ComplianceItem fileVault = addItem(cryptography,
			"FileVault enabled",
			"SELECT filevault_status FROM disk_encryption WHERE filevault_status = 'on' LIMIT 1;",
			"results.size() > 0");

		ComplianceGroup operations = new ComplianceGroup();
		operations.setName("A.12 Operations Security");
		groupRepository.persist(operations);

		ComplianceItem firewall = addItem(operations,
			"Firewall enabled",
			"SELECT global_state FROM alf;",
			"results.size() > 0 && results[0].global_state == '1'");

		ComplianceItem autoUpdates = addItem(operations,
			"Automatic updates enabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticCheckEnabled';",
			"results.size() > 0 && results[0].value == '1'");

		// A.12.6 — technical vulnerability management: checking for updates is
		// not enough, they have to be installed as well
		addItem(operations,
			"Critical security updates installed automatically",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'CriticalUpdateInstall';",
			"results.size() > 0 && results[0].value == '1'");

		addItem(operations,
			"macOS updates installed automatically",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticallyInstallMacOSUpdates';",
			"results.size() > 0 && results[0].value == '1'");

		// A.12.2 — protection against malware
		addItem(operations,
			"Gatekeeper enabled",
			"SELECT assessments_enabled FROM gatekeeper;",
			"results.size() > 0 && results[0].assessments_enabled == '1'");

		addItem(operations,
			"System Integrity Protection enabled",
			"SELECT enabled FROM sip_config WHERE config_flag = 'sip';",
			"results.size() > 0 && results[0].enabled == '1'");

		// A.12.3 — backup
		addItem(operations,
			"Time Machine backup destination configured",
			"SELECT destination_id FROM time_machine_destinations;",
			"results.size() > 0");

		// A.12.4 — logging and monitoring
		addItem(operations,
			"Firewall logging enabled",
			"SELECT logging_enabled FROM alf;",
			"results.size() > 0 && results[0].logging_enabled == '1'");

		ComplianceGroup access = new ComplianceGroup();
		access.setName("A.9 Access Control");
		groupRepository.persist(access);

		ComplianceItem screenLock = addItem(access,
			"Screen lock timeout ≤ 300 seconds",
			"SELECT value FROM preferences WHERE domain = 'com.apple.screensaver' AND key = 'idleTime';",
			"results.size() > 0 && results[0].value <= 300");

		// A.9.4.2 — secure log-on. A screensaver that blanks the display
		// without
		// asking for a password protects nothing.
		addItem(access,
			"Screen lock requires a password",
			"SELECT enabled, grace_period FROM screenlock;",
			"results.size() > 0 && results[0].enabled == '1' && results[0].grace_period <= 300");

		// Absence of the key means no auto-login user is configured
		addItem(access,
			"Automatic login disabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.loginwindow' AND key = 'autoLoginUser';",
			"results.size() == 0");

		addItem(access,
			"Guest account disabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.loginwindow' AND key = 'GuestEnabled';",
			"results.size() == 0 || results[0].value == '0'");

		ComplianceGroup communications = new ComplianceGroup();
		communications.setName("A.13 Communications Security");
		groupRepository.persist(communications);

		// A.13.1 — network security management: every inbound sharing service
		// is
		// attack surface on an endpoint that does not need to serve anything
		addItem(communications,
			"Remote login (SSH) disabled",
			"SELECT remote_login FROM sharing_preferences;",
			"results.size() > 0 && results[0].remote_login == '0'");

		addItem(communications,
			"Screen sharing disabled",
			"SELECT screen_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].screen_sharing == '0'");

		addItem(communications,
			"File sharing disabled",
			"SELECT file_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].file_sharing == '0'");

		addItem(communications,
			"Internet sharing disabled",
			"SELECT internet_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].internet_sharing == '0'");

		addItem(communications,
			"Firewall stealth mode enabled",
			"SELECT stealth_enabled FROM alf;",
			"results.size() > 0 && results[0].stealth_enabled == '1'");

		seedBlockedApps();

		seedReports(fileVault, firewall, autoUpdates, screenLock);
	}

	/**
	 * A.12.6.2 — restrictions on software installation. The check's SQL is
	 * generated from these rules on every agent run, so it is left null here.
	 */
	private void seedBlockedApps()
	{
		// Deliberately group-less: this check lives on the Blocked Apps screen
		// and in its own report section, not under Groups & Items.
		AppBlacklistCheck check = new AppBlacklistCheck();
		check.setName("No blacklisted applications installed");
		itemRepository.persist(check);

		// Nextcloud is the reason the rule owns several matchers: it ships as a
		// Homebrew cask and as a plain bundle, and one entry has to catch both.
		addBlockedApp("Nextcloud Desktop",
			"Company data must stay in the approved M365 tenant. Third-party sync clients move it outside the ISMS scope.",
			List.of(
				new AppMatcher(MatcherType.BUNDLE_ID, "com.nextcloud.%"),
				new AppMatcher(MatcherType.HOMEBREW, "nextcloud"),
				new AppMatcher(MatcherType.APP_NAME, "Nextcloud.app")));

		addBlockedApp("TeamViewer",
			"Unmanaged remote access bypasses the approved support channel and its logging.",
			List.of(
				new AppMatcher(MatcherType.BUNDLE_ID, "com.teamviewer.%"),
				new AppMatcher(MatcherType.HOMEBREW, "teamviewer")));

		addBlockedApp("BitTorrent clients",
			"Peer-to-peer file sharing risks unlicensed content and inbound connections on company devices.",
			List.of(
				new AppMatcher(MatcherType.HOMEBREW, "transmission"),
				new AppMatcher(MatcherType.HOMEBREW, "qbittorrent"),
				new AppMatcher(MatcherType.BUNDLE_ID, "org.m0k.transmission")));
	}

	private void addBlockedApp(String label, String reason, List<AppMatcher> matchers)
	{
		BlockedApp app = new BlockedApp();
		app.setLabel(label);
		app.setReason(reason);
		app.setEnabled(true);
		app.setMatchers(new ArrayList<>(matchers));
		blockedAppRepository.persist(app);
	}

	private void seedReports(ComplianceItem fileVault, ComplianceItem firewall,
		ComplianceItem autoUpdates, ComplianceItem screenLock)
	{
		// Report 1: fully compliant, finalized yesterday
		Report compliant = new Report();
		compliant.setDeviceId("MacBook-Pro-Alice.local");
		compliant.setUserId("alice");
		compliant.setKeycloakUser("alice");
		compliant.setCheckedAt(Instant.now().minus(1, ChronoUnit.DAYS));
		compliant.setStatus(ReportStatus.COMPLIANT);
		compliant.setFinalizedAt(Instant.now().minus(1, ChronoUnit.DAYS).plusSeconds(5));
		reportRepository.persist(compliant);

		addResult(compliant, fileVault, true, "[{\"filevault_status\":\"on\"}]");
		addResult(compliant, firewall, true, "[{\"global_state\":\"1\"}]");
		addResult(compliant, autoUpdates, true, "[{\"value\":\"1\"}]");
		addResult(compliant, screenLock, true, "[{\"value\":\"120\"}]");

		// Report 2: non-compliant with deadline, checked an hour ago
		Report nonCompliant = new Report();
		nonCompliant.setDeviceId("MacBook-Air-Bob.local");
		nonCompliant.setUserId("bob");
		nonCompliant.setKeycloakUser("bob");
		nonCompliant.setCheckedAt(Instant.now().minus(1, ChronoUnit.HOURS));
		nonCompliant.setStatus(ReportStatus.NON_COMPLIANT);
		nonCompliant.setDeadline(Instant.now().plus(6, ChronoUnit.DAYS));
		nonCompliant.setFinalizedAt(Instant.now().minus(1, ChronoUnit.HOURS).plusSeconds(5));
		reportRepository.persist(nonCompliant);

		addResult(nonCompliant, fileVault, false, "[]", FILEVAULT_FALLBACK);
		addResult(nonCompliant, firewall, true, "[{\"global_state\":\"1\"}]");
		addResult(nonCompliant, autoUpdates, false, "[{\"value\":\"0\"}]", AUTO_UPDATES_FALLBACK);
		addResult(nonCompliant, screenLock, true, "[{\"value\":\"240\"}]");

		// Report 3: compliant, for the "user" Keycloak test account
		Report userReport = new Report();
		userReport.setDeviceId("MacBook-Pro-User.local");
		userReport.setUserId("user");
		userReport.setKeycloakUser("user");
		userReport.setCheckedAt(Instant.now().minus(2, ChronoUnit.HOURS));
		userReport.setStatus(ReportStatus.COMPLIANT);
		userReport.setFinalizedAt(Instant.now().minus(2, ChronoUnit.HOURS).plusSeconds(5));
		reportRepository.persist(userReport);

		addResult(userReport, fileVault, true, "[{\"filevault_status\":\"on\"}]");
		addResult(userReport, firewall, true, "[{\"global_state\":\"1\"}]");
		addResult(userReport, autoUpdates, true, "[{\"value\":\"1\"}]");
		addResult(userReport, screenLock, true, "[{\"value\":\"180\"}]");

		// Device registry — no periodic flow in dev seed (flow needs Kafka)
		Device alice = new Device();
		alice.setDeviceId("MacBook-Pro-Alice.local");
		alice.setUserId("alice");
		alice.setKeycloakUser("alice");
		alice.setLastReportAt(compliant.getCheckedAt());
		deviceRepository.persist(alice);

		Device bob = new Device();
		bob.setDeviceId("MacBook-Air-Bob.local");
		bob.setUserId("bob");
		bob.setKeycloakUser("bob");
		bob.setLastReportAt(nonCompliant.getCheckedAt());
		deviceRepository.persist(bob);

		Device user = new Device();
		user.setDeviceId("MacBook-Pro-User.local");
		user.setUserId("user");
		user.setKeycloakUser("user");
		user.setLastReportAt(userReport.getCheckedAt());
		deviceRepository.persist(user);
	}

	private ComplianceItem addItem(ComplianceGroup group, String name, String query, String expectedExpression)
	{
		ExpressionCheck item = new ExpressionCheck();
		item.setName(name);
		item.setQuery(query);
		item.setExpectedExpression(expectedExpression);
		item.setGroup(group);
		itemRepository.persist(item);
		return item;
	}

	private void addResult(Report report, ComplianceItem item, boolean passed, String output)
	{
		addResult(report, item, passed, output, null);
	}

	private void addResult(Report report, ComplianceItem item, boolean passed, String output,
		ComplianceResultExplanation fallback)
	{
		ComplianceResult result = new ComplianceResult();
		result.setReport(report);
		result.setItem(item);
		result.setPassed(passed);
		result.setOutput(output);

		// The dev database is recreated on every boot, so — unlike a persisted
		// agent report — the seeded tips are regenerated on every dev run.
		if (!passed)
		{
			ComplianceResultExplanation exp = explain(item, output, fallback);
			result.setAiShortDescription(exp.shortDescription());
			result.setAiLongExplanation(exp.longExplanation());
		}
		resultRepository.persist(result);
	}

	private ComplianceResultExplanation explain(ComplianceItem item, String output,
		ComplianceResultExplanation fallback)
	{
		try
		{
			ResolvedCheck resolved = checkResolver.resolve(item);
			return aiService.explain(item.getName(), resolved.query(), resolved.expression(), output);
		}
		catch (Exception e)
		{
			LOG.warn("AI tip for seeded result '{}' fell back to static text: {}", item.getName(), e.getMessage());
			return fallback != null ? fallback : new ComplianceResultExplanation(null, null);
		}
	}
}
