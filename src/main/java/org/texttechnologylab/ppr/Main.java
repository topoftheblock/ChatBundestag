package org.texttechnologylab.ppr;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.texttechnologylab.ppr.chatbot.ChatUIServer;
import org.texttechnologylab.ppr.chatbot.ParliamentAssistant;
import org.texttechnologylab.ppr.chatbot.RagService;
import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.db.Neo4jConnection;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;
import org.texttechnologylab.ppr.parser.XMLParser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Starting point für die Anwendung zur Verarbeitung von Plenarprotokollen
 * Diese Klasse koordiniert den Process:
 * 1. Finden der XMLdateien im resourcesOrdner.
 * 2. Parsen der XML-Dateien in Java-Objekte Modelle über die {@link AppFactory}.
 * 3. Laden der Objekte in die Neo4j-Datenbank.
 * 4. Ausführen der statistischen Auswertungen auf der Database.
 * 5. Ausführen der Graphen-Algorithmen (Centrality, Communities, Pathfinding).
 * 6. Starten des KI-Assistenten Web-UI (RAG).
 */
public class Main {
    /**
     * Relativer Pfad zum Verzeichnis der Neo4j-Datenbank.
     */
    private static final String DB_PATH = "target/neo4j-db";

    public static void main(String[] args) {

        System.out.println("Gucke nach XML-Protokollen in Ordner /resources...");
        List<String> xmlDateien;
        try {
            xmlDateien = findXmlResources();
        } catch (IOException | URISyntaxException e) {
            System.err.println("Fehler beim Lesen des Ordners: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (xmlDateien.isEmpty()) {
            System.err.println("Warning: Keine .xml-Dateien im Ordner 'src/main/resources' gefunden!");
            return;
        }

        System.out.println("Starte Verarbeitung für " + xmlDateien.size() + " Dateien...");

        try {
            AppFactory factory = AppFactory.getInstance();
            XMLParser parser = factory.getParser();

            List<Sitzung> sitzungen = parser.parseFiles(xmlDateien);
            System.out.println("Parsing abgeschlossen. " + sitzungen.size() + " Sitzungen gefunden.");

            if (sitzungen.isEmpty()) {
                System.err.println("Keine Sitzungen geladen. Datenbank-Upload wird übersprungen.");
                return;
            }

            // Database processing block
            try (DatabaseConnection db = factory.createDatabaseConnection(DB_PATH)) {

                System.out.println("Starte Datenbank-Upload (Aufgabe 3)...");

                db.loescheDatenbank();
                db.erstelleConstraints();

                db.ladeSitzungen(sitzungen);
                db.ladeRedner(parser.getRednerCache().values());
                db.ladeRedenUndKommentare(sitzungen);
                db.erstelleBeziehungen(sitzungen);

                System.out.println("Datenbank-Upload abgeschlossen.");

                // 1. Standard-Statistiken ausführen
                db.fuehreStatistikenAus();

                // 2. Neue Graph-Data-Science Algorithmen ausführen
                if (db instanceof Neo4jConnection) {
                    ((Neo4jConnection) db).fuehreGraphAlgorithmenAus();
                }

                // --- START AI RAG SYSTEM ---
                startChatBot(sitzungen);

            } catch (Exception e) {
                System.err.println("Fehler bei der Datenbankverarbeitung:");
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("Kritischer Fehler bei der Initialisierung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialisiert den RagService und startet die Chat-UI.
     */
    private static void startChatBot(List<Sitzung> sitzungen) {
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey == null || openAiKey.isEmpty()) {
            System.err.println("\n[AI Info] OPENAI_API_KEY nicht gefunden. Überspringe Chatbot-Start.");
            return;
        }

        System.out.println("\n=== Initialisiere Parliament AI Assistant ===");

        // Da wir eine eingebettete DB nutzen, verbinden wir uns über Bolt
        // Wichtig: Bolt muss in Neo4jConnection aktiviert sein!
        try (Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.none())) {

            RagService ragService = new RagService(driver);

            // Alle Reden aus den Sitzungen extrahieren
            List<Rede> allSpeeches = sitzungen.stream()
                    .flatMap(s -> s.getReden().stream())
                    .collect(Collectors.toList());

            // Reden in den Vector Store (Neo4j) hochladen
            System.out.println("Generiere Embeddings für " + allSpeeches.size() + " Reden... (bitte warten)");
            ragService.ingestSpeeches(allSpeeches);

            ParliamentAssistant assistant = ragService.getAssistant();

            System.out.println("\n-------------------------------------------");
            System.out.println("Parliament AI Agent initialized!");
            System.out.println("-------------------------------------------");

            // Start the Web Interface on port 8080
            ChatUIServer uiServer = new ChatUIServer(assistant);
            uiServer.startServer(8080);

            // Keep the main thread alive so the server keeps running
            System.out.println("Press ENTER to shut down the server...");
            new Scanner(System.in).nextLine();

        } catch (Exception e) {
            System.err.println("KI-Fehler: " + e.getMessage());
        }
    }

    /**
     * Durchsucht das resourcesVerzeichnis nach XML-Dateien.
     */
    private static List<String> findXmlResources() throws IOException, URISyntaxException {
        ClassLoader classLoader = Main.class.getClassLoader();
        URL resourceUrl = classLoader.getResource("");
        if (resourceUrl == null) {
            return Collections.emptyList();
        }

        URI resourceUri = resourceUrl.toURI();
        Path resourcePath;

        if (resourceUri.getScheme().equals("jar")) {
            FileSystem fileSystem = FileSystems.newFileSystem(resourceUri, Collections.emptyMap());
            resourcePath = fileSystem.getPath("");
        } else {
            resourcePath = Paths.get(resourceUri);
        }

        try (Stream<Path> walk = Files.walk(resourcePath, 1)) {
            return walk.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".xml"))
                    .filter(name -> !name.equalsIgnoreCase("pom.xml"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}