package com.pruefstein.compliance.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.pruefstein.compliance.service.AppRiskAiService;
import com.pruefstein.compliance.service.BlockedAppAiService;
import com.pruefstein.compliance.service.BlockedAppSuggestion;
import com.pruefstein.compliance.service.ComplianceItemAiService;
import com.pruefstein.compliance.service.ComplianceItemSuggestion;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/ai")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("${pruefstein.security.admin-role:admin}")
public class AiSuggestResource
{
	@Inject
	ComplianceItemAiService aiService;

	@Inject
	BlockedAppAiService blockedAppAiService;

	@Inject
	AppRiskAiService appRiskAiService;

	private String schema;

	public record SuggestRequest(String description)
	{
	}

	@PostConstruct
	void loadSchema()
	{
		try (InputStream in = Thread.currentThread().getContextClassLoader()
			.getResourceAsStream("osquery/compliance-schema.txt"))
		{
			if (in == null)
			{
				throw new IllegalStateException("osquery/compliance-schema.txt not found on classpath");
			}
			schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Failed to load osquery schema", e);
		}
	}

	@POST
	@Path("/suggest-check")
	@Consumes(MediaType.APPLICATION_JSON)
	public ComplianceItemSuggestion suggest(SuggestRequest request)
	{
		if (request == null || request.description() == null || request.description().isBlank())
		{
			throw new BadRequestException("description is required");
		}
		return aiService.suggest(request.description(), schema);
	}

	public record BlockedAppRequest(String description, String knownFacts)
	{
	}

	/**
	 * Expands an application name into every identifier that recognises it —
	 * bundle IDs, Homebrew names, bundle filenames — so one rule catches the
	 * app however it was installed.
	 */
	@POST
	@Path("/suggest-blocked-app")
	@Consumes(MediaType.APPLICATION_JSON)
	public BlockedAppSuggestion suggestBlockedApp(BlockedAppRequest request)
	{
		if (request == null || request.description() == null || request.description().isBlank())
		{
			throw new InternalServerErrorException("description is required");
		}
		return blockedAppAiService.suggest(request.description(),
			request.knownFacts() == null ? "" : request.knownFacts());
	}

	/** Applications per request — the page sends the inventory in batches. */
	static final int MAX_APPS_PER_REQUEST = 300;

	private static final Set<String> RISK_CATEGORIES = Set.of("FILE_SHARING", "REMOTE_ACCESS", "OTHER");

	public record AnalyzedApp(String key, String source, String name, String identifier)
	{
	}

	public record AnalyzeAppsRequest(List<AnalyzedApp> apps)
	{
	}

	public record AppRisk(String key, String category, String reason)
	{
	}

	/**
	 * Asks the AI which of a batch of installed applications are risky — file
	 * sharing outside OneDrive, remote control — and answers by the keys the
	 * page sent. The AI only ever sees numbers, so whatever it answers can only
	 * point at an app that was in the batch.
	 */
	@POST
	@Path("/analyze-apps")
	@Consumes(MediaType.APPLICATION_JSON)
	public List<AppRisk> analyzeApps(AnalyzeAppsRequest request)
	{
		if (request == null || request.apps() == null || request.apps().isEmpty())
		{
			throw new BadRequestException("apps are required");
		}
		List<AnalyzedApp> apps = request.apps();
		if (apps.size() > MAX_APPS_PER_REQUEST)
		{
			throw new BadRequestException("at most " + MAX_APPS_PER_REQUEST + " apps per request");
		}

		StringBuilder lines = new StringBuilder();
		for (int i = 0; i < apps.size(); i++)
		{
			AnalyzedApp app = apps.get(i);
			lines.append(i + 1).append(" | ").append(oneLine(app.source()))
				.append(" | ").append(oneLine(app.name()))
				.append(" | ").append(oneLine(app.identifier())).append('\n');
		}

		return appRiskAiService.analyze(lines.toString()).riskyOrEmpty().stream()
			.filter(finding -> finding.number() != null && finding.number() >= 1 && finding.number() <= apps.size())
			.map(finding -> new AppRisk(apps.get(finding.number() - 1).key(), category(finding.category()),
				finding.reason() == null ? "" : finding.reason().strip()))
			.distinct()
			.toList();
	}

	private static String category(String raw)
	{
		String category = raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT);
		return RISK_CATEGORIES.contains(category) ? category : "OTHER";
	}

	/** Keeps one app on one line, whatever its name holds. */
	private static String oneLine(String value)
	{
		return value == null ? "" : value.replaceAll("[\\r\\n|]+", " ").strip();
	}
}
