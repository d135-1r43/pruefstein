package com.pruefstein.compliance.bootstrap;

import java.util.List;

import com.pruefstein.compliance.bootstrap.ComplianceCatalog.CheckDef;
import com.pruefstein.compliance.bootstrap.ComplianceCatalog.GroupDef;
import com.pruefstein.compliance.domain.AppBlacklistCheck;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.shared.bootstrap.SeedLedger;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seeding runs on every boot, so what matters is not that it creates the
 * catalog once but that it never touches what an administrator did afterwards.
 */
@QuarkusTest
class CatalogSeederTest
{
	@Inject
	CatalogSeeder seeder;

	@Inject
	SeedLedger ledger;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceGroupRepository groupRepository;

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			for (CheckDef def : ComplianceCatalog.CHECKS)
			{
				itemRepository.delete("name", def.name());
				ledger.deleteById(def.key());
			}
			for (GroupDef group : ComplianceCatalog.GROUPS)
			{
				groupRepository.delete("name", group.name());
			}
		});
	}

	@Test
	void theFirstRunCreatesTheWholeCatalog()
	{
		// when
		int applied = seed();

		// then
		assertEquals(ComplianceCatalog.CHECKS.size(), applied);
		for (CheckDef def : ComplianceCatalog.CHECKS)
		{
			assertNotNull(find(def.name()), def.name() + " should have been seeded");
		}
		for (GroupDef group : ComplianceCatalog.GROUPS)
		{
			assertTrue(groupRepository.find("name", group.name()).firstResultOptional().isPresent(),
				group.name() + " should have been created");
		}
	}

	@Test
	void theGeneratedCheckIsSeededWithoutAGroup()
	{
		// when
		seed();

		// then — its SQL comes from the blocked-app rules, and it belongs to no
		// group on the Groups & Items screen
		ComplianceItem blacklist = find("No blacklisted applications installed");
		assertInstanceOf(AppBlacklistCheck.class, blacklist);
		assertNull(blacklist.getGroup());
	}

	@Test
	void aSecondRunAddsNothing()
	{
		// given
		seed();

		// when — every boot from here on
		int applied = seed();

		// then
		assertEquals(0, applied);
		assertEquals(1, itemRepository.count("name", "FileVault enabled"));
	}

	@Test
	void aCheckTheAdministratorDeletedStaysDeleted()
	{
		// given
		seed();
		QuarkusTransaction.requiringNew().run(() -> itemRepository.delete("name", "Gatekeeper enabled"));

		// when
		int applied = seed();

		// then — the ledger remembers it was applied, so it is not resurrected
		assertEquals(0, applied);
		assertNull(find("Gatekeeper enabled"));
	}

	@Test
	void anEditedCheckIsLeftAlone()
	{
		// given — an administrator tightens the screen lock timeout
		seed();
		QuarkusTransaction.requiringNew().run(() -> {
			ExpressionCheck check = (ExpressionCheck)itemRepository
				.list("name", "Screen lock timeout ≤ 300 seconds").get(0);
			check.setExpectedExpression("results.size() > 0 && results[0].value <= 60");
		});

		// when
		seed();

		// then
		ExpressionCheck check = (ExpressionCheck)find("Screen lock timeout ≤ 300 seconds");
		assertEquals("results.size() > 0 && results[0].value <= 60", check.getExpectedExpression());
	}

	@Test
	void aCheckAddedToTheCatalogLaterIsPickedUp()
	{
		// given — everything applied, as after an upgrade of an existing
		// install
		seed();

		// when — the next release adds an entry, which is a key the ledger has
		// never seen
		QuarkusTransaction.requiringNew().run(() -> ledger.deleteById("a12.gatekeeper"));
		QuarkusTransaction.requiringNew().run(() -> itemRepository.delete("name", "Gatekeeper enabled"));
		int applied = seed();

		// then — only that one is created
		assertEquals(1, applied);
		assertNotNull(find("Gatekeeper enabled"));
	}

	@Test
	void aDeletedGroupIsRecreatedForTheCheckThatNeedsIt()
	{
		// given — the group is gone along with the ledger entry of one of its
		// checks
		seed();
		QuarkusTransaction.requiringNew().run(() -> {
			itemRepository.delete("name", "FileVault enabled");
			ledger.deleteById("a10.filevault");
			groupRepository.delete("name", "A.10 Cryptography");
		});

		// when
		seed();

		// then — a check has to live somewhere
		assertNotNull(find("FileVault enabled").getGroup());
		assertEquals("A.10 Cryptography", find("FileVault enabled").getGroup().getName());
	}

	private int seed()
	{
		return QuarkusTransaction.requiringNew().call(() -> seeder.seed());
	}

	private ComplianceItem find(String name)
	{
		List<ComplianceItem> found = QuarkusTransaction.requiringNew()
			.call(() -> itemRepository.list("name", name));
		return found.isEmpty() ? null : found.get(0);
	}
}
