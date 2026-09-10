package com.pruefstein.compliance.bootstrap;

import java.util.List;

/**
 * The baseline set of compliance checks every deployment starts with, mapped to
 * the ISO/IEC 27001:2022 Annex A controls they serve.
 *
 * <p>
 * Groups are the four themes the 2022 revision organises Annex A into, and each
 * check additionally names the individual control it evidences. The theme is
 * what an administrator navigates; the control is what an auditor traces. A.6
 * People has no entry here because nothing about it is measurable on an
 * endpoint — the seeder only creates a group some check asks for.
 *
 * <p>
 * Keys are permanent. They are what the seed ledger remembers, so renaming one
 * makes the deployment believe it is a new check and create a duplicate
 * alongside whatever the administrator has since done to the original. Add
 * entries freely; never repurpose a key. The {@code a9.}/{@code a10.}/
 * {@code a12.}/{@code a13.} prefixes are the 2013 domains these checks were
 * first written against: they are meaningless now and deliberately left alone,
 * because renaming them would cost every existing deployment a duplicate of
 * every check. Read the key as an opaque identifier and the {@code control} as
 * the standard it maps to.
 */
public final class ComplianceCatalog
{
	public record GroupDef(String key, String name)
	{
	}

	/**
	 * @param control
	 *            the Annex A control this check evidences, as it is numbered in
	 *            ISO/IEC 27001:2022
	 * @param query
	 *            the osquery SQL, or {@code null} for a check whose SQL is
	 *            generated at request time
	 */
	public record CheckDef(String key, String groupKey, String control, String name, String query, String expression)
	{
		public boolean generated()
		{
			return query == null;
		}
	}

	private static final String ORGANIZATIONAL = "a5";
	private static final String PHYSICAL = "a7";
	private static final String TECHNOLOGICAL = "a8";

	/**
	 * Where macOS keeps the automatic-update settings: the local file, and the
	 * copy a management profile writes when the setting is enforced centrally.
	 */
	private static final String SOFTWARE_UPDATE_PLIST = "/Library/Preferences/com.apple.SoftwareUpdate.plist";

	private static final String MANAGED_SOFTWARE_UPDATE_PLIST = "/Library/Managed Preferences/com.apple.SoftwareUpdate.plist";

	/**
	 * Passes when the effective value is on. osquery renders a plist boolean as
	 * {@code 1} or as {@code true} depending on how the value was written, and
	 * either one means enabled.
	 */
	private static final String SOFTWARE_UPDATE_ENABLED = "results.size() > 0 && (results[0].value == '1' || results[0].value == 'true')";

	/**
	 * Passes unless the setting is explicitly off.
	 *
	 * <p>
	 * For {@code AutomaticCheckEnabled}, which macOS does not write at all
	 * while it holds its default: on 26.6 the key is absent from
	 * {@code com.apple.SoftwareUpdate} on a machine where
	 * {@code softwareupdate --schedule} reports automatic checking as turned
	 * on. Demanding that the key be present therefore fails every Mac that has
	 * simply been left alone, which is the opposite of what this check is for.
	 * A managed fleet still gets a real assertion — enforcing the setting by
	 * profile writes the key, and a value of 0 fails here.
	 */
	private static final String SOFTWARE_UPDATE_NOT_DISABLED = "results.size() == 0 || results[0].value == '1' || results[0].value == 'true'";

	/** Machine-level login window settings, not the per-user domain. */
	private static final String LOGIN_WINDOW_PLIST = "/Library/Preferences/com.apple.loginwindow.plist";

	private static final String MANAGED_LOGIN_WINDOW_PLIST = "/Library/Managed Preferences/com.apple.loginwindow.plist";

	/**
	 * Passes when a login window setting is not switched on, and we can show
	 * that we actually looked.
	 *
	 * <p>
	 * The second half is the point. Reading a setting that is simply absent and
	 * reading nothing at all because the file could not be opened both used to
	 * come back as zero rows, and the check called both of them compliant — so
	 * it would have passed a machine with automatic login enabled just as
	 * happily as one without. Requiring that the file either not exist, or have
	 * been read, is what separates "the setting is off" from "we have no idea".
	 */
	private static final String LOGIN_WINDOW_OFF = "results.size() > 0 && results[0].switched_on == 0 && (results[0].file_present == 0 || results[0].keys_read > 0)";

	/**
	 * Every screen saver idle timeout on the machine — the managed value, and
	 * each user's own, including the by-host copy macOS actually writes.
	 */
	private static final String SCREENSAVER_IDLE_QUERY = "SELECT count(*) AS configured,"
		+ " min(cast(value AS integer)) AS shortest, max(cast(value AS integer)) AS longest"
		+ " FROM plist WHERE (path = '/Library/Managed Preferences/com.apple.screensaver.plist'"
		+ " OR path LIKE '/Users/%/Library/Preferences/com.apple.screensaver.plist'"
		+ " OR path LIKE '/Users/%/Library/Preferences/ByHost/com.apple.screensaver.%')"
		+ " AND key = 'idleTime';";

