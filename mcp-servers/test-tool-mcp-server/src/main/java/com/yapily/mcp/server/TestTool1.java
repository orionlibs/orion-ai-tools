package com.yapily.mcp.server;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class TestTool1
{
    @Tool(description = "Greets a person by name and returns a friendly message")
    public String greet(@ToolParam(description = "The name of the person to greet") String name)
    {
        return "Hello, " + name + "! Hope you are having a great day";
    }
}
