package com.pruefstein.user.web;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Named("currentUser")
@RequestScoped
public class CurrentUserBean
{
	@Inject
	SecurityIdentity identity;

	@ConfigProperty(name = "pruefstein.security.admin-role", defaultValue = "admin")
	String adminRole;

	public boolean isLoggedIn()
	{
		return !identity.isAnonymous();
	}

	public boolean isAdmin()
	{
		return identity.hasRole(adminRole);
	}

	/**
	 * Quarkus sets no "id_token" attribute: in the code flow the ID token is
	 * the principal itself. Looking up the attribute always returned null,
	 * which is why the UI fell back to the raw principal name.
	 */
	private JsonWebToken idToken()
	{
		return identity.getPrincipal() instanceof JsonWebToken jwt ? jwt : null;
	}

	/**
	 * Identifies the signed-in user the way reports record their owner. Never
	 * null for an authenticated user: a null would read as "no owner" to the
	 * report filter, which is the opposite of what a missing claim should mean.
	 */
	public String getUsername()
	{
		if (identity.isAnonymous())
		{
			return null;
		}
		JsonWebToken idToken = idToken();
		if (idToken != null)
		{
			String username = firstOf(idToken, "preferred_username", "upn", "email");
			if (username != null)
			{
				return username;
			}
		}
		// The subject, for a token that carries no friendlier handle
		return identity.getPrincipal().getName();
	}

	private static String firstOf(JsonWebToken token, String... claims)
	{
		for (String claim : claims)
		{
			String value = token.getClaim(claim);
			if (value != null && !value.isBlank())
			{
				return value;
			}
		}
		return null;
	}

	public String getDisplayName()
	{
		if (identity.isAnonymous())
		{
			return null;
		}
		JsonWebToken idToken = idToken();
		if (idToken != null)
		{
			String name = firstOf(idToken, "name", "preferred_username", "email");
			if (name != null)
			{
				return name;
			}
		}
		return identity.getPrincipal().getName();
	}
}
