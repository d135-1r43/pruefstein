package com.pruefstein.agent.command;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.context.ApplicationScoped;
import picocli.CommandLine;

@TopCommand
@ApplicationScoped
@CommandLine.Command(
	name = "agent",
	description = "Prüfstein compliance agent",
	subcommands = {LoginCommand.class, LogoutCommand.class, RunCommand.class},
	mixinStandardHelpOptions = true)
public class MainCommand implements Runnable
{
	@CommandLine.Spec
	CommandLine.Model.CommandSpec spec;

	@Override
	public void run()
	{
		spec.commandLine().usage(System.out);
	}
}
