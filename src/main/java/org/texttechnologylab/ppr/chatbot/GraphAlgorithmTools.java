package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.List;
import java.util.Map;

public class GraphAlgorithmTools {

    private final Driver driver;

    public GraphAlgorithmTools(Driver driver) {
        this.driver = driver;
    }

    /**
     * @Tool beschreibt dem LLM genau, WANN es dieses Tool nutzen soll.
     * @P beschreibt die Parameter, damit das LLM weiß, wie es sie befüllen muss.
     */
    @Tool("Führt Graphen-Algorithmen und komplexe Netzwerkanalysen im Parlament durch. " +
            "Nutze dies, wenn der Nutzer nach Beziehungen fragt (z.B. 'Über wen sind Politiker A und B verbunden?') " +
            "oder nach Zentralität (z.B. 'Wer ist am stärksten im Parlament vernetzt / spricht am meisten?').")
    public String executeGraphAlgorithm(
            @P("Der auszuführende Algorithmus. Erlaubte Werte: 'SHORTEST_PATH' (für Verbindungen) oder 'DEGREE_CENTRALITY' (für Aktivität)") String algorithmType,
            @P("Vorname des ersten Redners (nur nötig bei SHORTEST_PATH, sonst leer)") String vorname1,
            @P("Nachname des ersten Redners (nur nötig bei SHORTEST_PATH, sonst leer)") String nachname1,
            @P("Vorname des zweiten Redners (nur nötig bei SHORTEST_PATH, sonst leer)") String vorname2,
            @P("Nachname des zweiten Redners (nur nötig bei SHORTEST_PATH, sonst leer)") String nachname2) {

        System.out.println("[Agent Tool] Führe Graph-Algorithmus aus: " + algorithmType);

        try (Session session = driver.session()) {

            // Algorithmus 1: Kürzester Pfad (Shortest Path)
            if ("SHORTEST_PATH".equalsIgnoreCase(algorithmType)) {
                if (nachname1 == null || nachname2 == null) {
                    return "Für den Shortest Path Algorithmus werden die Namen beider Redner benötigt.";
                }

                // Nutzt die native Neo4j shortestPath() Funktion
                String cypher = "MATCH p=shortestPath((r1:Redner {vorname: $v1, nachname: $n1})-[*]-(r2:Redner {vorname: $v2, nachname: $n2})) " +
                        "RETURN [n in nodes(p) | coalesce(n.vorname + ' ' + n.nachname, n.text, n.datum, labels(n)[0])] AS pfad";

                Result result = session.run(cypher, Map.of(
                        "v1", vorname1, "n1", nachname1,
                        "v2", vorname2, "n2", nachname2
                ));

                if (!result.hasNext()) {
                    return "Es konnte keine direkte oder indirekte Verbindung zwischen den beiden Rednern im Graph gefunden werden.";
                }
                return "Kürzester Pfad gefunden: " + result.next().get("pfad").asList().toString();
            }

            // Algorithmus 2: Degree Centrality (Knotengrad)
            else if ("DEGREE_CENTRALITY".equalsIgnoreCase(algorithmType)) {
                // Berechnet, wer die meisten ausgehenden Beziehungen (Reden) hat.
                String cypher = "MATCH (r:Redner)-[:HAT_GESPROCHEN]->(rede:Rede) " +
                        "RETURN r.vorname + ' ' + r.nachname AS redner, count(rede) AS degree " +
                        "ORDER BY degree DESC LIMIT 5";

                Result result = session.run(cypher);
                List<String> records = result.list(record ->
                        record.get("redner").asString() + " (Zentralitäts-Score: " + record.get("degree").asInt() + ")"
                );
                return "Top Redner nach Degree Centrality:\n" + String.join("\n", records);
            }

            else {
                return "Fehler: Unbekannter Algorithmus. Bitte nutze 'SHORTEST_PATH' oder 'DEGREE_CENTRALITY'.";
            }

        } catch (Exception e) {
            return "Fehler bei der Ausführung des Graphen-Algorithmus: " + e.getMessage();
        }
    }
}