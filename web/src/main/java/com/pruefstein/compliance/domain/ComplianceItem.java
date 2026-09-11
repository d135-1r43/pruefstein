package com.pruefstein.compliance.domain;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * A check that belongs to a compliance group and produces one
 * {@link ComplianceResult} per report.
 * <p>
 * Everything shared by every kind of check lives here — a name, its group, and
 * its history. What a check actually asks the device is left to the subclass:
 * {@link ExpressionCheck} carries admin-authored osquery SQL, while
 * {@link AppBlacklistCheck} derives its SQL from the {@link BlockedApp} list.
 * Use {@code CheckResolver} to obtain the query and expression for any check
 * rather than branching on the type at the call site.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "check_type")
public abstract class ComplianceItem extends PanacheEntity
{
	private String name;

	/**
	 * The ISO/IEC 27001:2022 Annex A control this check evidences, such as
	 * {@code A.8.8} — the group holds only the broader theme.
	 *
	 * <p>
	 * Null for a check an administrator wrote themselves: we know which theme
	 * they filed it under, and inventing a control number on their behalf would
	 * put a claim in front of an auditor that nobody made.
	 */
	@Column(length = 16)
	private String control;

	@ManyToOne
	private ComplianceGroup group;

	@OneToMany(mappedBy = "item")
	private List<ComplianceResult> results;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getControl()
	{
		return control;
	}

	public void setControl(String control)
	{
		this.control = control;
	}

	public ComplianceGroup getGroup()
	{
		return group;
	}

	public void setGroup(ComplianceGroup group)
	{
		this.group = group;
	}

	public List<ComplianceResult> getResults()
	{
		return results;
	}

	public void setResults(List<ComplianceResult> results)
	{
		this.results = results;
	}

	/**
	 * Whether an admin can edit this check's query and expression directly.
	 * Generated checks are managed through their own screen instead.
	 */
	public boolean isEditable()
	{
		return true;
	}
}
