package com.pruefstein.compliance.domain;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

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
