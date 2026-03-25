package com.pruefstein.compliance.repository;

import com.pruefstein.compliance.domain.ComplianceResult;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceResultRepository implements PanacheRepository<ComplianceResult>
{
}
