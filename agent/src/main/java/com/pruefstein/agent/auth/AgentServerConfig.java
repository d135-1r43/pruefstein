package com.pruefstein.agent.auth;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * The IdP bootstrap a Prüfstein server hands out at
 * {@code /internal/agent-config}.
 */
@RegisterForReflection
public record AgentServerConfig(String issuer, String clientId, String scopes)
{
}
