package com.pruefstein.repository;

import com.pruefstein.model.ComplianceResult;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceResultRepository implements PanacheRepository<ComplianceResult>
{
}
