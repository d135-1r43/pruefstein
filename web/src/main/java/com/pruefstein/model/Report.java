package com.pruefstein.model;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Report extends PanacheEntity
{
    public String title;

    @ManyToOne
    public User user;

    @OneToMany(mappedBy = "report")
    public List<ComplianceResult> results;
}
