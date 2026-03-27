package com.pruefstein.agent.command;

import com.pruefstein.agent.runner.ComplianceRunner;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "Run all compliance checks and push results to the server", mixinStandardHelpOptions = true)
public class RunCommand implements Runnable
{

	@Inject
	ComplianceRunner runner;

	@Override
	public void run()
	{
		runner.runAll();
	}
}
