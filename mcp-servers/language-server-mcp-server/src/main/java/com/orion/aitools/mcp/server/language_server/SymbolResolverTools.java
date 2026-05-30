package com.orion.aitools.mcp.server.language_server;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class SymbolResolverTools
{
    private final LSPBridgeService lspBridgeService;


    public SymbolResolverTools(LSPBridgeService lspBridgeService)
    {
        this.lspBridgeService = lspBridgeService;
    }


    // Java 25 record representing structured output
    public record SymbolResolutionResult(String symbol, String kind, String location, String hoverDocumentation)
    {
    }


    @McpTool(
                    name = "resolve_global_symbol",
                    description = "Finds a symbol's exact declaration, compiled signature, and JSDoc/Javadoc without grep churn."
    )
    public SymbolResolutionResult resolveGlobalSymbol(@McpToolParam(description = "The name of the class, interface, method, or variable to find.") String query)
    {
        try
        {
            var matches = lspBridgeService.searchWorkspaceSymbol(query);
            if(matches.isEmpty())
            {
                return new SymbolResolutionResult(query, "Unknown", "Not Found", "No matches found in AST.");
            }
            // Grab the top lexical match from the AST index
            var topMatch = matches.get(0);
            var location = topMatch.getLocation();
            // Query the exact file position for type information and docstrings
            var hover = lspBridgeService.getHoverInfo(location.getUri(), location.getRange().getStart());
            String docContent = (hover != null && hover.getContents().isRight())
                            ? hover.getContents().getRight().getValue()
                            : "No hover metadata returned by LSP.";
            return new SymbolResolutionResult(topMatch.getName(), topMatch.getKind().toString(), "%s#L%d".formatted(location.getUri(), location.getRange().getStart().getLine() + 1), docContent);
        }
        catch(Exception e)
        {
            return new SymbolResolutionResult(query, "Error", "Exception raised", e.getMessage());
        }
    }
}
