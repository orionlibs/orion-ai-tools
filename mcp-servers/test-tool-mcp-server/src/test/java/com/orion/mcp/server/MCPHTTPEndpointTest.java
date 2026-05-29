package com.orion.mcp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
public class MCPHTTPEndpointTest
{
    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    // session ID obtained from initialize — shared across tests in the same instance
    private String sessionId;
    // Streamable HTTP sends everything as POST to /mcp
    private static final String MCP_ENDPOINT = "/mcp";


    @BeforeEach
    void setUp() throws Exception
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        sessionId = initializeSession();
    }


    @Test
    void initialize_returnsServerCapabilities() throws Exception
    {
        String body = postMcp("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {},
                            "clientInfo": { "name": "test-client", "version": "1.0" }
                          }
                        }
                        """, sessionId);
        assertThat(body).contains("protocolVersion")
                        .contains("capabilities")
                        .contains("tools");
    }
    // ----------------------------------------------------------------
    // tools/list
    // ----------------------------------------------------------------


    @Test
    void toolsList_containsGreetTool() throws Exception
    {
        String body = postMcp("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/list",
                          "params": {}
                        }
                        """, sessionId);
        assertThat(body).contains("greet_person");
    }


    @Test
    void toolsList_allToolsHaveDescriptions() throws Exception
    {
        String body = postMcp("""
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "method": "tools/list",
                          "params": {}
                        }
                        """, sessionId);
        // every registered tool name must be accompanied by a non-empty description
        assertThat(body).contains("description");
        assertThat(body).doesNotContain("\"description\":\"\"");
    }
    // ----------------------------------------------------------------
    // tools/call — greet
    // ----------------------------------------------------------------


    @Test
    void toolCall_greet_returnsGreeting() throws Exception
    {
        String body = postMcp("""
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "method": "tools/call",
                          "params": {
                            "name": "greet_person",
                            "arguments": {"name": "Alice"}
                          }
                        }
                        """, sessionId);
        assertThat(body).contains("Alice");
    }


    @Test
    void toolCall_unknownTool_returnsError() throws Exception
    {
        String body = postMcp("""
                        {
                          "jsonrpc": "2.0",
                          "id": 9,
                          "method": "tools/call",
                          "params": {
                            "name": "does_not_exist",
                            "arguments": {}
                          }
                        }
                        """, sessionId);
        assertThat(body).contains("error");
    }


    /**
     * Sends initialize and returns the mcp-session-id from the response header.
     */
    private String initializeSession() throws Exception
    {
        var result = mockMvc.perform(post(MCP_ENDPOINT)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)  // ← try both
                                        .content("""
                                                        {
                                                          "jsonrpc": "2.0",
                                                          "id": 0,
                                                          "method": "initialize",
                                                          "params": {
                                                            "protocolVersion": "2025-11-25",
                                                            "capabilities": {},
                                                            "clientInfo": { "name": "test-client", "version": "1.0" }
                                                          }
                                                        }
                                                        """))
                        .andReturn();  // ← no andExpect here
        var response = result.getResponse();
        System.out.println("=== INIT STATUS : " + response.getStatus());
        System.out.println("=== INIT HEADERS: " + response.getHeaderNames().stream().map(h -> h + "=" + response.getHeader(h)).toList());
        System.out.println("=== INIT BODY   : " + response.getContentAsString());
        return response.getHeader("mcp-session-id");
    }


    private String postMcp(String jsonBody, String mcpSessionId) throws Exception
    {
        var request = post(MCP_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)  // ← add TEXT_EVENT_STREAM
                        .content(jsonBody);
        if(mcpSessionId != null)
        {
            request = request.header("mcp-session-id", mcpSessionId);
        }
        var result = mockMvc.perform(request).andReturn();
        var response = result.getResponse();
        System.out.println("=== STATUS : " + response.getStatus());
        System.out.println("=== BODY   : " + response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(200);
        return response.getContentAsString();
    }
}
