package com.pruefstein.agent.client;

import io.quarkus.oidc.client.OidcClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BearerTokenFilter implements ClientRequestFilter
{
	@Inject
	OidcClient oidcClient;

	@Override
	public void filter(ClientRequestContext requestContext)
	{
		String token = oidcClient.getTokens().await().indefinitely().getAccessToken();
		requestContext.getHeaders().add("Authorization", "Bearer " + token);
	}
}
