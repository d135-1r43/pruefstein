package com.pruefstein.compliance.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.pruefstein.compliance.domain.AppBlacklistCheck;
import com.pruefstein.compliance.domain.AppMatcher;
import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.MatcherType;
import com.pruefstein.compliance.repository.BlockedAppRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.service.BlacklistQueryGenerator;
import com.pruefstein.compliance.service.BlockedAppAiService;
import com.pruefstein.compliance.service.BlockedAppSuggestion;
import com.pruefstein.report.api.Reports;
import com.pruefstein.report.service.FleetInventory;
import com.pruefstein.report.service.FleetInventory.FleetApp;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.LaunchMode;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed("**")
public class Apps extends Controller
{
	private static final Logger LOG = LoggerFactory.getLogger(Apps.class);

	@Inject
	BlockedAppRepository repository;

	@Inject
	BlockedAppAiService aiService;

	@Inject
	BlacklistQueryGenerator queryGenerator;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	FleetInventory fleetInventory;

	static final String TAB_ALL = "all";

	static final String TAB_BLOCKED = "blocked";

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
		}

		public static native TemplateInstance index(String tab, List<FleetApp> apps, List<BlockedApp> blockedApps,
			String checkName, String generatedQuery, String generatedExpression, boolean devMode);
	}

	/**
	 * Two tabs: every application installed anywhere, to screen, and the ones
	 * blocked so far. All apps first, since that is where blocking starts.
	 */
	public TemplateInstance index(@RestQuery String tab)
	{
		String current = TAB_BLOCKED.equals(tab) ? TAB_BLOCKED : TAB_ALL;
		String checkName = itemRepository.findActiveBlacklistCheck()
			.map(AppBlacklistCheck::getName)
			.orElse(null);
		boolean devMode = LaunchMode.current() == LaunchMode.DEVELOPMENT;
		return Templates.index(current, fleetInventory.list(), repository.listAllSorted(), checkName,
			queryGenerator.generate(repository.listEnabled()), BlacklistQueryGenerator.EXPRESSION, devMode);
	}

	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
	public void create(
		@RestForm @NotBlank String label,
		@RestForm String reason,
		@RestForm String bundleIds,
		@RestForm String appNames,
		@RestForm String homebrewNames)
	{
		if (validationFailed())
		{
			index(TAB_BLOCKED);
			return;
		}
		BlockedApp app = new BlockedApp();
		app.setLabel(label.strip());
		app.setReason(blankToNull(reason));
		app.setEnabled(true);
		app.setMatchers(parseMatchers(bundleIds, appNames, homebrewNames));
		repository.persist(app);
		index(TAB_BLOCKED);
	}

	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
	public void update(
		@RestForm Long id,
		@RestForm @NotBlank String label,
		@RestForm String reason,
		@RestForm String bundleIds,
		@RestForm String appNames,
		@RestForm String homebrewNames,
		@RestForm Boolean enabled)
	{
		if (validationFailed())
		{
			index(TAB_BLOCKED);
			return;
		}
		BlockedApp app = repository.findById(id);
		if (app == null)
		{
			notFound();
			return;
		}
		app.setLabel(label.strip());
		app.setReason(blankToNull(reason));
		app.setEnabled(enabled != null && enabled);
		app.getMatchers().clear();
		app.getMatchers().addAll(parseMatchers(bundleIds, appNames, homebrewNames));
		index(TAB_BLOCKED);
	}

	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
	public void delete(@RestForm Long id)
	{
		repository.deleteById(id);
		index(TAB_BLOCKED);
	}

	/**
	 * One-click block straight from an installed-app list — a report's, or the
	 * fleet's on the All Apps tab. The matcher is derived from how the app was
	 * installed — a Homebrew name for packages, the bundle identifier for
	 * application bundles — and the admin is returned to the list they came
	 * from.
	 */
	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
	public void blockFromReport(
		@RestForm @NotBlank String label,
		@RestForm @NotBlank String matcherType,
		@RestForm @NotBlank String pattern,
		@RestForm String reason,
		@RestForm Long reportId,
		@RestForm Boolean useAi)
	{
		if (validationFailed())
		{
			index(TAB_ALL);
			return;
		}
		BlockedApp app = new BlockedApp();
		app.setLabel(label.strip());
		app.setReason(blankToNull(reason));
		app.setEnabled(true);
		app.setMatchers(new ArrayList<>(
			List.of(new AppMatcher(MatcherType.valueOf(matcherType), pattern.strip()))));

		if (useAi != null && useAi)
		{
			expandWithAi(app, label.strip(), matcherType, pattern.strip());
		}
		repository.persist(app);

		// Without a report it came from the fleet list, which is where to
		// return
		if (reportId == null)
		{
			index(TAB_ALL);
			return;
		}
		redirect(Reports.class).show(reportId);
	}

	/**
	 * Adds the other install routes the AI knows about, keeping the matcher the
	 * inventory row gave us. The observed identifier is trusted; the AI's
	 * additions are a superset around it, so a failed or empty suggestion still
	 * leaves a working rule.
	 */
	private void expandWithAi(BlockedApp app, String label, String matcherType, String observedPattern)
	{
		try
		{
			String knownFacts = "Observed on a device as " + matcherType + " = " + observedPattern;
			BlockedAppSuggestion suggestion = aiService.suggest(label, knownFacts);

			if (app.getReason() == null && suggestion.reason() != null && !suggestion.reason().isBlank())
			{
				app.setReason(suggestion.reason().strip());
			}
			addSuggested(app, MatcherType.BUNDLE_ID, suggestion.bundleIdsOrEmpty());
			addSuggested(app, MatcherType.HOMEBREW, suggestion.homebrewNamesOrEmpty());
			addSuggested(app, MatcherType.APP_NAME, suggestion.appNamesOrEmpty());
		}
		catch (Exception e)
		{
			LOG.warn("AI expansion skipped for '{}'.", label, e);
		}
	}

	private static void addSuggested(BlockedApp app, MatcherType type, List<String> patterns)
	{
		for (String pattern : patterns)
		{
			if (pattern == null || pattern.isBlank())
			{
				continue;
			}
			AppMatcher matcher = new AppMatcher(type, pattern.strip());
			if (!app.getMatchers().contains(matcher))
			{
				app.getMatchers().add(matcher);
			}
		}
	}

	private static List<AppMatcher> parseMatchers(String bundleIds, String appNames, String homebrewNames)
	{
		List<AppMatcher> matchers = new ArrayList<>();
		addAll(matchers, MatcherType.BUNDLE_ID, bundleIds);
		addAll(matchers, MatcherType.APP_NAME, appNames);
		addAll(matchers, MatcherType.HOMEBREW, homebrewNames);
		return matchers;
	}

	/**
	 * Patterns arrive one per line (commas also accepted) — a plain textarea is
	 * easier to manage than a dynamic row editor and pastes well from a policy
	 * document.
	 */
	private static void addAll(List<AppMatcher> matchers, MatcherType type, String raw)
	{
		if (raw == null || raw.isBlank())
		{
			return;
		}
		Arrays.stream(raw.split("[\\r\\n,]+"))
			.map(String::strip)
			.filter(s -> !s.isEmpty())
			.distinct()
			.forEach(pattern -> matchers.add(new AppMatcher(type, pattern)));
	}

	private static String blankToNull(String value)
	{
		return value == null || value.isBlank() ? null : value.strip();
	}
}
