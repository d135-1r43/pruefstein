package com.pruefstein.compliance.domain;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

/**
 * An application that must not be installed on a managed device.
 * <p>
 * One rule describes one <em>application</em>, not one pattern, and carries as
 * many {@link AppMatcher}s as it needs to recognise that application however it
 * was installed. That keeps a single row — and a single reason — in front of
 * the admin even when the app ships as a Homebrew cask and an {@code .app}
 * bundle at once.
 */
@Entity
public class BlockedApp extends PanacheEntity
{
	private String label;

	/**
	 * Why this application is not permitted. Doubles as the audit trail for
	 * A.12.6.2 and as context for the generated remediation tip.
	 */
	@Column(columnDefinition = "TEXT")
	private String reason;

	private boolean enabled = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "BlockedApp_matchers", joinColumns = @JoinColumn(name = "blocked_app_id"))
	private List<AppMatcher> matchers = new ArrayList<>();

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getReason()
	{
		return reason;
	}

	public void setReason(String reason)
	{
		this.reason = reason;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public List<AppMatcher> getMatchers()
	{
		return matchers;
	}

	public void setMatchers(List<AppMatcher> matchers)
	{
		this.matchers = matchers;
	}

	public List<AppMatcher> matchersOf(MatcherType type)
	{
		return matchers.stream().filter(m -> m.getType() == type).toList();
	}

	// ── View helpers: patterns as newline-separated text, matching the
	// textarea-per-type shape of the edit form
	// ────────────────────────────────────────

	public String getBundleIdPatterns()
	{
		return joined(MatcherType.BUNDLE_ID);
	}

	public String getAppNamePatterns()
	{
		return joined(MatcherType.APP_NAME);
	}

	public String getHomebrewPatterns()
	{
		return joined(MatcherType.HOMEBREW);
	}

	public boolean isIncomplete()
	{
		return matchers.isEmpty();
	}

	private String joined(MatcherType type)
	{
		return String.join("\n", matchersOf(type).stream().map(AppMatcher::getPattern).toList());
	}
}
