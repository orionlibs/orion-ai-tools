package com.orion.aitools.mcp.server.language_server;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.springframework.stereotype.Service;

@Service
public class LspBridgeService
{
    private Process lspProcess;
    private LanguageServer languageServer;


    @PostConstruct
    public void init()
    {
        try
        {
            var pb = new ProcessBuilder("jdtls");
            this.lspProcess = pb.start();
            var launcher = LSPLauncher.createClientLauncher(
                            new GenericLSPClient(),
                            lspProcess.getInputStream(),
                            lspProcess.getOutputStream()
            );
            launcher.startListening();
            this.languageServer = launcher.getRemoteProxy();
            var initParams = new InitializeParams();
            initParams.setRootUri(System.getenv("CLAUDE_PROJECT_DIR"));
            languageServer.initialize(initParams).get();
            languageServer.initialized(new InitializedParams());
        }
        catch(IOException e)
        {
            // Log the error but allow the Spring Context to finish loading successfully
            System.err.println("CRITICAL: Could not start 'jdtls' language server binary. Ensure it is installed and in your PATH. Tool features will be unavailable.");
        }
        catch(Exception e)
        {
            System.err.println("Failed to initialize LSP connection: " + e.getMessage());
        }
    }


    public List<? extends SymbolInformation> searchWorkspaceSymbol(String query) throws Exception
    {
        var params = new WorkspaceSymbolParams(query);
        return languageServer.getWorkspaceService().symbol(params).get().getLeft();
    }


    public Hover getHoverInfo(String uri, Position position) throws Exception
    {
        var params = new HoverParams(new TextDocumentIdentifier(uri), position);
        return languageServer.getTextDocumentService().hover(params).get();
    }


    @PreDestroy
    public void shutdown()
    {
        if(lspProcess != null && lspProcess.isAlive())
        {
            lspProcess.destroy();
        }
    }
}