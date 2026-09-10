package com.pruefstein.compliance.bootstrap;

import java.util.List;

import com.pruefstein.compliance.bootstrap.ComplianceCatalog.CheckDef;
import com.pruefstein.compliance.bootstrap.ComplianceCatalog.GroupDef;
import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.shared.bootstrap.SeedLedger;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A database seeded before the 2022 revision has its checks in groups named for
 * the old domains. Re-filing them has to move ours without disturbing anything
 * an administrator put there.
 */
@QuarkusTest
class AnnexA2022MigrationTest
{
	private static final String LEDGER_KEY = "annex-a-2022#themes";
	private static final String OLD_CRYPTO_GROUP = "A.10 Cryptography";
	private static final String FILEVAULT = "FileVault enabled";

	@Inject
	AnnexA2022Migration migration;

	@Inject
	SeedLedger ledger;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceGroupRepository groupRepository;

	/**
	 * Seeding is off under {@code %test}, so nothing here is startup's doing —
	 * this clears what the previous test in the class left behind, so each one
	 * begins from an unclaimed ledger and builds the rows it needs itself.
	 */
	@BeforeEach
	void clearPreviousTest()
	{
		reset();
	}

	@AfterEach
	void tearDown()
	{
		reset();
	}

	@Test
	void aCheckInAnOldDomainGroupMovesToItsThemeAndGainsItsControl()
	{
		// given — the row a pre-2022 release left behind
		givenLegacyCheck(FILEVAULT, OLD_CRYPTO_GROUP);

		// when
		int refiled = migrate();

		// then
		assertTrue(refiled > 0, "the seeded check should have been re-filed");
		ComplianceItem check = find(FILEVAULT);
		assertEquals("A.8 Technological controls", check.getGroup().getName());
		assertEquals("A.8.24", check.getControl());
	}

	@Test
	void theEmptiedOldGroupIsRemoved()
	{
		// given
		givenLegacyCheck(FILEVAULT, OLD_CRYPTO_GROUP);

		// when
		migrate();

		// then — nothing is left in it, so it stops cluttering the screen
		assertTrue(groupRepository.list("name", OLD_CRYPTO_GROUP).isEmpty(),
			OLD_CRYPTO_GROUP + " should have been dropped once empty");
	}

	@Test
	void anOldGroupKeepingTheAdministratorsOwnCheckSurvives()
	{
		// given — they filed a check of their own alongside ours
		givenLegacyCheck(FILEVAULT, OLD_CRYPTO_GROUP);
		givenLegacyCheck("Our own crypto check", OLD_CRYPTO_GROUP);

		// when
		migrate();

		// then — deleting the group would take their work with it
		List<ComplianceGroup> survivors = QuarkusTransaction.requiringNew()
			.call(() -> groupRepository.list("name", OLD_CRYPTO_GROUP));
		assertEquals(1, survivors.size(), OLD_CRYPTO_GROUP + " should have been kept");
		assertEquals(OLD_CRYPTO_GROUP, find("Our own crypto check").getGroup().getName());
	}

	@Test
	void theGeneratedCheckKeepsItsControlWithoutGainingAGroup()
	{
		// given
		givenLegacyCheck("No blacklisted applications installed", OLD_CRYPTO_GROUP);

		// when
		migrate();

		// then — it belongs on the Blocked Apps screen, not in a group, but the
		// control still applies to it
		ComplianceItem blacklist = find("No blacklisted applications installed");
		assertNull(blacklist.getGroup());
		assertEquals("A.8.19", blacklist.getControl());
	}

	@Test
	void theReFilingHappensOnlyOnce()
	{
		// given — migrated, then an administrator deliberately moves a check
		// back into a group of their own
		givenLegacyCheck(FILEVAULT, OLD_CRYPTO_GROUP);
		migrate();
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceItem check = itemRepository.list("name", FILEVAULT).get(0);
			check.setGroup(groupRepository.findOrCreateByName("Where I want it"));
		});

		// when — the application restarts
		int refiled = migrate();

		// then — the ledger remembers, so their choice stands
		assertEquals(0, refiled);
		assertEquals("Where I want it", find(FILEVAULT).getGroup().getName());
	}

	@Test
	void everyCatalogCheckNamesAControlInAThemeThatExists()
	{
		// then — a check with no control would show a blank cell to an auditor,
		// and one pointing at an undefined group would fail to seed at all
		List<String> themes = ComplianceCatalog.GROUPS.stream().map(GroupDef::key).toList();
		for (CheckDef def : ComplianceCatalog.CHECKS)
		{
			assertNotNull(def.control(), def.key() + " has no control");
			assertTrue(def.control().startsWith("A."), def.key() + ": " + def.control());
			if (def.groupKey() != null)
			{
				assertTrue(themes.contains(def.groupKey()),
					def.key() + " is filed under undefined group " + def.groupKey());
			}
		}
	}

	private void givenLegacyCheck(String name, String groupName)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			ExpressionCheck check = new ExpressionCheck();
			check.setName(name);
			check.setQuery("SELECT 1;");
			check.setExpectedExpression("results.size() > 0");
			check.setGroup(groupRepository.findOrCreateByName(groupName));
			itemRepository.persist(check);
		});
	}

	private void reset()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			for (CheckDef def : ComplianceCatalog.CHECKS)
			{
				itemRepository.delete("name", def.name());
			}
			itemRepository.delete("name", "Our own crypto check");
			ledger.deleteById(LEDGER_KEY);
			for (String name : List.of(OLD_CRYPTO_GROUP, "Where I want it"))
			{
				for (ComplianceGroup group : groupRepository.list("name", name))
				{
					groupRepository.delete(group);
				}
			}
		});
	}

	private int migrate()
	{
		return QuarkusTransaction.requiringNew().call(() -> migration.migrate());
	}

	private ComplianceItem find(String name)
	{
		List<ComplianceItem> found = QuarkusTransaction.requiringNew()
			.call(() -> itemRepository.list("name", name));
		return found.isEmpty() ? null : found.get(0);
	}
}
