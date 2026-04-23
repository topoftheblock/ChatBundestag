package org.texttechnologylab.ppr;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.texttechnologylab.ppr.chatbot.ChatUIServer;
import org.texttechnologylab.ppr.chatbot.ParliamentAssistant;
import org.texttechnologylab.ppr.chatbot.RagService;
import org.texttechnologylab.ppr.db.Neo4jConnection;

import java.util.Scanner;

public class ChatbotApp {

    // Pfad zur bereits existierenden Datenbank
    private static final String DB_PATH = "target/neo4j-db";

    public static void main(String[] args) {
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey == null || openAiKey.isEmpty()) {
            System.err.println("OPENAI_API_KEY nicht gefunden. Bitte setze die Umgebungsvariable.");
            return;
        }

        System.out.println("=== Starte isolierten GraphRAG Chatbot ===");

        // 1. Starte NUR die eingebettete Datenbank (ohne sie zu löschen oder neu zu befüllen)
        // Dadurch wird der Bolt-Connector auf Port 7687 geöffnet.
        Neo4jConnection dbConnection = new Neo4jConnection(DB_PATH);

        try {
            // 2. Verbinde den LangChain4j RAG-Service mit der laufenden Datenbank
            Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.none());
            RagService ragService = new RagService(driver);

            // WICHTIG: Wir rufen ragService.ingestSpeeches(...) NICHT auf!

            ParliamentAssistant assistant = ragService.getAssistant();

            // 3. Starte das Web-Interface
            ChatUIServer uiServer = new ChatUIServer(assistant);
            uiServer.startServer(8080);

            // 4. Halte den Prozess am Leben
            System.out.println("Drücke ENTER, um den Server herunterzufahren...");
            new Scanner(System.in).nextLine();

            driver.close();
        } catch (Exception e) {
            System.err.println("Fehler beim Starten des Chatbots: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.close();
        }
    }
}