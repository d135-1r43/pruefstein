package com.pruefstein.compliance.service;

import java.util.List;

import com.pruefstein.compliance.domain.AppMatcher;
import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.MatcherType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlacklistQueryGeneratorTest
{
	private final BlacklistQueryGenerator generator = new BlacklistQueryGenerator();

	private static BlockedApp app(String label, AppMatcher... matchers)
	{
		BlockedApp app = new BlockedApp();
		app.setLabel(label);
		app.setMatchers(List.of(matchers));
		return app;
	}

	@Test
	void emptyBlacklistYieldsValidQueryThatMatchesNothing()
	{
		String sql = generator.generate(List.of());

		assertTrue(sql.contains("FROM apps"));
		assertTrue(sql.contains("WHERE 1 = 0"));
		assertFalse(sql.contains("UNION"));
	}

	@Test
	void bundleIdsAndHomebrewNamesProduceBothBranches()
	{
		String sql = generator.generate(List.of(app("Nextcloud",
			new AppMatcher(MatcherType.BUNDLE_ID, "com.nextcloud.desktopclient.nextcloud"),
			new AppMatcher(MatcherType.HOMEBREW, "nextcloud"))));

		assertTrue(sql.contains("FROM apps"));
		assertTrue(sql.contains("FROM homebrew_packages"));
		assertTrue(sql.contains("UNION ALL"));
		assertTrue(sql.contains("LOWER(bundle_identifier) IN ('com.nextcloud.desktopclient.nextcloud')"));
		assertTrue(sql.contains("LOWER(name) IN ('nextcloud')"));
	}

	@Test
	void onlyHomebrewMatchersSkipTheAppsBranch()
	{
		String sql = generator.generate(List.of(app("wget", new AppMatcher(MatcherType.HOMEBREW, "wget"))));

		assertTrue(sql.contains("FROM homebrew_packages"));
		assertFalse(sql.contains("FROM apps"));
		assertFalse(sql.contains("UNION"));
	}

	@Test
	void exactPatternsCollapseIntoOneInClause()
	{
		String sql = generator.generate(List.of(
			app("A", new AppMatcher(MatcherType.HOMEBREW, "transmission")),
			app("B", new AppMatcher(MatcherType.HOMEBREW, "qbittorrent"))));

		assertTrue(sql.contains("LOWER(name) IN ('transmission', 'qbittorrent')"));
	}

	@Test
	void wildcardPatternBecomesLike()
	{
		String sql = generator.generate(List.of(app("TeamViewer",
			new AppMatcher(MatcherType.BUNDLE_ID, "com.teamviewer.%"))));

		assertTrue(sql.contains("LOWER(bundle_identifier) LIKE 'com.teamviewer.%'"));
		assertFalse(sql.contains(" IN ("));
	}

	@Test
	void patternsAreLowercasedSoMatchingIsCaseInsensitive()
	{
		String sql = generator.generate(List.of(app("Nextcloud",
			new AppMatcher(MatcherType.APP_NAME, "NextCloud.app"))));

		assertTrue(sql.contains("LOWER(name) IN ('nextcloud.app')"));
	}

	@Test
	void quotesInPatternsAreEscapedRatherThanTerminatingTheLiteral()
	{
		String sql = generator.generate(List.of(app("Injection",
			new AppMatcher(MatcherType.APP_NAME, "x'); DROP TABLE apps;--"))));

		// The quote is doubled, so the whole payload stays one string literal
		assertTrue(sql.contains("'x''); drop table apps;--'"));
		assertFalse(sql.contains("'x');"));
	}

	@Test
	void blankAndDuplicatePatternsAreDropped()
	{
		String sql = generator.generate(List.of(
			app("A", new AppMatcher(MatcherType.HOMEBREW, "nextcloud"),
				new AppMatcher(MatcherType.HOMEBREW, "  "),
				new AppMatcher(MatcherType.HOMEBREW, " nextcloud ")),
			app("B", new AppMatcher(MatcherType.HOMEBREW, "NEXTCLOUD"))));

		assertTrue(sql.contains("LOWER(name) IN ('nextcloud')"));
	}

	@Test
	void disabledRulesAreTheCallersConcernNotTheGenerators()
	{
		// The generator renders whatever it is handed; filtering happens in the
		// repository, so an explicitly empty list must still be safe.
		assertDoesNotThrow(() -> generator.generate(List.of()));
	}
}
