package com.pruefstein.agent.client;

/**
 * One installed application or Homebrew package, as reported to the server.
 *
 * @param source {@code app}, {@code brew:formula} or {@code brew:cask}
 */
public record InstalledAppPayload(String source, String name, String identifier, String version, String path)
{
}