	/**
	 * Passes only when every account on the machine has a screen saver that
	 * starts, and starts within five minutes.
	 *
	 * <p>
	 * Aggregated in SQL because the check is about the machine rather than any
	 * one account: {@code longest} is what holds the whole fleet to the policy,
	 * and {@code shortest} rejects a timeout of 0, which means the screen saver
	 * never starts and which a plain {@code <= 300} would have waved through.
	 * No timeout configured anywhere fails as well — nothing then evidences
	 * that the screen locks at all, and passing on an absent setting is how the
	 * old version of this check reported machines it had never really measured.
	 */
	private static final String SCREEN_LOCK_WITHIN_POLICY = "results.size() > 0 && results[0].configured > 0 && results[0].shortest > 0 && results[0].longest <= 300";
	public static final List<GroupDef> GROUPS = List.of(
		new GroupDef(ORGANIZATIONAL, "A.5 Organizational controls"),
		new GroupDef(PHYSICAL, "A.7 Physical controls"),
		new GroupDef(TECHNOLOGICAL, "A.8 Technological controls"));

	public static final List<CheckDef> CHECKS = List.of(
		new CheckDef("a10.filevault", TECHNOLOGICAL, "A.8.24",
			"FileVault enabled",
			"SELECT filevault_status FROM disk_encryption WHERE filevault_status = 'on' LIMIT 1;",
			"results.size() > 0"),

		new CheckDef("a12.firewall", TECHNOLOGICAL, "A.8.20",
			"Firewall enabled",
			"SELECT global_state FROM alf;",
			"results.size() > 0 && results[0].global_state == '1'"),

		new CheckDef("a12.auto-updates", TECHNOLOGICAL, "A.8.8",
			"Automatic updates enabled",
			softwareUpdateQuery("AutomaticCheckEnabled"),
			SOFTWARE_UPDATE_NOT_DISABLED),

		// Checking for updates is not enough, they have to be installed as well
		new CheckDef("a12.critical-updates", TECHNOLOGICAL, "A.8.8",
			"Critical security updates installed automatically",
			softwareUpdateQuery("CriticalUpdateInstall"),
			SOFTWARE_UPDATE_ENABLED),

		new CheckDef("a12.macos-updates", TECHNOLOGICAL, "A.8.8",
			"macOS updates installed automatically",
			softwareUpdateQuery("AutomaticallyInstallMacOSUpdates"),
			SOFTWARE_UPDATE_ENABLED),

		new CheckDef("a12.gatekeeper", TECHNOLOGICAL, "A.8.7",
			"Gatekeeper enabled",
			"SELECT assessments_enabled FROM gatekeeper;",
			"results.size() > 0 && results[0].assessments_enabled == '1'"),

		new CheckDef("a12.sip", TECHNOLOGICAL, "A.8.7",
			"System Integrity Protection enabled",
			"SELECT enabled FROM sip_config WHERE config_flag = 'sip';",
			"results.size() > 0 && results[0].enabled == '1'"),

		new CheckDef("a12.time-machine", TECHNOLOGICAL, "A.8.13",
			"Time Machine backup destination configured",
			"SELECT destination_id FROM time_machine_destinations;",
			"results.size() > 0"),

		new CheckDef("a12.firewall-logging", TECHNOLOGICAL, "A.8.15",
			"Firewall logging enabled",
			"SELECT logging_enabled FROM alf;",
			"results.size() > 0 && results[0].logging_enabled == '1'"),

		// Deliberately group-less: it lives on the Blocked Apps screen and in
		// its own report section, and its SQL comes from the rules kept there.
		// The control still applies, so it carries one.
		new CheckDef("a12.blocked-apps", null, "A.8.19",
			"No blacklisted applications installed", null, null),

		new CheckDef("a9.screen-lock-timeout", PHYSICAL, "A.7.7",
			"Screen lock timeout ≤ 300 seconds",
			SCREENSAVER_IDLE_QUERY,
			SCREEN_LOCK_WITHIN_POLICY),

		// A screensaver that blanks the display without asking for a password
		// protects nothing.
		new CheckDef("a9.screen-lock-password", PHYSICAL, "A.7.7",
			"Screen lock requires a password",
			"SELECT enabled, grace_period FROM screenlock;",
			"results.size() > 0 && results[0].enabled == '1' && results[0].grace_period <= 300"),

		// Automatic login bypasses authentication outright, which is why this
		// is secure authentication rather than access control.
		new CheckDef("a9.auto-login", TECHNOLOGICAL, "A.8.5",
			"Automatic login disabled",
			loginWindowQuery("autoLoginUser", "value != ''"),
			LOGIN_WINDOW_OFF),

		// An account nobody owns cannot be granted or revoked, so this sits
		// with access control as a rule rather than with the technological
		// controls.
		new CheckDef("a9.guest-account", ORGANIZATIONAL, "A.5.15",
			"Guest account disabled",
			loginWindowQuery("GuestEnabled", "value IN ('1', 'true')"),
			LOGIN_WINDOW_OFF),

		// Every inbound sharing service is attack surface on an endpoint that
		// need not serve anything
		new CheckDef("a13.remote-login", TECHNOLOGICAL, "A.8.20",
			"Remote login (SSH) disabled",
			"SELECT remote_login FROM sharing_preferences;",
			"results.size() > 0 && results[0].remote_login == '0'"),

		new CheckDef("a13.screen-sharing", TECHNOLOGICAL, "A.8.20",
			"Screen sharing disabled",
			"SELECT screen_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].screen_sharing == '0'"),

		new CheckDef("a13.file-sharing", TECHNOLOGICAL, "A.8.20",
			"File sharing disabled",
			"SELECT file_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].file_sharing == '0'"),

		new CheckDef("a13.internet-sharing", TECHNOLOGICAL, "A.8.20",
			"Internet sharing disabled",
			"SELECT internet_sharing FROM sharing_preferences;",
			"results.size() > 0 && results[0].internet_sharing == '0'"),

		new CheckDef("a13.stealth-mode", TECHNOLOGICAL, "A.8.20",
			"Firewall stealth mode enabled",
			"SELECT stealth_enabled FROM alf;",
			"results.size() > 0 && results[0].stealth_enabled == '1'"));

