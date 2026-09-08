package com.pruefstein.agent.runner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes sure {@code osqueryi} is installed before a run starts, and offers to
 * install it when it is not.
 * <p>
 * Every check and the whole inventory shell out to {@code osqueryi}
 * ({@link ComplianceRunner}), so a run without it pushes a report in which
 * every single check errored — which reads like a badly misconfigured machine
 * rather than a missing tool. Asking rather than installing unprompted is the
 * other half: this is someone's laptop, and the installer wants a password.
 */
@ApplicationScoped
public class OsqueryRequirement
{
	private static final Logger LOG = LoggerFactory.getLogger(OsqueryRequirement.class);

	private static final String BINARY = "osqueryi";
	private static final String DOWNLOAD_PAGE = "https://osquery.io/downloads";

	/**
	 * An install is only offered on macOS: the inventory query and the seeded
	 * checks read macOS-only osquery tables ({@code apps},
	 * {@code homebrew_packages}), so an agent anywhere else has more missing
	 * than the binary.
	 */
	private static final List<String> MACOS_INSTALL = List.of("brew", "install", "--cask", "osquery");

	/**
	 * @return {@code true} when {@code osqueryi} can be run — it was already
	 *         installed, or it just was. {@code false} means abort: the answer
	 *         was no, the install failed, or there was nobody to ask.
	 */
	public boolean ensureAvailable()
	{
		if (isOnPath(BINARY))
		{
			return true;
		}

		Optional<List<String>> command = installCommand(System.getProperty("os.name"));
		if (command.isEmpty() || !isOnPath(command.get().getFirst()))
		{
			System.out.println(BINARY + " is not installed. Install osquery and run again: " + DOWNLOAD_PAGE);
			return false;
		}

		String commandLine = String.join(" ", command.get());
		return confirm(commandLine) && install(command.get(), commandLine);
	}

	private static boolean confirm(String commandLine)
	{
		Prompt.Answer answer = Prompt.ask("You need " + BINARY + " to continue, install it? [y/n]");
		if (answer == Prompt.Answer.YES)
		{
			return true;
		}
		if (answer == Prompt.Answer.NONE)
		{
			// Nothing typed the newline that would have closed the prompt line
			System.out.println();
		}
		System.out.println("Aborted. " + BINARY + " is required; install it with: " + commandLine);
		return false;
	}

	private static boolean install(List<String> command, String commandLine)
	{
		System.out.println("Running " + commandLine);
		try
		{
			// inheritIO: the cask installs a signed pkg, so Homebrew asks for a
			// sudo password on the terminal. Captured streams would swallow that
			// prompt and the install would sit there looking hung.
			Process process = new ProcessBuilder(command).inheritIO().start();
			// Deliberately unbounded, unlike the query calls: a download plus a
			// pkg install runs for minutes, and killing one half-way leaves a
			// worse mess than waiting does.
			int exit = process.waitFor();
			if (exit != 0)
			{
				System.out.println(
					"Install failed (exit " + exit + "). Install osquery manually: " + DOWNLOAD_PAGE);
				return false;
			}
		}
		catch (IOException e)
		{
			LOG.warn("Could not run {}.", commandLine, e);
			return false;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			LOG.warn("Interrupted while waiting for {}.", commandLine, e);
			return false;
		}

		if (!isOnPath(BINARY))
		{
			System.out.println(commandLine + " finished, but " + BINARY
				+ " is still not on the PATH. Open a new shell and run the agent again.");
			return false;
		}
		System.out.println(BINARY + " installed.");
		return true;
	}

	private static boolean isOnPath(String binary)
	{
		return locate(binary, System.getenv("PATH")).isPresent();
	}

	/**
	 * The same lookup {@link ProcessBuilder} will do, done up front so the
	 * agent can say what is missing instead of failing on every check with an
	 * {@link IOException}.
	 */
	static Optional<Path> locate(String binary, String pathEnv)
	{
		if (pathEnv == null || pathEnv.isBlank())
		{
			return Optional.empty();
		}
		return Arrays.stream(pathEnv.split(File.pathSeparator))
			.filter(directory -> !directory.isBlank())
			.map(directory -> Path.of(directory).resolve(binary))
			.filter(Files::isRegularFile)
			.filter(Files::isExecutable)
			.findFirst();
	}

	static Optional<List<String>> installCommand(String osName)
	{
		if (osName != null && osName.toLowerCase(Locale.ROOT).contains("mac"))
		{
			return Optional.of(MACOS_INSTALL);
		}
		return Optional.empty();
	}
}
