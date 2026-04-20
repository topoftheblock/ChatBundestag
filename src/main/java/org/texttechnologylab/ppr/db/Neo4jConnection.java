package org.texttechnologylab.ppr.db;

import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.model.interfaces.Kommentar;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Implementierung der DatabaseConnection Schnittstelle für eine eingebettete Neo4j-Datenbank
 */
public class Neo4jConnection implements DatabaseConnection {

    private final DatabaseManagementService managementService;
    private final GraphDatabaseService graphDb;
    private static final String DEFAULT_DB_NAME = "neo4j";

    /**
     * Konstruiert eine neue Neo4jConnection und startet die
     * eingebettete Datenbank im angegebenenVerzeichnis.
     * WICHTIG: Bolt wird aktiviert, damit LangChain4j via RAG zugreifen kann.
     */
    public Neo4jConnection(String databaseDirectory) {
        System.out.println("Starte Embedded Neo4j-Datenbank in: " + databaseDirectory);
        Path dbPath = new File(databaseDirectory).toPath();

        // Aktiviere den Bolt Connector, damit LangChain4j (und GraphDatabase.driver) sich verbinden können
        this.managementService = new DatabaseManagementServiceBuilder(dbPath)
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
                .build();

        this.graphDb = managementService.database(DEFAULT_DB_NAME);
        System.out.println("Neo4j-Datenbank (inkl. Bolt auf Port 7687) erfolgreich gestartet.");
    }

    private void executeWriteQuery(String query, Map<String, Object> parameters) {
        try (Transaction tx = graphDb.beginTx()) {
            tx.execute(query, parameters);
            tx.commit();
        } catch (Exception e) {
            System.err.println("Fehler bei Cypher-Query: " + query);
            e.printStackTrace();
        }
    }

    private void executeUnwindQuery(String query, List<Map<String, Object>> dataList, String listName) {
        try (Transaction tx = graphDb.beginTx()) {
            tx.execute(query, Map.of(listName, dataList));
            tx.commit();
        } catch (Exception e) {
            System.err.println("Fehler bei Cypher-UNWIND-Query: " + query);
            e.printStackTrace();
        }
    }

    @Override
    public void loescheDatenbank() {
        System.out.println("Lösche alte Datenbankinhalte...");
        executeWriteQuery("MATCH (n) DETACH DELETE n", Map.of());
        System.out.println("Löschen abgeschlossen.");
    }

    @Override
    public void erstelleConstraints() {
        System.out.println("Erstelle Indizes und Constraints...");
        executeWriteQuery("CREATE CONSTRAINT IF NOT EXISTS FOR (s:Sitzung) REQUIRE (s.sitzungNr, s.wahlperiode) IS UNIQUE", Map.of());
        executeWriteQuery("CREATE CONSTRAINT IF NOT EXISTS FOR (r:Redner) REQUIRE r.id IS UNIQUE", Map.of());
        executeWriteQuery("CREATE CONSTRAINT IF NOT EXISTS FOR (r:Rede) REQUIRE r.id IS UNIQUE", Map.of());
        System.out.println("Constraints erstellt.");
    }

    @Override
    public void ladeSitzungen(List<Sitzung> sitzungen) {
        System.out.println("Lade " + sitzungen.size() + " Sitzungen...");
        String query = "UNWIND $data AS props " +
                "MERGE (s:Sitzung { wahlperiode: props.wahlperiode, sitzungNr: props.sitzungNr }) " +
                "SET s.datum = date(props.datum), " +
                "    s.startDateTime = CASE WHEN props.startDateTime IS NOT NULL THEN datetime(props.startDateTime) ELSE null END, " +
                "    s.endDateTime = CASE WHEN props.endDateTime IS NOT NULL THEN datetime(props.endDateTime) ELSE null END";

        List<Map<String, Object>> data = sitzungen.stream()
                .map(Sitzung::toNode)
                .collect(Collectors.toList());

        executeUnwindQuery(query, data, "data");
    }

