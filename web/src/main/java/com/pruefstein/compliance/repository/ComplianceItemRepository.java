package com.pruefstein.compliance.repository;

import java.util.List;

import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ExpressionCheck;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplianceItemRepository implements PanacheRepository<ComplianceItem>
{
	/**
	 * Every authored check whose SQL is exactly {@code query}.
	 *
	 * <p>
	 * Typed against {@link ExpressionCheck} rather than this repository's own
	 * entity because the column belongs to the subclass: the hierarchy shares
	 * one table, so HQL has to name {@code ExpressionCheck} to see it at all.
	 *
	 * @param query
	 *            matched byte for byte — this exists to recognise SQL a release
	 *            shipped, not to search for checks
	 */
	public List<ExpressionCheck> findByQuery(String query)
	{
		return getEntityManager()
			.createQuery("FROM ExpressionCheck WHERE query = :query", ExpressionCheck.class)
			.setParameter("query", query)
			.getResultList();
	}
}
