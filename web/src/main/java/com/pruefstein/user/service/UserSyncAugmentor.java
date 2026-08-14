package com.pruefstein.user.service;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserSyncAugmentor implements SecurityIdentityAugmentor
{
	@Inject
	UserSyncService userSyncService;

	@Override
	public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext ctx)
	{
		// Quarkus sets no "id_token" attribute: in the code flow the ID token is
		// the principal itself. The old lookup was always null, so no user was
		// ever synced and the UI fell back to showing the raw principal name.
		org.eclipse.microprofile.jwt.JsonWebToken idToken =
			identity.getPrincipal() instanceof org.eclipse.microprofile.jwt.JsonWebToken jwt ? jwt : null;
		if (idToken == null || identity.isAnonymous())
		{
			return Uni.createFrom().item(identity);
		}

		return ctx.runBlocking(() -> {
			userSyncService.syncUser(
				idToken.getSubject(),
				idToken.getClaim("email"),
				idToken.getClaim("given_name"),
				idToken.getClaim("family_name"));
			return identity;
		});
	}
}
