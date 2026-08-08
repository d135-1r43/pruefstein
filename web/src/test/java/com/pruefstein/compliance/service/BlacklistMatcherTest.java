package com.pruefstein.compliance.service;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruefstein.compliance.domain.AppMatcher;
import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.MatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlacklistMatcherTest
{
	private BlacklistMatcher matcher;

	@BeforeEach
	void setUp()
	{
		matcher = new BlacklistMatcher();
		matcher.objectMapper = new ObjectMapper();
	}

	private static BlockedApp app(String label, String reason, AppMatcher... matchers)
	{
		BlockedApp app = new BlockedApp();
		app.setLabel(label);
		app.setReason(reason);
		app.setMatchers(List.of(matchers));
		return app;
	}

	private static final BlockedApp NEXTCLOUD = app("Nextcloud Desktop", "Data must stay in M365",
		new AppMatcher(MatcherType.BUNDLE_ID, "com.nextcloud.desktopclient.nextcloud"),
		new AppMatcher(MatcherType.HOMEBREW, "nextcloud"));

	@Test
	void matchesBundleIdentifierOnAppRows()
	{
		String output = """
			[{"source":"app","name":"Nextcloud.app","identifier":"com.nextcloud.desktopclient.nextcloud",
			  "version":"3.13.0","path":"/Applications/Nextcloud.app"}]
			""";

		assertEquals(List.of(NEXTCLOUD), matcher.match(output, List.of(NEXTCLOUD)));
	}

	@Test
	void matchesHomebrewNameOnBrewRows()
	{
		String output = """
			[{"source":"brew:cask","name":"nextcloud","identifier":"nextcloud",
			  "version":"3.13.0","path":"/opt/homebrew/Caskroom/nextcloud"}]
			""";

		assertEquals(List.of(NEXTCLOUD), matcher.match(output, List.of(NEXTCLOUD)));
	}

	@Test
	void theSameRuleIsReportedOnceEvenWhenBothInstallRoutesAreFound()
	{
		String output = """
			[{"source":"app","name":"Nextcloud.app","identifier":"com.nextcloud.desktopclient.nextcloud",
			  "version":"3.13.0","path":"/Applications/Nextcloud.app"},
			 {"source":"brew:cask","name":"nextcloud","identifier":"nextcloud",
			  "version":"3.13.0","path":"/opt/homebrew/Caskroom/nextcloud"}]
			""";

		assertEquals(1, matcher.match(output, List.of(NEXTCLOUD)).size());
	}

	@Test
	void homebrewMatcherDoesNotMatchAnAppRowOfTheSameName()
	{
		BlockedApp brewOnly = app("wget", "CLI", new AppMatcher(MatcherType.HOMEBREW, "wget"));
		String output = """
			[{"source":"app","name":"wget","identifier":"com.example.wget","version":"1","path":"/Applications/wget.app"}]
			""";

		assertTrue(matcher.match(output, List.of(brewOnly)).isEmpty());
	}

	@Test
	void matchingIsCaseInsensitive()
	{
		String output = """
			[{"source":"app","name":"NextCloud.app","identifier":"COM.NEXTCLOUD.DESKTOPCLIENT.NEXTCLOUD",
			  "version":"3","path":"/Applications/NextCloud.app"}]
			""";

		assertEquals(List.of(NEXTCLOUD), matcher.match(output, List.of(NEXTCLOUD)));
	}

	@Test
	void percentWildcardMatchesLikeTheGeneratedSqlWould()
	{
		BlockedApp teamViewer = app("TeamViewer", "Unmanaged remote access",
			new AppMatcher(MatcherType.BUNDLE_ID, "com.teamviewer.%"));
		String output = """
			[{"source":"app","name":"TeamViewer.app","identifier":"com.teamviewer.TeamViewer",
			  "version":"15","path":"/Applications/TeamViewer.app"}]
			""";

		assertEquals(List.of(teamViewer), matcher.match(output, List.of(teamViewer)));
	}

	@Test
	void wildcardIsAnchoredSoItDoesNotMatchASuffixByAccident()
	{
		BlockedApp rule = app("Prefixed", "r", new AppMatcher(MatcherType.BUNDLE_ID, "com.evil.%"));
		String output = """
			[{"source":"app","name":"X.app","identifier":"org.good.com.evil.thing","version":"1","path":"/X"}]
			""";

		assertTrue(matcher.match(output, List.of(rule)).isEmpty());
	}

	@Test
	void emptyOutputAndMalformedJsonYieldNoMatchesRatherThanThrowing()
	{
		assertTrue(matcher.match("[]", List.of(NEXTCLOUD)).isEmpty());
		assertTrue(matcher.match("", List.of(NEXTCLOUD)).isEmpty());
		assertTrue(matcher.match(null, List.of(NEXTCLOUD)).isEmpty());
		assertTrue(matcher.match("not json", List.of(NEXTCLOUD)).isEmpty());
	}

	@Test
	void reasonsRenderLabelAndPolicyTextForThePrompt()
	{
		String reasons = matcher.reasons(List.of(NEXTCLOUD));

		assertTrue(reasons.contains("Nextcloud Desktop"));
		assertTrue(reasons.contains("Data must stay in M365"));
	}

	@Test
	void reasonsFallBackWhenNoPolicyTextWasEntered()
	{
		BlockedApp noReason = app("Some App", null, new AppMatcher(MatcherType.HOMEBREW, "x"));

		assertTrue(matcher.reasons(List.of(noReason)).contains("not approved"));
	}
}
