package com.pruefstein.agent.auth;

/**
 * The two endpoints the device authorization grant needs, as published by the
 * issuer's discovery document.
 */
public record OidcEndpoints(String deviceAuthorizationEndpoint, String tokenEndpoint)
{
}
