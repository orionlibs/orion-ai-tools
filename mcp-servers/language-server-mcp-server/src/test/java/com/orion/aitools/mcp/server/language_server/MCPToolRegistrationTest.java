package com.orion.aitools.mcp.server.language_server;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class MCPToolRegistrationTest
{
    @Autowired
    McpSyncServer mcpSyncServer;


    @Test
    void resolveGlobalSymbolTool_isRegistered()
    {
        assertToolRegistered("resolve_global_symbol");
    }


    @Test
    void resolveGlobalSymbolTool_hasCorrectInputSchema()
    {
        McpSchema.Tool tool = findTool("resolve_global_symbol");
        // schema should declare a 'name' parameter
        String schemaJson = tool.inputSchema().toString();
        assertThat(schemaJson).contains("name");
    }


    @Test
    void allTools_haveDescriptions()
    {
        List<Tool> tools = mcpSyncServer.listTools();
        assertThat(tools).allSatisfy(tool ->
                        assertThat(tool.description()).as("Tool '%s' must have a description", tool.name())
                                        .isNotBlank()
        );
    }


    private void assertToolRegistered(String toolName)
    {
        assertThat(findTool(toolName)).as("Expected tool '%s' to be registered", toolName).isNotNull();
    }


    private McpSchema.Tool findTool(String name)
    {
        return mcpSyncServer.listTools()
                        .stream()
                        .filter(t -> t.name().equals(name))
                        .findFirst()
                        .orElse(null);
    }
}
