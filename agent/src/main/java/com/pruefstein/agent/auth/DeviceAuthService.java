package com.pruefstein.agent.auth;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DeviceAuthService
{
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>()
	{
	};

	@ConfigProperty(name = "pruefstein.agent.auth-server-url")
	String authServerUrl;

	@ConfigProperty(name = "pruefstein.agent.client-id")
	String clientId;

	@Inject
	ObjectMapper objectMapper;

	private final HttpClient http = HttpClient.newHttpClient();

	public StoredToken login() throws IOException, InterruptedException
	{
		String deviceEndpoint = authServerUrl + "/protocol/openid-connect/auth/device";
		String body = "client_id=" + encode(clientId);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(deviceEndpoint))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		Map<String, Object> device = objectMapper.readValue(response.body(), MAP_TYPE);

		String userCode = (String) device.get("user_code");
		String verificationUri = (String) device.get("verification_uri_complete");
		if (verificationUri == null)
		{
			verificationUri = (String) device.get("verification_uri");
		}
		String deviceCode = (String) device.get("device_code");
		int interval = ((Number) device.getOrDefault("interval", 5)).intValue();

		System.out.println();
		System.out.println("  Please open this URL to log in:");
		System.out.println("  " + verificationUri);
		System.out.println("  Code: " + userCode);
		System.out.println();
		System.out.print("  Waiting for authentication");

		return pollForToken(deviceCode, interval);
	}

	private StoredToken pollForToken(String deviceCode, int intervalSeconds)
		throws IOException, InterruptedException
	{
		String tokenEndpoint = authServerUrl + "/protocol/openid-connect/token";
		String body = "client_id=" + encode(clientId)
			+ "&grant_type=urn:ietf:params:oauth:grant-type:device_code"
			+ "&device_code=" + encode(deviceCode);

		while (true)
		{
			Thread.sleep(intervalSeconds * 1000L);
			System.out.print(".");

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(tokenEndpoint))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			Map<String, Object> result = objectMapper.readValue(response.body(), MAP_TYPE);

			if (result.containsKey("access_token"))
			{
				System.out.println(" done.");
				return toStoredToken(result);
			}

			String error = (String) result.get("error");
			if (!"authorization_pending".equals(error) && !"slow_down".equals(error))
			{
				throw new RuntimeException("Authentication failed: " + error);
			}
			if ("slow_down".equals(error))
			{
				intervalSeconds += 5;
			}
		}
	}

	public StoredToken refresh(String refreshToken) throws IOException, InterruptedException
	{
		String tokenEndpoint = authServerUrl + "/protocol/openid-connect/token";
		String body = "client_id=" + encode(clientId)
			+ "&grant_type=refresh_token"
			+ "&refresh_token=" + encode(refreshToken);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(tokenEndpoint))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		Map<String, Object> result = objectMapper.readValue(response.body(), MAP_TYPE);

		if (!result.containsKey("access_token"))
		{
			throw new RuntimeException("Token refresh failed: " + result.get("error"));
		}
		return toStoredToken(result);
	}

	private StoredToken toStoredToken(Map<String, Object> tokenResponse)
	{
		String accessToken = (String) tokenResponse.get("access_token");
		String refreshToken = (String) tokenResponse.get("refresh_token");
		int expiresIn = ((Number) tokenResponse.getOrDefault("expires_in", 300)).intValue();
		Instant expiresAt = Instant.now().plusSeconds(expiresIn);
		return new StoredToken(accessToken, refreshToken, expiresAt);
	}

	private static String encode(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
