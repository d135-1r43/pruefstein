package com.pruefstein.model;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class ComplianceGroup extends PanacheEntity
{
    public String name;

    @OneToMany(mappedBy = "group")
    public List<ComplianceItem> items;
}