package com.yapily.mcp.server;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

@Service
public class TestTool1
{
    @McpTool(
                    name = "greet_person",
                    description = "Greets a person by name. Use this when asked to say hello to someone."
    )
    public String greet(@McpToolParam(description = "The name of the person to greet", required = true) String name, ToolContext context)
    {
        // read values the client passed in the context map
        String correlationID = null;
        if(context != null && context.getContext() != null)
        {
            correlationID = (String)context.getContext().get("correlationID");
        }
        System.out.println("correlationID provided by client: " + correlationID);
        return "Hello, " + name + "! Hope you're having a great day.";
    }
}
