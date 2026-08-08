package com.pruefstein.compliance.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * One recognition pattern belonging to a {@link BlockedApp}. The pattern is
 * matched case-insensitively and supports SQL {@code LIKE} wildcards: {@code %}
 * for any sequence and {@code _} for a single character.
 */
@Embeddable
public class AppMatcher
{
	@Enumerated(EnumType.STRING)
	@Column(name = "matcher_type")
	private MatcherType type;

	@Column(name = "matcher_pattern")
	private String pattern;

	public AppMatcher()
	{
	}

	public AppMatcher(MatcherType type, String pattern)
	{
		this.type = type;
		this.pattern = pattern;
	}

	public MatcherType getType()
	{
		return type;
	}

	public void setType(MatcherType type)
	{
		this.type = type;
	}

	public String getPattern()
	{
		return pattern;
	}

	public void setPattern(String pattern)
	{
		this.pattern = pattern;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof AppMatcher other))
		{
			return false;
		}
		return type == other.type && Objects.equals(pattern, other.pattern);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(type, pattern);
	}
}
