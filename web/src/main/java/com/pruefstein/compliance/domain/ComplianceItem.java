package com.pruefstein.compliance.domain;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class ComplianceItem extends PanacheEntity
{
    private String name;

    @ManyToOne
    private ComplianceGroup group;

    @OneToOne(mappedBy = "item")
    private ComplianceResult result;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ComplianceGroup getGroup() {
        return group;
    }

    public void setGroup(ComplianceGroup group) {
        this.group = group;
    }

    public ComplianceResult getResult() {
        return result;
    }

    public void setResult(ComplianceResult result) {
        this.result = result;
    }
}
