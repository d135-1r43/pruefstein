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
	private static final Path TOKEN_FILE = Path.of(
		System.getProperty("user.home"), ".config", "pruefstein", "credentials.json");

	private static final Logger LOG = getLogger(TokenStore.class);

	@Inject
	ObjectMapper objectMapper;

	public Optional<StoredToken> load()
	{
		if (!Files.exists(TOKEN_FILE))
		{
			return Optional.empty();
		}
		try
		{
			return Optional.of(objectMapper.readValue(TOKEN_FILE.toFile(), StoredToken.class));
		}
		catch (IOException e)
		{
			LOG.warn("Error while loading token", e);
			return Optional.empty();
		}
	}

	public void save(StoredToken token)
	{
		try
		{
			Files.createDirectories(TOKEN_FILE.getParent());
			objectMapper.writeValue(TOKEN_FILE.toFile(), token);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Could not save credentials to " + TOKEN_FILE, e);
		}
	}

	public void clear()
	{
		try
		{
			Files.deleteIfExists(TOKEN_FILE);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Could not clear credentials at " + TOKEN_FILE, e);
		}
	}
}
