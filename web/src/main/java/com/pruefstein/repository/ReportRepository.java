package com.pruefstein.repository;

import com.pruefstein.model.Report;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReportRepository implements PanacheRepository<Report>
{
}
