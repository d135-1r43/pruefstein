package com.pruefstein.agent.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
	ServerConfigClient serverConfigClient;

	@Inject
	TokenHolder tokenHolder;

	/**
	 * Only a fallback for a first login with no {@code --server}: once
	 * credentials exist, the stored server URL feeds this same property through
	 * {@code CredentialsConfigSource}.
	 */
	@ConfigProperty(name = "quarkus.rest-client.pruefstein-api.url")
	String configuredServerUrl;

	/**
	 * Ensures a valid access token is loaded into TokenHolder, using the cached
	 * one, refreshing it, or falling back to an interactive device login.
	 */
	public void ensureAuthenticated() throws IOException, InterruptedException
	{
		ensureAuthenticated(null);
	}

	/**
	 * @param serverOverride the server named on the command line, or
	 *        {@code null} to stay with the one already logged in to. Naming a
	 *        server discards any cached token: it was minted by whichever IdP
	 *        the previous server uses and means nothing to this one.
	 */
	public void ensureAuthenticated(String serverOverride) throws IOException, InterruptedException
	{
		Optional<Credentials> stored = tokenStore.load();

		if (serverOverride == null && stored.isPresent())
		{
			Credentials credentials = stored.get();
			if (!credentials.isExpired())
			{
				tokenHolder.setAccessToken(credentials.accessToken());
				return;
			}

			if (credentials.refreshToken() != null)
			{
				try
				{
					Credentials refreshed = deviceAuthService.refresh(credentials);
					tokenStore.save(refreshed);
					tokenHolder.setAccessToken(refreshed.accessToken());
					return;
				}
				catch (Exception e)
				{
					LOG.debug("Token refresh failed, re-authenticating. ", e);
				}
			}
		}

		String serverUrl = resolveServerUrl(serverOverride, stored);
		AgentServerConfig config = serverConfigClient.fetch(serverUrl);

		Credentials fresh = deviceAuthService.login(serverUrl, config);
		tokenStore.save(fresh);
		tokenHolder.setAccessToken(fresh.accessToken());
	}

	private String resolveServerUrl(String serverOverride, Optional<Credentials> stored)
	{
		if (serverOverride != null && !serverOverride.isBlank())
		{
			return serverOverride;
		}
		return stored.map(Credentials::serverUrl)
			.filter(url -> url != null && !url.isBlank())
			.orElse(configuredServerUrl);
	}
}
