package com.yapily.mcp.server;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

public class SampleClient {
    private final McpClientTransport transport;

    public SampleClient(McpClientTransport transport) {
        this.transport = transport;
    }

    public void run() {
        McpSyncClient client = McpClient.sync(this.transport).build();
        client.initialize();
        client.ping();
        McpSchema.ListToolsResult toolsList = client.listTools();
        //System.out.println("Available Tools = " + toolsList);
        toolsList.tools().stream().forEach(tool -> {
            System.out.println("Tool: " + tool.name() + ", description: " + tool.description() + ", schema: " + tool.inputSchema());
        });
        McpSchema.CallToolResult getInstitutionsResult = client.callTool(new McpSchema.CallToolRequest("getRegisteredInstitutions",
                                                                                                      Map.of()));
        System.out.println("getInstitutionsResult: " + getInstitutionsResult);
        //CallToolResult alertResult = client.callTool(new CallToolRequest("getAlerts", Map.of("state", "NY")));
        client.closeGracefully();
    }
}
