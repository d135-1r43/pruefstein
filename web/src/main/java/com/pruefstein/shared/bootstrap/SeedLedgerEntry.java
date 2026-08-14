package com.pruefstein.shared.bootstrap;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Record that one seed entry has been applied to this database, ever.
 *
 * <p>
 * The key is the contract: it identifies what was seeded, not what the row
 * currently looks like. Seeded data belongs to whoever administers it
 * afterwards, so an entry is applied once and never reconciled — an edited
 * check keeps its edits and a deleted one stays deleted.
 */
@Entity
public class SeedLedgerEntry extends PanacheEntityBase
{
	@Id
	@Column(length = 96)
	private String entryKey;

	private Instant appliedAt;

	public SeedLedgerEntry()
	{
	}

	public SeedLedgerEntry(String entryKey, Instant appliedAt)
	{
		this.entryKey = entryKey;
		this.appliedAt = appliedAt;
	}

	public String getEntryKey()
	{
		return entryKey;
	}

	public Instant getAppliedAt()
	{
		return appliedAt;
	}
}
