package com.pruefstein.agent.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuthResolver
{
	private static final Logger LOG = LoggerFactory.getLogger(AuthResolver.class);

	@Inject
	TokenStore tokenStore;

	@Inject
	DeviceAuthService deviceAuthService;

	@Inject
	TokenHolder tokenHolder;

	/**
	 * Ensures a valid access token is loaded into TokenHolder.
	 * Uses cached token, refreshes if expired, or triggers device auth login if none.
	 */
	public void ensureAuthenticated() throws IOException, InterruptedException
	{
		Optional<StoredToken> stored = tokenStore.load();

		if (stored.isPresent())
		{
			StoredToken token = stored.get();
			if (!token.isExpired())
			{
				tokenHolder.setAccessToken(token.accessToken());
				return;
			}

			if (token.refreshToken() != null)
			{
				try
				{
					StoredToken refreshed = deviceAuthService.refresh(token.refreshToken());
					tokenStore.save(refreshed);
					tokenHolder.setAccessToken(refreshed.accessToken());
					return;
				}
				catch (Exception e)
				{
					LOG.debug("Token refresh failed, re-authenticating: " + e.getMessage());
				}
			}
		}

		StoredToken fresh = deviceAuthService.login();
		tokenStore.save(fresh);
		tokenHolder.setAccessToken(fresh.accessToken());
	}
}
