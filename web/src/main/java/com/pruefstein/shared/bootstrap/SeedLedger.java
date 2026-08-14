package com.pruefstein.shared.bootstrap;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tracks which seed entries this database has already seen, so seeding can run
 * on every boot without repeating itself or undoing an administrator's work.
 */
@ApplicationScoped
public class SeedLedger implements PanacheRepositoryBase<SeedLedgerEntry, String>
{
	/**
	 * Claims an entry for the caller to apply.
	 *
	 * @return {@code true} if this database has never seen the entry, in which
	 *         case it is now recorded and the caller should create it;
	 *         {@code false} if it was applied before and must be left alone
	 */
	public boolean claim(String entryKey)
	{
		if (findByIdOptional(entryKey).isPresent())
		{
			return false;
		}
		persist(new SeedLedgerEntry(entryKey, Instant.now()));
		return true;
	}
}
