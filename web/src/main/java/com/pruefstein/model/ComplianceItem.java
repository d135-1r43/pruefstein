package com.pruefstein.model;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class ComplianceItem extends PanacheEntity
{
    public String name;

    @ManyToOne
    public ComplianceGroup group;

    @OneToOne(mappedBy = "item")
    public ComplianceResult result;
}