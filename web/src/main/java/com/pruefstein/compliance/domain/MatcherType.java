package com.pruefstein.compliance.domain;

/**
 * How a {@link BlockedApp} rule recognises an installed application. One rule
 * usually carries several matchers, because the same application can arrive by
 * more than one route — Nextcloud, for example, is both a Homebrew cask and a
 * traditional {@code .app} bundle.
 */
public enum MatcherType
{
	/** Matched against {@code apps.bundle_identifier} — the reliable one. */
	BUNDLE_ID,

	/** Matched against {@code apps.name}. Fragile; use as a fallback. */
	APP_NAME,

	/** Matched against {@code homebrew_packages.name} (formula or cask). */
	HOMEBREW
}
