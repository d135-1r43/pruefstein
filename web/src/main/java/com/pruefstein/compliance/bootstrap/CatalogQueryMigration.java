package com.pruefstein.compliance.bootstrap;

import java.util.List;

import com.pruefstein.compliance.bootstrap.ComplianceCatalog.CheckDef;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.shared.bootstrap.SeedLedger;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces baseline check SQL that shipped measuring the wrong thing.
 *
 * <p>
 * {@link CatalogSeeder} applies a check once and never reconciles it, which is
 * what keeps an administrator's edits safe — but it also means a query
 * corrected in {@link ComplianceCatalog} never reaches a database that already
 * seeded the broken one. Nothing else closes that gap: checks are data rather
 * than schema, so no schema-migration tool would see them.
 *
 * <p>
 * Each rewrite is ledgered under its own key, so it happens at most once per
 * database, and it only touches a check whose stored SQL is still
 * byte-identical to what the catalog used to ship. An administrator who has
 * edited the query, or deleted the check, keeps that decision — the same
 * promise {@link SeedLedger} already gives the seeder.
 *
 * <p>
 * This is for SQL that never measured what its check claimed to measure. A
 * query that is merely phrased differently belongs in the catalog alone, where
 * it reaches new deployments and leaves existing ones untouched.
 */
@ApplicationScoped
public class CatalogQueryMigration
{
	/** After the catalog exists, before the demo data that builds on it. */
	public static final int PRIORITY = CatalogSeeder.PRIORITY + 10;

	private static final Logger LOG = LoggerFactory.getLogger(CatalogQueryMigration.class);

	/**
	 * @param ledgerKey
	 *            permanent, and deliberately not the check's own seed key:
	 *            claiming it must never look like having seeded the check
	 * @param checkKey
	 *            the catalog entry that holds the corrected SQL, so the fix
	 *            lives in one place and the two cannot drift apart
	 * @param outdatedQuery
	 *            the SQL to replace, exactly as it was shipped
	 */
	record Rewrite(String ledgerKey, String checkKey, String outdatedQuery)
	{
	}

	/**
	 * Every one of these read a machine-level setting through the
	 * {@code preferences} table, which returns nothing at all to an agent
	 * running as the invoking user rather than as root — so none of them was
	 * measuring the machine it reported on.
	 *
	 * <p>
	 * The direction of the resulting lie differed. The
	 * {@code com.apple.SoftwareUpdate} checks and the screen lock timeout
	 * demanded a row and so failed every device, automatic updates switched on
	 * or not. The two {@code com.apple.loginwindow} checks treated no rows as
	 * compliant, and passed every device whether or not automatic login was
	 * enabled or a guest account was available — the more dangerous half,
	 * because a fleet full of them looked clean.
	 */
	static final List<Rewrite> REWRITES = List.of(
		new Rewrite("a12.auto-updates#plist", "a12.auto-updates",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticCheckEnabled';"),
		new Rewrite("a12.critical-updates#plist", "a12.critical-updates",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'CriticalUpdateInstall';"),
		new Rewrite("a12.macos-updates#plist", "a12.macos-updates",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticallyInstallMacOSUpdates';"),
		new Rewrite("a9.screen-lock-timeout#plist", "a9.screen-lock-timeout",
			"SELECT value FROM preferences WHERE domain = 'com.apple.screensaver' AND key = 'idleTime';"),
		new Rewrite("a9.auto-login#plist", "a9.auto-login",
			"SELECT value FROM preferences WHERE domain = 'com.apple.loginwindow' AND key = 'autoLoginUser';"),
		new Rewrite("a9.guest-account#plist", "a9.guest-account",
			"SELECT value FROM preferences WHERE domain = 'com.apple.loginwindow' AND key = 'GuestEnabled';"));

	@Inject
	SeedLedger ledger;

	@Inject
	ComplianceItemRepository itemRepository;

	/**
	 * A deployment that has frozen its catalog has opted out of us changing its
	 * checks, corrections included.
	 */
	@ConfigProperty(name = "pruefstein.compliance.seed-catalog", defaultValue = "true")
	boolean seedingEnabled;

	@Transactional
	void migrateOnStartup(@Observes @Priority(PRIORITY) StartupEvent event)
	{
		if (!seedingEnabled)
		{
			return;
		}
		int rewritten = migrate();
		if (rewritten > 0)
		{
			LOG.info("Corrected the SQL of {} baseline compliance check(s)", rewritten);
		}
	}

	/**
	 * @return how many checks this call rewrote; zero on a database seeded
	 *         after the fix, which already has the corrected SQL
	 */
	public int migrate()
	{
		int rewritten = 0;
		for (Rewrite rewrite : REWRITES)
		{
			if (!ledger.claim(rewrite.ledgerKey()))
			{
				continue;
			}
			CheckDef corrected = ComplianceCatalog.check(rewrite.checkKey());
			for (ExpressionCheck check : itemRepository.findByQuery(rewrite.outdatedQuery()))
			{
				check.setQuery(corrected.query());
				check.setExpectedExpression(corrected.expression());
				rewritten++;
			}
		}
		return rewritten;
	}
}
