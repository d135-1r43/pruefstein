package com.pruefstein.agent.api;

import java.util.Optional;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Tells the agent which identity provider this deployment authenticates
 * against. Without it the agent would need its own copy of the issuer, client
 * id and scopes, and every one of them could drift out of step with the
 * {@code api} tenant that has to accept the resulting token. The agent knows
 * one thing — the server URL — and asks for the rest.
 * <p>
 * Public by way of {@code quarkus.http.auth.permission.public.paths}: it is the
 * same metadata the IdP publishes at its discovery endpoint, and the agent has
 * no token yet when it asks.
 */
@Path("/internal/agent-config")
@Produces(MediaType.APPLICATION_JSON)
public class AgentConfigResource
{
	public record AgentConfig(String issuer, String clientId, String scopes)
	{
	}

	/**
	 * Optional because a deployment can run with OIDC switched off entirely —
	 * the test profile does. The agent gets an empty issuer and says so rather
	 * than the server refusing to start.
	 */
	@ConfigProperty(name = "pruefstein.agent.issuer")
	Optional<String> issuer;

	@ConfigProperty(name = "pruefstein.agent.client-id")
	String clientId;

	@ConfigProperty(name = "pruefstein.agent.scopes")
	String scopes;

	@GET
	@PermitAll
	public AgentConfig get()
	{
		return new AgentConfig(issuer.orElse(""), clientId, scopes);
	}
}
