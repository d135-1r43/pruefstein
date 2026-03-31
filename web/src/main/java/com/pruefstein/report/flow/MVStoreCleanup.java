package com.pruefstein.report.flow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Deletes the MVStore persistence file on shutdown so that Quarkus dev-mode
 * hot-reloads don't hit {@code OverlappingFileLockException}: the old
 * ClassLoader releases the file lock only when GC'd, which may be after the new
 * one already tries to open the same path.
 */
@ApplicationScoped
public class MVStoreCleanup
{
	private static final Logger log = Logger.getLogger(MVStoreCleanup.class);

	@ConfigProperty(name = "quarkus.flow.persistence.mvstore.db-path")
	String dbPath;

	void onShutdown(@Observes @jakarta.annotation.Priority(Interceptor.Priority.PLATFORM_BEFORE) ShutdownEvent ev)
	{
		deleteIfExists(dbPath);
		deleteIfExists(dbPath + ".lock");
	}

	private void deleteIfExists(String path)
	{
		try
		{
			Files.deleteIfExists(Path.of(path));
		}
		catch (IOException e)
		{
			log.debugf("Could not delete MVStore file %s: %s", path, e.getMessage());
		}
	}
}
