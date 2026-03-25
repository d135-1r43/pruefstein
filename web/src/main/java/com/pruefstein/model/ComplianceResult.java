package com.pruefstein.model;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class ComplianceResult extends PanacheEntity
{
    @OneToOne
    public ComplianceItem item;

    @ManyToOne
    public Report report;
}