package com.pruefstein.repository;

import com.pruefstein.model.ComplianceItem;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceItemRepository implements PanacheRepository<ComplianceItem>
{
}
