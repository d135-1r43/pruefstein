package com.pruefstein.user.web;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
	 * Quarkus sets no "id_token" attribute: in the code flow the ID token is the
	 * principal itself. Looking up the attribute always returned null, which is
	 * why the UI fell back to the raw principal name.
	 */
	private org.eclipse.microprofile.jwt.JsonWebToken idToken()
	{
		return identity.getPrincipal() instanceof org.eclipse.microprofile.jwt.JsonWebToken jwt ? jwt : null;
	}

	public String getUsername()
	{
		if (identity.isAnonymous())
		{
			return null;
		}
		org.eclipse.microprofile.jwt.JsonWebToken idToken = idToken();
		if (idToken != null)
		{
			return idToken.getClaim("preferred_username");
		}
		return identity.getPrincipal().getName();
	}

	public String getDisplayName()
	{
		if (identity.isAnonymous())
		{
			return null;
		}
		org.eclipse.microprofile.jwt.JsonWebToken idToken = idToken();
		if (idToken != null)
		{
			String username = idToken.getClaim("preferred_username");
			if (username != null)
			{
				return username;
			}
			String email = idToken.getClaim("email");
			if (email != null)
			{
				return email;
			}
		}
		return identity.getPrincipal().getName();
	}
}
