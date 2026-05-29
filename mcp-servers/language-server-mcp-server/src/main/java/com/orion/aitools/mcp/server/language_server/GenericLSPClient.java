package com.orion.aitools.mcp.server.language_server;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Minimal implementation required by LSP4J to handle async callbacks from the LSP
 */
public class GenericLSPClient implements LanguageClient
{
    public void telemetryEvent(Object object)
    {
    }


    public void publishDiagnostics(PublishDiagnosticsParams pdp)
    {
    }


    public void showMessage(MessageParams mp)
    {
    }


    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams smrp)
    {
        return null;
    }


    public void logMessage(MessageParams mp)
    {
    }
}
