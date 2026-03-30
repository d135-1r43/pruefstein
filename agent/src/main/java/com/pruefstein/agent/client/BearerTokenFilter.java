package com.pruefstein.agent.client;

import com.pruefstein.agent.auth.TokenHolder;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BearerTokenFilter implements ClientRequestFilter
{
	@Inject
	TokenHolder tokenHolder;

	@Override
	public void filter(ClientRequestContext requestContext)
	{
		requestContext.getHeaders().add("Authorization", "Bearer " + tokenHolder.getAccessToken());
	}
}