    @Override
    public void ladeRedner(Collection<Redner> redner) {
        System.out.println("Lade " + redner.size() + " Redner...");
        String query = "UNWIND $data AS props " +
                "MERGE (r:Redner { id: props.id }) " +
                "SET r.vorname = props.vorname, " +
                "    r.nachname = props.nachname, " +
                "    r.titel = props.titel, " +
                "    r.fraktion = props.fraktion";

        List<Map<String, Object>> data = redner.stream()
                .map(Redner::toNode)
                .collect(Collectors.toList());

        executeUnwindQuery(query, data, "data");

        String abgLabelQuery = "MATCH (r:Redner) " +
                "WHERE r.fraktion IS NOT NULL " +
                "SET r:Abgeordneter";

        executeWriteQuery(abgLabelQuery, Map.of());
        System.out.println("Redner geladen und Abgeordneten-Label aktualisiert.");
    }

    @Override
    public void ladeRedenUndKommentare(List<Sitzung> sitzungen) {
        System.out.println("Lade Reden und Kommentare...");

        String redeQuery = "UNWIND $data AS props " +
                "MERGE (r:Rede { id: props.id }) " +
                "SET r.text = props.text, r.textLaenge = props.textLaenge";

        List<Map<String, Object>> redenData = sitzungen.stream()
                .flatMap(s -> s.getReden().stream())
                .map(Rede::toNode)
                .collect(Collectors.toList());

        executeUnwindQuery(redeQuery, redenData, "data");

        String kommentarQuery = "UNWIND $data AS d " +
                "MATCH (r:Rede { id: d.redeId }) " +
                "UNWIND d.kommentare AS kommentarProps " +
                "CREATE (k:Kommentar) " +
                "SET k = kommentarProps " +
                "MERGE (r)-[:BEINHALTET]->(k)";

        List<Map<String, Object>> kommentarData = sitzungen.stream()
                .flatMap(s -> s.getReden().stream())
                .map(rede -> Map.of(
                        "redeId", rede.getId(),
                        "kommentare", rede.getKommentare().stream()
                                .map(Kommentar::toNode)
                                .collect(Collectors.toList())
                ))
                .filter(map -> !((List)map.get("kommentare")).isEmpty())
                .collect(Collectors.toList());

        executeUnwindQuery(kommentarQuery, kommentarData, "data");
        System.out.println("Reden und Kommentare geladen.");
    }

    @Override
    public void erstelleBeziehungen(List<Sitzung> sitzungen) {
        System.out.println("Erstelle Beziehungen (Redner/Rede/Sitzung)...");

        List<Map<String, Object>> beziehungsDaten = sitzungen.stream()
                .flatMap(sitzung -> sitzung.getReden().stream()
                        .filter(rede -> rede.getRedner() != null)
                        .map(rede -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("redeId", rede.getId());
                            map.put("rednerId", rede.getRedner().getId());
                            map.put("sitzungWp", sitzung.getWahlperiode());
                            map.put("sitzungNr", sitzung.getSitzungNr());
                            return map;
                        }))
                .toList();

        String query = "UNWIND $data AS d " +
                "MATCH (r:Rede { id: d.redeId }) " +
                "MATCH (redner:Redner { id: d.rednerId }) " +
                "MATCH (s:Sitzung { wahlperiode: d.sitzungWp, sitzungNr: d.sitzungNr }) " +
                "MERGE (redner)-[:HAT_GESPROCHEN]->(r) " +
                "MERGE (r)-[:GEHALTEN_IN]->(s)";

