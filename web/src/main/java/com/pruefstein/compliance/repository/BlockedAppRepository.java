package com.pruefstein.compliance.repository;

import java.util.List;

import com.pruefstein.compliance.domain.BlockedApp;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BlockedAppRepository implements PanacheRepository<BlockedApp>
{
	public List<BlockedApp> listEnabled()
	{
		return list("enabled", Sort.by("label"), true);
	}

	public List<BlockedApp> listAllSorted()
	{
		return listAll(Sort.by("label"));
	}
}
