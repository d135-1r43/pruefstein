package com.pruefstein.agent.auth;

import java.time.Instant;

public record StoredToken(String accessToken, String refreshToken, Instant expiresAt)
{
	public boolean isExpired()
	{
		return Instant.now().isAfter(expiresAt.minusSeconds(30));
	}
}