        executeUnwindQuery(query, beziehungsDaten, "data");
        System.out.println("Alle Daten erfolgreich geladen.");
    }

    @Override
    public void fuehreStatistikenAus() {
        System.out.println("\n--- STATISTIKEN (AUFGABE 4) ---");

        System.out.println("\n(4a) Durchschnittliche Redelänge (Anzahl Zeichen):");
        String q4a_redner =
                "MATCH (rn:Redner)-[:HAT_GESPROCHEN]->(r:Rede) " +
                        "RETURN rn.vorname AS vorname, rn.nachname AS nachname, " +
                        "       rn.fraktion AS Partei, avg(r.textLaenge) AS avgLaenge " +
                        "ORDER BY avgLaenge DESC LIMIT 10";
        printResults(q4a_redner, "  Pro Redner (Top 10):");

        String q4a_fraktion =
                "MATCH (rn:Abgeordneter)-[:HAT_GESPROCHEN]->(r:Rede) " +
                        "WHERE rn.fraktion IS NOT NULL AND rn.fraktion <> '' AND rn.fraktion <> 'FRAKTIONSLOS' " +
                        "RETURN rn.fraktion AS Partei, avg(r.textLaenge) AS avgLaenge " +
                        "ORDER BY avgLaenge DESC";
        printResults(q4a_fraktion, "  Pro Partei:");

        System.out.println("\n(4b) Durchschnittliche Kommentar-Häufigkeit pro Rede:");
        String q4b_redner =
                "MATCH (rn:Redner)-[:HAT_GESPROCHEN]->(r:Rede) " +
                        "OPTIONAL MATCH (r)-[:BEINHALTET]->(k:Kommentar) " +
                        "WITH rn, r, count(k) AS kommentarAnzahl " +
                        "RETURN rn.vorname AS vorname, rn.nachname AS nachname, " +
                        "       rn.fraktion AS Partei, avg(kommentarAnzahl) AS avgKommentare " +
                        "ORDER BY avgKommentare DESC LIMIT 10";
        printResults(q4b_redner, "  Pro Redner (Top 10):");

        String q4b_fraktion =
                "MATCH (rn:Abgeordneter)-[:HAT_GESPROCHEN]->(r:Rede) " +
                        "WHERE rn.fraktion IS NOT NULL AND rn.fraktion <> '' AND rn.fraktion <> 'FRAKTIONSLOS' " +
                        "OPTIONAL MATCH (r)-[:BEINHALTET]->(k:Kommentar) " +
                        "WITH rn, r, count(k) AS kommentarAnzahl " +
                        "RETURN rn.fraktion AS Partei, avg(kommentarAnzahl) AS avgKommentare " +
                        "ORDER BY avgKommentare DESC";
        printResults(q4b_fraktion, "  Pro Partei:");

        System.out.println("\n(4c) Längste Sitzung:");
        String q4c_zeit =
                "MATCH (s:Sitzung) " +
                        "WHERE s.startDateTime IS NOT NULL AND s.endDateTime IS NOT NULL " +
                        "RETURN s.wahlperiode AS wp, s.sitzungNr AS nr, s.datum AS datum, " +
                        "       duration.between(s.startDateTime, s.endDateTime).minutes AS dauerMinuten " +
                        "ORDER BY dauerMinuten DESC LIMIT 1";
        printResults(q4c_zeit, "  Längste Sitzung (nach Zeit):");

        String q4c_text =
                "MATCH (s:Sitzung)<-[:GEHALTEN_IN]-(r:Rede) " +
                        "RETURN s.wahlperiode AS wp, s.sitzungNr AS nr, s.datum AS datum, " +
                        "       sum(r.textLaenge) AS gesamtLaenge " +
                        "ORDER BY gesamtLaenge DESC LIMIT 1";
        printResults(q4c_text, "  Längste Sitzung (nach Redetextlänge):");

        System.out.println("\n--- STATISTIKEN ENDE ---");
    }

    /**
     * Führt erweiterte Graph-Algorithmen und Netzwerkanalysen aus.
     */
    public void fuehreGraphAlgorithmenAus() {
        System.out.println("\n--- ERWEITERTE GRAPH-ANALYSEN ---");

        System.out.println("\n(1) Degree Centrality: Provokanteste Redner (Meiste Unterbrechungen/Kommentare)");
        String qCentrality =
                "MATCH (redner:Redner)-[:HAT_GESPROCHEN]->(r:Rede)-[:BEINHALTET]->(k:Kommentar) " +
                        "WITH redner, count(k) AS totalComments " +
                        "RETURN redner.vorname AS vorname, redner.nachname AS nachname, " +
                        "       redner.fraktion AS Partei, totalComments AS degreeCentrality " +
                        "ORDER BY degreeCentrality DESC LIMIT 10";
        printResults(qCentrality, "  Top 10 Redner mit der höchsten Degree Centrality:");

        System.out.println("\n(2) Community Detection & Interaktions-Netzwerke (Partei-übergreifend)");
        String qCommunity =
                "MATCH (speaker:Abgeordneter)-[:HAT_GESPROCHEN]->(r:Rede)-[:BEINHALTET]->(k:Kommentar) " +
                        "WHERE k.text IS NOT NULL AND speaker.fraktion IS NOT NULL " +
                        "UNWIND ['SPD', 'CDU/CSU', 'BÜNDNIS 90/DIE GRÜNEN', 'FDP', 'AfD', 'DIE LINKE'] AS party " +
                        "WITH speaker, k, party " +
                        "WHERE k.text CONTAINS party AND speaker.fraktion <> party " +
                        "RETURN speaker.fraktion AS sprechendePartei, party AS kommentierendePartei, count(k) AS interaktionsGewicht " +
                        "ORDER BY interaktionsGewicht DESC LIMIT 10";
        printResults(qCommunity, "  Stärkste informelle Netzwerke (Sprecher-Fraktion vs. Kommentierende Partei):");

        System.out.println("\n(3) Pathfinding: Entwicklung eines Themas über die Zeit (z.B. 'Klima')");
        String topic = "klima";
        String qPathfinding =
                "MATCH (r:Rede)-[:GEHALTEN_IN]->(s:Sitzung) " +
                        "WHERE toLower(r.text) CONTAINS '" + topic + "' " +
                        "WITH s, count(r) AS themaErwaehnungen " +
                        "ORDER BY s.datum ASC " +
                        "RETURN s.wahlperiode AS WP, s.sitzungNr AS SitzungNr, s.datum AS Datum, " +
                        "       themaErwaehnungen AS RedenMitThema";
        printResults(qPathfinding, "  Traversierung des Themas '" + topic + "' durch die Zeitachse:");

        System.out.println("\n--- GRAPH-ANALYSEN ENDE ---");
    }

    private void printResults(String query, String title) {
        System.out.println(title);
        try (Transaction tx = graphDb.beginTx();
             Result result = tx.execute(query)) {

            if (!result.hasNext()) {
                System.out.println("    Keine Ergebnisse gefunden.");
                System.out.println();
                return;
            }

            List<String> columns = result.columns();
            List<List<String>> rows = new ArrayList<>();
            Map<String, Integer> colWidths = new LinkedHashMap<>();
            for (String col : columns) {
                colWidths.put(col, col.length());
            }

            while (result.hasNext()) {
                Map<String, Object> rowMap = result.next();
                List<String> rowData = new ArrayList<>();

                for (String col : columns) {
                    Object value = rowMap.get(col);
                    String formattedValue;

                    if (value == null) {
                        if (col.equals("Partei")) {
                            formattedValue = "Keine Fraktion";
                        } else if (col.equals("Rolle")) {
                            formattedValue = "Keine Rolle";
                        }
                        else {
                            formattedValue = "N/A";
                        }
                    } else if (value instanceof Double) {
                        formattedValue = String.format("%.2f", (Double) value);
                    } else {
                        formattedValue = String.valueOf(value);
                    }

                    rowData.add(formattedValue);

                    if (formattedValue.length() > colWidths.get(col)) {
                        colWidths.put(col, formattedValue.length());
                    }
                }
                rows.add(rowData);
            }

            StringBuilder formatBuilder = new StringBuilder();
            StringBuilder separatorBuilder = new StringBuilder();

            formatBuilder.append("    ");
            separatorBuilder.append("    ");

            for (String col : columns) {
                int width = colWidths.get(col);
                formatBuilder.append(" | %-").append(width).append("s");
                separatorBuilder.append(" +").append("-".repeat(width)).append("-");
            }
            formatBuilder.append(" |%n");
            separatorBuilder.append(" +%n");

            String formatString = formatBuilder.toString();
            String separator = String.format(separatorBuilder.toString());

            System.out.print(separator);
            System.out.printf(formatString, columns.toArray());
            System.out.print(separator);

            for (List<String> row : rows) {
                System.out.printf(formatString, row.toArray());
            }
            System.out.print(separator);
            System.out.println();

        } catch (Exception e) {
            System.err.println("Fehler bei Statistik-Query: " + title);
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        System.out.println("Fahre Neo4j-Datenbank herunter...");
        managementService.shutdown();
        System.out.println("Datenbank heruntergefahren.");
    }
}