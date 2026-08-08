package com.pruefstein.compliance.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A check that fails when any {@link BlockedApp} is installed on the device.
 * <p>
 * It deliberately stores no SQL of its own: the query is rendered from the
 * enabled blocked-app rules every time it is served, so editing the list is the
 * only way to change what this check looks for. That is why it is not editable
 * from the compliance-group screen.
 */
@Entity
@DiscriminatorValue("APP_BLACKLIST")
public class AppBlacklistCheck extends ComplianceItem
{
	@Override
	public boolean isEditable()
	{
		return false;
	}
}
