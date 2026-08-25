package com.pruefstein.agent.auth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Reads the issuer's {@code /.well-known/openid-configuration}.
 * <p>
 * The endpoint layout differs per provider — Keycloak serves
 * {@code /protocol/openid-connect/auth/device}, Entra
 * {@code /oauth2/v2.0/devicecode} — so appending a fixed suffix to the issuer
 * only ever works for the one provider it was written against. Asking is the
 * only thing that works for both.
 */
@ApplicationScoped
public class OidcDiscovery
{
	@Inject
	ObjectMapper objectMapper;

	private final HttpClient http = HttpClient.newHttpClient();

	public OidcEndpoints discover(String issuer) throws IOException, InterruptedException
	{
		String url = trimTrailingSlash(issuer) + "/.well-known/openid-configuration";

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.GET()
			.build();

		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
		{
			throw new IllegalStateException(
				"OIDC discovery failed at " + url + " (HTTP " + response.statusCode() + ")");
		}
		return parse(url, objectMapper.readTree(response.body()));
	}

	static OidcEndpoints parse(String url, JsonNode metadata)
	{
		String deviceEndpoint = text(metadata, "device_authorization_endpoint");
		String tokenEndpoint = text(metadata, "token_endpoint");

		if (deviceEndpoint == null)
		{
			throw new IllegalStateException(
				"The identity provider at " + url + " does not advertise a device_authorization_endpoint, "
					+ "so the agent cannot log in against it.");
		}
		if (tokenEndpoint == null)
		{
			throw new IllegalStateException("No token_endpoint in the discovery document at " + url);
		}
		return new OidcEndpoints(deviceEndpoint, tokenEndpoint);
	}

	private static String text(JsonNode node, String field)
	{
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}

	private static String trimTrailingSlash(String value)
	{
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
