package com.pruefstein.agent.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one thing that makes {@code agent login --server} stick: the stored URL
 * has to reach {@code @RegisterRestClient}, which only reads MicroProfile
 * Config.
 */
@QuarkusTest
class CredentialsConfigSourceTest
{
	private static final String URL_KEY = "quarkus.rest-client.pruefstein-api.url";

	private static final Path CREDENTIALS = Path.of(
		System.getProperty("user.home"), ".config", "pruefstein", "credentials.json");

	private Path backup;

	@BeforeEach
	void preserveExistingLogin() throws Exception
	{
		if (Files.exists(CREDENTIALS))
		{
			backup = Files.createTempFile("pruefstein-credentials", ".json");
			Files.copy(CREDENTIALS, backup, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterEach
	void restoreExistingLogin() throws Exception
	{
		Files.deleteIfExists(CREDENTIALS);
		if (backup != null)
		{
			Files.createDirectories(CREDENTIALS.getParent());
			Files.move(backup, CREDENTIALS, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@Test
	void theStoredServerBecomesTheRestClientBaseUrl() throws Exception
	{
		Files.createDirectories(CREDENTIALS.getParent());
		Files.writeString(CREDENTIALS, """
			{"serverUrl":"https://pruefstein.example.com","issuer":"https://idp.example.com",
			 "clientId":"agent","scopes":"openid offline_access","accessToken":"tok",
			 "refreshToken":"ref","expiresAt":"2099-01-01T00:00:00Z"}
			""");

		assertEquals("https://pruefstein.example.com",
			ConfigProvider.getConfig().getValue(URL_KEY, String.class));
	}

	@Test
	void withoutALoginTheConfiguredDefaultStands() throws Exception
	{
		Files.deleteIfExists(CREDENTIALS);

		assertEquals("http://localhost:8080", ConfigProvider.getConfig().getValue(URL_KEY, String.class));
	}
}
