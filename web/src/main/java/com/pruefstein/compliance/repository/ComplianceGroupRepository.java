package com.pruefstein.compliance.repository;

import com.pruefstein.compliance.domain.ComplianceGroup;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceGroupRepository implements PanacheRepository<ComplianceGroup>
{
}