	private ComplianceCatalog()
	{
	}

	/**
	 * @throws IllegalStateException
	 *             if the key names no check — a caller holding a key the
	 *             catalog has dropped is a bug, not a no-op
	 */
	public static CheckDef check(String checkKey)
	{
		return CHECKS.stream()
			.filter(def -> def.key().equals(checkKey))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No check defined for key " + checkKey));
	}

	/**
	 * The SQL that reads one {@code com.apple.SoftwareUpdate} preference.
	 *
	 * <p>
	 * Read through {@code plist} rather than the {@code preferences} table.
	 * These settings live in the system domain, the agent runs {@code osqueryi}
	 * as the invoking user rather than as root, and the {@code preferences}
	 * table additionally ignores by-host values (osquery#3501). A user-context
	 * read of the domain comes back empty on a machine that has automatic
	 * updates switched on, and an empty result fails the expression — reporting
	 * a compliant device as non-compliant, which is the one direction a
	 * compliance check must never fail in.
	 *
	 * <p>
	 * Both paths are read because a management profile overrides the local
	 * file. {@code ORDER BY path} is what puts the managed value first —
	 * {@code Managed Preferences} sorts ahead of {@code Preferences} — so
	 * {@code results[0]} is the value actually in effect.
	 */
	private static String softwareUpdateQuery(String preferenceKey)
	{
		return "SELECT path, key, value FROM plist WHERE path IN ('" + MANAGED_SOFTWARE_UPDATE_PLIST
			+ "', '" + SOFTWARE_UPDATE_PLIST + "') AND key = '" + preferenceKey + "' ORDER BY path;";
	}

	/**
	 * SQL that counts how often a machine-level {@code com.apple.loginwindow}
	 * setting is switched on, alongside the two numbers
	 * {@link #LOGIN_WINDOW_OFF} needs to tell an absent setting apart from an
	 * unread file.
	 *
	 * <p>
	 * The {@code COALESCE} is how a management profile takes precedence: the
	 * setting is counted at the managed path when that file carries the key,
	 * and at the local one otherwise. Counting both together would instead fail
	 * a machine whose profile forces the setting off over a stale local value
	 * that says on.
	 *
	 * @param onPredicate
	 *            SQL matching the values that mean the setting is on. It is
	 *            spelled out per setting because these are not all booleans:
	 *            {@code autoLoginUser} holds a user name, so any non-empty
	 *            value means automatic login is configured.
	 */
	private static String loginWindowQuery(String preferenceKey, String onPredicate)
	{
		String paths = "'" + MANAGED_LOGIN_WINDOW_PLIST + "', '" + LOGIN_WINDOW_PLIST + "'";
		return "SELECT"
			+ " (SELECT count(*) FROM file WHERE path IN (" + paths + ")) AS file_present,"
			+ " (SELECT count(*) FROM plist WHERE path IN (" + paths + ")) AS keys_read,"
			+ " (SELECT count(*) FROM plist WHERE key = '" + preferenceKey + "' AND " + onPredicate
			+ " AND path = COALESCE((SELECT '" + MANAGED_LOGIN_WINDOW_PLIST + "' FROM plist WHERE path = '"
			+ MANAGED_LOGIN_WINDOW_PLIST + "' AND key = '" + preferenceKey + "' LIMIT 1), '"
			+ LOGIN_WINDOW_PLIST + "')) AS switched_on;";
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
