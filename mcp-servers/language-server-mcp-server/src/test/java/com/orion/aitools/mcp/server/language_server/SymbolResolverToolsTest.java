package com.orion.aitools.mcp.server.language_server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SymbolResolverToolsTest
{
    @Mock
    private LSPBridgeService lspBridgeService;
    @InjectMocks
    private SymbolResolverTools symbolResolverTools;
    private SymbolInformation sampleSymbol;
    private Hover sampleHover;


    @BeforeEach
    void setUp()
    {
        // Construct a mock LSP SymbolInformation object
        Position startPosition = new Position(41, 15); // 0-indexed internally (Line 42)
        Range range = new Range(startPosition, new Position(41, 30));
        Location location = new Location("file:///src/utils/Finance.java", range);
        sampleSymbol = new SymbolInformation("calculateTax", SymbolKind.Function, location);
        // Construct a mock LSP Hover object containing Markdown documentation
        MarkupContent markupContent = new MarkupContent();
        markupContent.setKind(MarkupKind.MARKDOWN);
        markupContent.setValue("Calculates state tax.");
        sampleHover = new Hover(markupContent);
    }


    @Test
    @DisplayName("Should successfully resolve a global symbol and fetch documentation")
    void testResolveGlobalSymbolSuccess() throws Exception
    {
        // Arrange
        doReturn(List.of(sampleSymbol)).when(lspBridgeService).searchWorkspaceSymbol("calculateTax");
        when(lspBridgeService.getHoverInfo(eq("file:///src/utils/Finance.java"), any(Position.class))).thenReturn(sampleHover);
        // Act
        var result = symbolResolverTools.resolveGlobalSymbol("calculateTax");
        // Assert
        assertNotNull(result);
        assertEquals("calculateTax", result.symbol());
        assertEquals("Function", result.kind());
        assertEquals("file:///src/utils/Finance.java#L42", result.location()); // Verifies 1-based line conversion
        assertEquals("Calculates state tax.", result.hoverDocumentation());
    }


    @Test
    @DisplayName("Should return a clean 'Not Found' record when symbol does not exist in AST")
    void testResolveGlobalSymbolNotFound() throws Exception
    {
        // Arrange
        when(lspBridgeService.searchWorkspaceSymbol("missingMethod")).thenReturn(Collections.emptyList());
        // Act
        var result = symbolResolverTools.resolveGlobalSymbol("missingMethod");
        // Assert
        assertNotNull(result);
        assertEquals("missingMethod", result.symbol());
        assertEquals("Unknown", result.kind());
        assertEquals("Not Found", result.location());
        assertTrue(result.hoverDocumentation().contains("No matches found"));
    }


    @Test
    @DisplayName("Should gracefully catch exceptions and pass the error context to Claude")
    void testResolveGlobalSymbolHandlesException() throws Exception
    {
        // Arrange
        when(lspBridgeService.searchWorkspaceSymbol("brokenSymbol")).thenThrow(new RuntimeException("LSP Process crashed"));
        // Act
        var result = symbolResolverTools.resolveGlobalSymbol("brokenSymbol");
        // Assert
        assertNotNull(result);
        assertEquals("Error", result.kind());
        assertEquals("Exception raised", result.location());
        assertEquals("LSP Process crashed", result.hoverDocumentation());
    }
}
