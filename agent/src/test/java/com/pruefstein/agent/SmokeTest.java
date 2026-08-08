package com.pruefstein.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import com.pruefstein.agent.auth.StoredToken;
import com.pruefstein.agent.auth.TokenStore;
import com.pruefstein.agent.command.LogoutCommand;
import com.pruefstein.agent.command.MainCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SmokeTest
{
	@Inject
	@TopCommand
	Object topCommand;

	@Inject
	LogoutCommand logoutCommand;

	@Inject
	TokenStore tokenStore;

	@Test
	void applicationStartsAndTopCommandResolves()
	{
		assertNotNull(topCommand);
		assertInstanceOf(MainCommand.class, topCommand);
	}

	@Test
	void helpOutputContainsSubcommands()
	{
		CommandLine cmd = new CommandLine(topCommand);
		String help = cmd.getUsageMessage();
		assertTrue(help.contains("login"), "help should list 'login' subcommand");
		assertTrue(help.contains("logout"), "help should list 'logout' subcommand");
		assertTrue(help.contains("run"), "help should list 'run' subcommand");
	}

	@Test
	void logoutClearsCredentials() throws Exception
	{
		Path credFile = Path.of(System.getProperty("user.home"), ".config", "pruefstein", "credentials.json");
		tokenStore.save(new StoredToken("tok", "ref", Instant.now().plusSeconds(300)));
		assertTrue(Files.exists(credFile), "credentials file should exist after save");

		logoutCommand.run();

		assertFalse(Files.exists(credFile), "credentials file should be deleted after logout");
	}
}
