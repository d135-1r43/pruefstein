package com.pruefstein.report.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReportRepository implements PanacheRepository<Report>
{
	public Optional<Report> findOpenByDeviceAndUser(String deviceId, String userId)
	{
		return find("deviceId = ?1 and userId = ?2 and status = ?3",
			deviceId, userId, ReportStatus.OPEN).firstResultOptional();
	}

	public List<Report> findExpiredOpen(Instant now)
	{
		return list("status = ?1 and deadline < ?2 and flowInstanceId is not null",
			ReportStatus.OPEN, now);
	}

	public List<Report> listFiltered(ReportStatus status, String q, String sort, String dir)
	{
		return listFiltered(status, q, sort, dir, null);
	}

	public List<Report> listFiltered(ReportStatus status, String q, String sort, String dir, String keycloakUser)
	{
		StringBuilder query = new StringBuilder();
		List<Object> params = new ArrayList<>();
		int p = 1;

		if (status != null)
		{
			query.append("status = ?").append(p++);
			params.add(status);
		}

		if (q != null && !q.isBlank())
		{
			if (!query.isEmpty()) query.append(" and ");
			String like = "%" + q.toLowerCase() + "%";
			query.append("(lower(deviceId) like ?").append(p)
				.append(" or lower(userId) like ?").append(p)
				.append(" or lower(keycloakUser) like ?").append(p).append(")");
			params.add(like);
			p++;
		}

		if (keycloakUser != null)
		{
			if (!query.isEmpty()) query.append(" and ");
			query.append("keycloakUser = ?").append(p++);
			params.add(keycloakUser);
		}

		Sort panacheSort = buildSort(sort, dir);

		if (query.isEmpty())
		{
			return listAll(panacheSort);
		}
		return list(query.toString(), panacheSort, params.toArray());
	}

	private Sort buildSort(String col, String dir)
	{
		String column = switch (col != null ? col : "")
		{
			case "status" -> "status";
			case "deviceId" -> "deviceId";
			case "user" -> "keycloakUser";
			default -> "checkedAt";
		};
		boolean desc = !"asc".equals(dir);
		return desc ? Sort.by(column).descending() : Sort.by(column).ascending();
	}
}
