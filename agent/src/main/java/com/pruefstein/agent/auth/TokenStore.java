package com.pruefstein.agent.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class TokenStore
{
	/**
	 * Also read by {@code CredentialsConfigSource} before CDI exists, which is
	 * why the path lives in a constant both can reach.
	 */
	public static final Path CREDENTIALS_FILE = Path.of(
		System.getProperty("user.home"), ".config", "pruefstein", "credentials.json");

	private static final Logger LOG = getLogger(TokenStore.class);

	@Inject
	ObjectMapper objectMapper;

	public Optional<Credentials> load()
	{
		if (!Files.exists(CREDENTIALS_FILE))
		{
			return Optional.empty();
		}
		try
		{
			return Optional.of(objectMapper.readValue(CREDENTIALS_FILE.toFile(), Credentials.class));
		}
		catch (IOException e)
		{
			LOG.warn("Error while loading credentials", e);
			return Optional.empty();
		}
	}

	public void save(Credentials credentials)
	{
		try
		{
			Files.createDirectories(CREDENTIALS_FILE.getParent());
			objectMapper.writeValue(CREDENTIALS_FILE.toFile(), credentials);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Could not save credentials to " + CREDENTIALS_FILE, e);
		}
	}

	public void clear()
	{
		try
		{
			Files.deleteIfExists(CREDENTIALS_FILE);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Could not clear credentials at " + CREDENTIALS_FILE, e);
		}
	}
}
