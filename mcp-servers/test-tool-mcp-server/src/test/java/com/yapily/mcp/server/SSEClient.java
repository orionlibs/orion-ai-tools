package com.yapily.mcp.server;

import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;

public class SSEClient {
    public static void main(String[] args) {
        var transport = HttpClientSseClientTransport.builder("http://localhost:8080").build();
        new SampleClient(transport).run();
    }
}
