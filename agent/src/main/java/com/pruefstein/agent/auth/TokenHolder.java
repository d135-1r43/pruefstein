package com.pruefstein.agent.auth;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenHolder
{
	private volatile String accessToken;

	public String getAccessToken()
	{
		return accessToken;
	}

	public void setAccessToken(String accessToken)
	{
		this.accessToken = accessToken;
	}
}
