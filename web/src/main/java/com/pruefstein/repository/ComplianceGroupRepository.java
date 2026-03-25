package com.pruefstein.repository;

import com.pruefstein.model.ComplianceGroup;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceGroupRepository implements PanacheRepository<ComplianceGroup>
{
}
