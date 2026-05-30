package com.orion.aitools.mcp.server.language_server;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LSPBridgeService
{
    private static final String DOWNLOAD_URL = "https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz";
    private static final Path CACHE_DIR = Paths.get(System.getProperty("user.home"), ".mcp-cache", "jdtls");
    private Process lspProcess;
    private LanguageServer languageServer;
    @Value("${lsp.server.enabled:true}")
    private boolean lspEnabled;


    @PostConstruct
    public void init() throws Exception
    {
        if(!lspEnabled)
        {
            System.err.println("LSP Bridge initialization skipped (disabled by configuration).");
            return;
        }
        // 1. Ensure jdtls is provisioned locally
        ensureJdtlsIsInstalled();
        // 2. Resolve OS-specific configurations and the Equinox launcher JAR
        String osConfigDir = detectOSConfigDir();
        Path launcherJar = findEquinoxLauncher();
        // 3. Construct raw Java command parameters to run the headless language server
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Declipse.application=org.eclipse.jdt.ls.core.id1");
        command.add("-Dosgi.bundles.defaultStartLevel=4");
        command.add("-Declipse.product=org.eclipse.jdt.ls.core.product");
        command.add("-Xmx1G");
        command.add("--add-modules=ALL-SYSTEM");
        command.add("--add-opens=java.base/java.util=ALL-UNNAMED");
        command.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
        command.add("-jar");
        command.add(launcherJar.toAbsolutePath().toString());
        command.add("-configuration");
        command.add(CACHE_DIR.resolve(osConfigDir).toAbsolutePath().toString());
        command.add("-data");
        command.add(CACHE_DIR.resolve("workspace").toAbsolutePath().toString());
        // 4. Start the process
        this.lspProcess = new ProcessBuilder(command).start();
        // 5. Connect LSP Streams
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


    private void ensureJdtlsIsInstalled() throws IOException
    {
        if(Files.exists(CACHE_DIR.resolve("plugins")))
        {
            return; // Already downloaded and unzipped
        }
        Files.createDirectories(CACHE_DIR);
        System.err.println("Downloading jdtls server binaries... Please wait.");
        try(InputStream in = URI.create(DOWNLOAD_URL).toURL().openStream();
                        GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
                        TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn))
        {
            TarArchiveEntry entry;
            while((entry = tarIn.getNextTarEntry()) != null)
            {
                Path entryPath = CACHE_DIR.resolve(entry.getName());
                if(entry.isDirectory())
                {
                    Files.createDirectories(entryPath);
                }
                else
                {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(tarIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        System.err.println("jdtls successfully extraction completed.");
    }


    private String detectOSConfigDir()
    {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win"))
        {
            return "config_win";
        }
        if(os.contains("mac"))
        {
            return "config_mac";
        }
        return "config_linux";
    }


    private Path findEquinoxLauncher() throws IOException
    {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(CACHE_DIR.resolve("plugins"), "org.eclipse.equinox.launcher_*.jar"))
        {
            for(Path path : stream)
            {
                return path; // Return the first matched versioned launcher jar file
            }
        }
        throw new FileNotFoundException("Could not locate Equinox Launcher JAR inside plugins directory.");
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