package com.pruefstein.agent.auth;

import java.time.Instant;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Everything the agent needs to talk to one server, written to
 * {@code ~/.config/pruefstein/credentials.json} by {@code agent login}.
 * <p>
 * The IdP coordinates are stored alongside the tokens rather than configured:
 * they came from the server at login time, and a refresh has to go back to the
 * same issuer that minted the refresh token.
 */
@RegisterForReflection
public record Credentials(
	String serverUrl,
	String issuer,
	String clientId,
	String scopes,
	String accessToken,
	String refreshToken,
	Instant expiresAt)
{
	public boolean isExpired()
	{
		return Instant.now().isAfter(expiresAt.minusSeconds(30));
	}

	public AgentServerConfig serverConfig()
	{
		return new AgentServerConfig(issuer, clientId, scopes);
	}
}
