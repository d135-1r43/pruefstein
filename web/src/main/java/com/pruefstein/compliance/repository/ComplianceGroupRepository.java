package com.pruefstein.compliance.repository;

import com.pruefstein.compliance.domain.ComplianceGroup;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceGroupRepository implements PanacheRepository<ComplianceGroup>
{
	/**
	 * The group of this name, created if it does not exist yet.
	 *
	 * <p>
	 * Groups are matched by name rather than ledgered: a check being seeded or
	 * re-filed needs somewhere to live, so if its group is gone it is recreated
	 * along with it.
	 */
	public ComplianceGroup findOrCreateByName(String name)
	{
		return find("name", name).firstResultOptional()
			.orElseGet(() -> {
				ComplianceGroup group = new ComplianceGroup();
				group.setName(name);
				persist(group);
				return group;
			});
	}
}
