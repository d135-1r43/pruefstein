package com.pruefstein.agent.command;

import com.pruefstein.agent.auth.AuthResolver;
import com.pruefstein.agent.runner.ComplianceRunner;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "Run all compliance checks and push results to the server", mixinStandardHelpOptions = true)
public class RunCommand implements Runnable
{
	@Inject
	AuthResolver authResolver;

	@Inject
	ComplianceRunner runner;

	@Override
	public void run()
	{
		try
		{
			authResolver.ensureAuthenticated();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
		}
		runner.runAll();
	}
}
