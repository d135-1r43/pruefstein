package com.pruefstein.compliance.bootstrap;

import java.util.List;

/**
 * The baseline set of compliance checks every deployment starts with, mapped to
 * the ISO 27001 Annex A controls they serve.
 *
 * <p>
 * Keys are permanent. They are what the seed ledger remembers, so renaming one
 * makes the deployment believe it is a new check and create a duplicate
 * alongside whatever the administrator has since done to the original. Add
 * entries freely; never repurpose a key.
 */
public final class ComplianceCatalog
{
	public record GroupDef(String key, String name)
	{
	}

	/**
	 * @param query
	 *            the osquery SQL, or {@code null} for a check whose SQL is
	 *            generated at request time
	 */
	public record CheckDef(String key, String groupKey, String name, String query, String expression)
	{
		public boolean generated()
		{
			return query == null;
		}
	}

	private static final String CRYPTO = "a10";
	private static final String OPERATIONS = "a12";
	private static final String ACCESS = "a9";
	private static final String COMMUNICATIONS = "a13";

	public static final List<GroupDef> GROUPS = List.of(
		new GroupDef(CRYPTO, "A.10 Cryptography"),
		new GroupDef(OPERATIONS, "A.12 Operations Security"),
		new GroupDef(ACCESS, "A.9 Access Control"),
		new GroupDef(COMMUNICATIONS, "A.13 Communications Security"));

	public static final List<CheckDef> CHECKS = List.of(
		new CheckDef("a10.filevault", CRYPTO,
			"FileVault enabled",
			"SELECT filevault_status FROM disk_encryption WHERE filevault_status = 'on' LIMIT 1;",
			"results.size() > 0"),

		new CheckDef("a12.firewall", OPERATIONS,
			"Firewall enabled",
			"SELECT global_state FROM alf;",
			"results.size() > 0 && results[0].global_state == '1'"),

		new CheckDef("a12.auto-updates", OPERATIONS,
			"Automatic updates enabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticCheckEnabled';",
			"results.size() > 0 && results[0].value == '1'"),

		// A.12.6 — technical vulnerability management: checking for updates is
		// not enough, they have to be installed as well
		new CheckDef("a12.critical-updates", OPERATIONS,
			"Critical security updates installed automatically",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'CriticalUpdateInstall';",
			"results.size() > 0 && results[0].value == '1'"),

		new CheckDef("a12.macos-updates", OPERATIONS,
			"macOS updates installed automatically",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticallyInstallMacOSUpdates';",
			"results.size() > 0 && results[0].value == '1'"),

		// A.12.2 — protection against malware
		new CheckDef("a12.gatekeeper", OPERATIONS,
			"Gatekeeper enabled",
			"SELECT assessments_enabled FROM gatekeeper;",
			"results.size() > 0 && results[0].assessments_enabled == '1'"),

		new CheckDef("a12.sip", OPERATIONS,
			"System Integrity Protection enabled",
			"SELECT enabled FROM sip_config WHERE config_flag = 'sip';",
			"results.size() > 0 && results[0].enabled == '1'"),

		// A.12.3 — backup
		new CheckDef("a12.time-machine", OPERATIONS,
			"Time Machine backup destination configured",
			"SELECT destination_id FROM time_machine_destinations;",
			"results.size() > 0"),

		// A.12.4 — logging and monitoring
		new CheckDef("a12.firewall-logging", OPERATIONS,
			"Firewall logging enabled",
			"SELECT logging_enabled FROM alf;",
			"results.size() > 0 && results[0].logging_enabled == '1'"),

		// A.12.6.2 — restrictions on software installation. Deliberately
		// group-less: it lives on the Blocked Apps screen and in its own report
		// section, and its SQL comes from the rules kept there.
		new CheckDef("a12.blocked-apps", null,
			"No blacklisted applications installed", null, null),

		new CheckDef("a9.screen-lock-timeout", ACCESS,
			"Screen lock timeout ≤ 300 seconds",
			"SELECT value FROM preferences WHERE domain = 'com.apple.screensaver' AND key = 'idleTime';",
			"results.size() > 0 && results[0].value <= 300"),

		// A.9.4.2 — secure log-on. A screensaver that blanks the display
		// without asking for a password protects nothing.
		new CheckDef("a9.screen-lock-password", ACCESS,
			"Screen lock requires a password",
			"SELECT enabled, grace_period FROM screenlock;",
			"results.size() > 0 && results[0].enabled == '1' && results[0].grace_period <= 300"),

		// Absence of the key means no auto-login user is configured
		new CheckDef("a9.auto-login", ACCESS,
			"Automatic login disabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.loginwindow' AND key = 'autoLoginUser';",
			"results.size() == 0"),

		new CheckDef("a9.guest-account", ACCESS,
			"Guest account disabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.loginwindow' AND key = 'GuestEnabled';",
			"results.size() == 0 || results[0].value == '0'"),

		// A.13.1 — network security management: every inbound sharing service
		// is attack surface on an endpoint that need not serve anything
		new CheckDef("a13.remote-login", COMMUNICATIONS,
			"Remote login (SSH) disabled",
			"SELECT remote_login FROM sharing_preferences;",
			"results.size() > 0 && results[0].remote_login == '0'"),

		new CheckDef("a13.screen-sharing", COMMUNICATIONS,
			"Screen sharing disabled",
			"SELECT screen_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].screen_sharing == '0'"),

		new CheckDef("a13.file-sharing", COMMUNICATIONS,
			"File sharing disabled",
			"SELECT file_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].file_sharing == '0'"),

		new CheckDef("a13.internet-sharing", COMMUNICATIONS,
			"Internet sharing disabled",
			"SELECT internet_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].internet_sharing == '0'"),

		new CheckDef("a13.stealth-mode", COMMUNICATIONS,
			"Firewall stealth mode enabled",
			"SELECT stealth_enabled FROM alf;",
			"results.size() > 0 && results[0].stealth_enabled == '1'"));

	private ComplianceCatalog()
	{
	}

	public static String groupName(String groupKey)
	{
		return GROUPS.stream()
			.filter(group -> group.key().equals(groupKey))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No group defined for key " + groupKey))
			.name();
	}
}
