package com.pruefstein.agent.client;

public record ResultPayload(Long itemId, boolean passed, String output)
{
}
