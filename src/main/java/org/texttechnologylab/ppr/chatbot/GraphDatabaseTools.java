package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.agent.tool.Tool;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.List;
import java.util.stream.Collectors;

public class GraphDatabaseTools {

    private final Driver driver;

    public GraphDatabaseTools(Driver driver) {
        this.driver = driver;
    }

    /**
     * The @Tool annotation tells the LLM that this function exists and when to use it.
     * We provide the exact graph schema in the description so the LLM writes correct Cypher.
     */
    @Tool("Führt eine Cypher-Abfrage auf der Neo4j-Datenbank aus, um statistische oder strukturelle Fragen zum Parlament zu beantworten. " +
            "Nutze dies, wenn der Nutzer nach Anzahlen, Parteien, Sitzungsdaten oder wer wen unterbrochen hat fragt. " +
            "Das Schema ist:\n" +
            "- Knoten: Rede {id}, Redner {id, vorname, nachname, fraktion}, Sitzung {id, datum}, Kommentar {text}\n" +
            "- Beziehungen: (redner:Redner)-[:HAT_GESPROCHEN]->(r:Rede), (r:Rede)-[:GEHALTEN_IN]->(s:Sitzung), (r:Rede)-[:BEINHALTET]->(k:Kommentar)\n" +
            "Gib immer spezifische Felder zurück, benutze niemals RETURN *. Limitiere Ergebnisse auf max 10.")
    public String executeCypherQuery(String cypherQuery) {
        System.out.println("[Agent Tool] Führe generierte Cypher-Query aus: \n" + cypherQuery);
        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery);
            List<String> records = result.list(record -> record.asMap().toString());

            if (records.isEmpty()) {
                return "Die Datenbankabfrage hat keine Ergebnisse geliefert.";
            }
            return String.join("\n", records);
        } catch (Exception e) {
            return "Fehler bei der Ausführung der Cypher-Query (bitte korrigiere die Syntax): " + e.getMessage();
        }
    }
}