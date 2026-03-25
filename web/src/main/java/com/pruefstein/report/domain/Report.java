package com.pruefstein.report.domain;

import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.user.domain.User;
import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Report extends PanacheEntity
{
    private String title;

    @ManyToOne
    private User user;

    @OneToMany(mappedBy = "report")
    private List<ComplianceResult> results;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ComplianceResult> getResults() {
        return results;
    }

    public void setResults(List<ComplianceResult> results) {
        this.results = results;
    }
}
