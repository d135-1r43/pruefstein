package com.pruefstein.osversion.domain;

/**
 * How a device's macOS compares to the newest release Apple had published.
 * <p>
 * Only the newest release is green. Anything short of the newest fix of its
 * train is red, whether that is a fix or a feature update; the newest fix of an
 * older train Apple still patches is amber, because nothing is left to install
 * short of the upgrade. A train Apple has dropped is red however patched it is.
 */
public enum OsVersionStanding
{
	/** At the newest release, or ahead of it on a beta. */
	CURRENT,

	/** Newest train and feature update, missing a fix. Shown in red. */
	PATCH_BEHIND,

	/** Newest train, an older feature update. Shown in red. */
	MINOR_BEHIND,

	/**
	 * An older train Apple still ships security fixes for, on its newest fix.
	 * Not the newest macOS, yet nothing left to install short of the upgrade.
	 * Shown in amber.
	 */
	OLDER_TRAIN_PATCHED,

	/**
	 * An older train Apple still ships security fixes for, but not on its
	 * newest fix — or its newest fix is not known. Shown in red, and its age is
	 * named.
	 */
	OLDER_TRAIN_UNPATCHED,

	/**
	 * A train Apple no longer ships security fixes for, or one too old to tell.
	 * Shown in red, and its age is named.
	 */
	UNSUPPORTED_TRAIN,

	/** No version was reported, or it could not be read. */
	UNKNOWN
}
