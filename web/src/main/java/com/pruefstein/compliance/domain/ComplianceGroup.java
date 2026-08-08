package com.pruefstein.compliance.domain;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
public class ComplianceGroup extends PanacheEntity
{
	private String name;

	@OneToMany(mappedBy = "group")
	private List<ComplianceItem> items;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<ComplianceItem> getItems()
	{
		return items;
	}

	public void setItems(List<ComplianceItem> items)
	{
		this.items = items;
	}
}
