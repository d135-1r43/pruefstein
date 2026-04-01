package com.pruefstein.compliance.repository;

import com.pruefstein.compliance.domain.ComplianceItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceItemRepository implements PanacheRepository<ComplianceItem>
{
}
