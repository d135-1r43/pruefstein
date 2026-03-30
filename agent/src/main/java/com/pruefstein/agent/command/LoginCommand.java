package com.pruefstein.agent.command;

import com.pruefstein.agent.auth.AuthResolver;

import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "login", description = "Authenticate with the Prüfstein server", mixinStandardHelpOptions = true)
public class LoginCommand implements Runnable
{
	@Inject
	AuthResolver authResolver;

	@Override
	public void run()
	{
		try
		{
			authResolver.ensureAuthenticated();
			System.out.println("Login successful. Credentials cached for future runs.");
		}
		catch (Exception e)
		{
			throw new RuntimeException("Login failed: " + e.getMessage(), e);
		}
	}
}
