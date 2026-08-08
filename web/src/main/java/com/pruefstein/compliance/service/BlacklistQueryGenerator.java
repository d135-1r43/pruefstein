package com.pruefstein.compliance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.pruefstein.compliance.domain.AppMatcher;
import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.MatcherType;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Renders the osquery SQL for the app-blacklist check from the enabled
 * {@link BlockedApp} rules.
 * <p>
 * The SQL is generated on every request rather than stored, so there is no
 * denormalised copy to keep in sync when the blacklist changes. Only matching
 * rows ever travel back from the device — the full application inventory stays
 * on the endpoint.
 */
@ApplicationScoped
public class BlacklistQueryGenerator
{
	/**
	 * The expression paired with every generated query. It never changes, which
	 * is the whole point: the list is data, not a hand-edited expression.
	 */
	public static final String EXPRESSION = "results.size() == 0";

	private static final String APPS_SELECT = "SELECT 'app' AS source, name, bundle_identifier AS identifier, "
		+ "bundle_short_version AS version, path FROM apps";

	private static final String BREW_SELECT = "SELECT 'brew:' || type AS source, name, name AS identifier, "
		+ "version, path FROM homebrew_packages";

	public String generate(List<BlockedApp> blockedApps)
	{
		List<String> bundleIds = patterns(blockedApps, MatcherType.BUNDLE_ID);
		List<String> appNames = patterns(blockedApps, MatcherType.APP_NAME);
		List<String> brewNames = patterns(blockedApps, MatcherType.HOMEBREW);

		List<String> branches = new ArrayList<>();

		String appsPredicate = anyOf(predicate("bundle_identifier", bundleIds), predicate("name", appNames));
		if (appsPredicate != null)
		{
			branches.add(APPS_SELECT + " WHERE " + appsPredicate);
		}

		String brewPredicate = predicate("name", brewNames);
		if (brewPredicate != null)
		{
			branches.add(BREW_SELECT + " WHERE " + brewPredicate);
		}

		// An empty blacklist still has to yield valid SQL that returns no rows,
		// so the check passes instead of erroring on the device.
		if (branches.isEmpty())
		{
			return APPS_SELECT + " WHERE 1 = 0;";
		}
		return String.join("\nUNION ALL\n", branches) + ";";
	}

	private static List<String> patterns(List<BlockedApp> blockedApps, MatcherType type)
	{
		return blockedApps.stream()
			.flatMap(a -> a.getMatchers().stream())
			.filter(m -> m.getType() == type)
			.map(AppMatcher::getPattern)
			.filter(p -> p != null && !p.isBlank())
			.map(p -> p.strip().toLowerCase(Locale.ROOT))
			.distinct()
			.toList();
	}

	/**
	 * Exact patterns collapse into a single {@code IN (…)}; wildcard patterns
	 * each need their own {@code LIKE}.
	 */
	private static String predicate(String column, List<String> patterns)
	{
		List<String> exact = patterns.stream().filter(p -> !isWildcard(p)).toList();
		List<String> wildcards = patterns.stream().filter(BlacklistQueryGenerator::isWildcard).toList();

		List<String> parts = new ArrayList<>();
		if (!exact.isEmpty())
		{
			List<String> literals = exact.stream().map(BlacklistQueryGenerator::literal).toList();
			parts.add("LOWER(" + column + ") IN (" + String.join(", ", literals) + ")");
		}
		for (String wildcard : wildcards)
		{
			parts.add("LOWER(" + column + ") LIKE " + literal(wildcard));
		}
		return parts.isEmpty() ? null : String.join(" OR ", parts);
	}

	private static boolean isWildcard(String pattern)
	{
		return pattern.indexOf('%') >= 0 || pattern.indexOf('_') >= 0;
	}

	private static String anyOf(String left, String right)
	{
		if (left == null)
		{
			return right;
		}
		if (right == null)
		{
			return left;
		}
		return left + " OR " + right;
	}

	/**
	 * Patterns are admin-supplied and land inside SQL string literals, so a
	 * stray quote must not be able to terminate — or extend — the statement.
	 */
	private static String literal(String value)
	{
		return "'" + value.replace("'", "''") + "'";
	}
}
