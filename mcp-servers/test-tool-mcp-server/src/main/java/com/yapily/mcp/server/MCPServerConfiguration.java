package com.yapily.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MCPServerConfiguration
{
    @Bean
    public ToolCallbackProvider greetingToolCallbackProvider(TestTool1 testTool1)
    {
        return MethodToolCallbackProvider.builder()
                        .toolObjects(testTool1)
                        .build();
    }
}
