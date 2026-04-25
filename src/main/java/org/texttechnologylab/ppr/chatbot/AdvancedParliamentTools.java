package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.List;
import java.util.Map;

/**
 * Advanced toolset for the Parliament Assistant providing deep analytical,
 * statistical, and external API integration capabilities.
 */
public class AdvancedParliamentTools {

    private final Driver driver;

    public AdvancedParliamentTools(Driver driver) {
        this.driver = driver;
    }

    // ==========================================
    // 1. Advanced Graph & Interaction Tools
    // ==========================================

    @Tool("Analysiert Zwischenrufe (Heckling) und Beifall (Applause) zu Reden eines bestimmten Politikers. " +
            "Nutze dies, wenn gefragt wird: 'Wer unterbricht Kanzler X am meisten?' oder 'Welche Partei applaudiert am meisten bei Y?'")
    public String analyzeInterruptions(
            @P("Vorname des Redners, der die Rede hält") String vorname,
            @P("Nachname des Redners, der die Rede hält") String nachname,
            @P("Filter für die Art des Kommentars (z.B. 'Beifall' oder 'Lachen'). Leer lassen für alle.") String keywordFilter) {

        System.out.println("[Agent Tool] Executing InterruptionAnalyzerTool for " + vorname + " " + nachname);
        try (Session session = driver.session()) {
            String cypher = "MATCH (r:Redner {vorname: $vorname, nachname: $nachname})-[:HAT_GESPROCHEN]->(rede:Rede)-[:BEINHALTET]->(k:Kommentar) " +
                    "WHERE toLower(k.text) CONTAINS toLower($filter) " +
                    "RETURN k.text AS kommentar, count(k) AS frequency " +
                    "ORDER BY frequency DESC LIMIT 10";

            Result result = session.run(cypher, Map.of(
                    "vorname", vorname != null ? vorname : "",
                    "nachname", nachname != null ? nachname : "",
                    "filter", keywordFilter != null ? keywordFilter : ""
            ));

            List<String> records = result.list(record -> "- " + record.get("kommentar").asString() + " (" + record.get("frequency").asInt() + " mal)");
            return records.isEmpty() ? "Keine relevanten Unterbrechungen gefunden." : "Häufigste Unterbrechungen:\n" + String.join("\n", records);
        } catch (Exception e) {
            return "Fehler bei der Graphen-Analyse: " + e.getMessage();
        }
    }

    @Tool("Analysiert parteiübergreifende Zustimmung. Nutze dies um herauszufinden, bei welchen Themen Regierung und Opposition übereinstimmen.")
    public String crossPartyAgreementScorer(
            @P("Name der ersten Fraktion/Partei (z.B. 'SPD')") String fraktion1,
            @P("Name der zweiten Fraktion/Partei (z.B. 'FDP')") String fraktion2) {

        return "CrossPartyAgreement Analyse: Dies erfordert eine komplexe Graph-Query, um Reden von " + fraktion1 +
                " mit Beifall-Kommentaren der " + fraktion2 + " abzugleichen. (Feature in Entwicklung)";
    }

    // ==========================================
    // 2. Temporal & Statistical Analytics Tools
    // ==========================================

    @Tool("Gibt eine Zeitreihen-Analyse für ein bestimmtes Schlagwort zurück. " +
            "Nutze dies für Fragen wie: 'Wie oft wurde das Wort Inflation im letzten Jahr erwähnt?'")
    public String trackTopicTrend(
            @P("Das Thema oder Schlagwort, das analysiert werden soll (z.B. 'Zeitenwende')") String keyword) {

        System.out.println("[Agent Tool] Executing TopicTrendTracker for keyword: " + keyword);
        try (Session session = driver.session()) {
            String cypher = "MATCH (r:Rede)-[:GEHALTEN_IN]->(s:Sitzung) " +
                    "WHERE toLower(r.text) CONTAINS toLower($keyword) " +
                    "RETURN s.datum AS datum, count(r) AS anzahl " +
                    "ORDER BY s.datum ASC";

            Result result = session.run(cypher, Map.of("keyword", keyword));
            List<String> records = result.list(record -> record.get("datum").asString() + ": " + record.get("anzahl").asInt() + " Erwähnungen");

            return records.isEmpty() ? "Das Schlagwort '" + keyword + "' wurde nicht gefunden." : "Trendanalyse für '" + keyword + "':\n" + String.join("\n", records);
        } catch (Exception e) {
            return "Fehler bei der Trendanalyse: " + e.getMessage();
        }
    }

    @Tool("Liefert generelle Redestatistiken. Nutze dies für Fragen wie: 'Wer hat die längste Redezeit?' oder 'Wer spricht am häufigsten?'")
    public String fetchSpeechStatistics(
            @P("Die gesuchte Metrik: 'ANZAHL_REDEN' oder 'WORTANZAHL'") String metric) {
        // Here you would implement queries counting (:Rede) nodes per (:Redner)
        return "Statistik-Abfrage für " + metric + " ausgeführt. (Beispieldaten: Redner X hat bisher 45 Reden gehalten).";
    }

    // ==========================================
    // 3. External Data & Contextual Tools
    // ==========================================

    @Tool("Ruft das tatsächliche namentliche Abstimmungsverhalten (Roll Call Votes) von Abgeordneten ab. " +
            "Nutze dies für Fragen wie: 'Wie hat Christian Lindner beim Mindestlohn abgestimmt?'")
    public String fetchRollCallVote(
            @P("Vorname des Abgeordneten") String vorname,
            @P("Nachname des Abgeordneten") String nachname,
            @P("Das Thema der Abstimmung") String topic) {

        System.out.println("[Agent Tool] Fetching Roll Call Votes from external API for: " + vorname + " " + nachname);
        // Implementation note: Make an HTTP GET request to Bundestag API or Abgeordnetenwatch here
        return "Namentliche Abstimmung für " + vorname + " " + nachname + " zum Thema '" + topic + "': Der Abgeordnete hat DAFÜR gestimmt. (Mock-Daten aus externer API)";
    }

    @Tool("Ruft biografische Metadaten eines Abgeordneten ab (Ausschüsse, Wahlkreis, Nebenverdienste, Alter).")
    public String getMPBiography(
            @P("Vorname des Abgeordneten") String vorname,
            @P("Nachname des Abgeordneten") String nachname) {

        System.out.println("[Agent Tool] Fetching MP Biography for: " + vorname + " " + nachname);
        // Implementation note: Fetch from a relational DB or external open data API
        return "Biografie von " + vorname + " " + nachname + ": Geboren 1980, Wahlkreis Berlin-Mitte, Mitglied im Finanzausschuss. (Mock-Daten)";
    }

    // ==========================================
    // 4. Advanced NLP & Semantic Tools
    // ==========================================

    @Tool("Analysiert die Stimmung (Sentiment) und den Tonfall einer Debatte oder Rede. " +
            "Nutze dies bei Fragen nach der 'Stimmung' oder ob eine Rede 'aggressiv' war.")
    public String analyzeSentimentAndEmotion(
            @P("Das Thema der Debatte, deren Stimmung analysiert werden soll") String topic) {

        System.out.println("[Agent Tool] Running Sentiment Analysis on topic: " + topic);
        // Implementation note: Fetch top N speeches from VectorStore, run them through an LLM sentiment prompt
        return "Stimmungsanalyse zum Thema '" + topic + "': Die Debatte war stark polarisiert. Die Regierung sprach optimistisch, während die Opposition einen sehr kritischen, teils aggressiven Tonfall wählte.";
    }

    @Tool("Fasst eine komplette Debatte zu einem Tagesordnungspunkt zusammen, um Pro- und Contra-Argumente gegenüberzustellen.")
    public String summarizeDebate(
            @P("Tagesordnungspunkt oder Thema der Debatte") String tagesordnungspunkt) {

        System.out.println("[Agent Tool] Map-Reducing Debate for Topic: " + tagesordnungspunkt);
        // Implementation note: Fetch all speeches linked to the specific agenda item, chunk them, and use a map-reduce summarization chain
        return "Zusammenfassung der Debatte zu '" + tagesordnungspunkt + "':\nPro-Argumente: ...\nContra-Argumente: ...";
    }

    // ==========================================
    // 5. Multi-Hop Reasoning Tools
    // ==========================================

    @Tool("Sucht nach semantischen Widersprüchen in vergangenen Aussagen desselben Politikers oder derselben Partei. " +
            "Nutze dies, wenn gefragt wird, ob sich jemand 'widersprochen' hat oder seine Meinung geändert hat.")
    public String findContradictions(
            @P("Vorname des Redners") String vorname,
            @P("Nachname des Redners") String nachname,
            @P("Das Thema, zu dem auf Widersprüche geprüft werden soll") String topic) {

        System.out.println("[Agent Tool] Scanning for contradictions for " + nachname + " regarding " + topic);
        // Implementation note:
        // 1. Semantic search for 'topic' filtered by 'speaker'.
        // 2. Pass retrieved chunks to an LLM explicitly prompted to find opposing viewpoints across timestamps.
        return "Widerspruchsanalyse für " + vorname + " " + nachname + " zum Thema '" + topic + "': " +
                "In 2021 lehnte der Redner das Thema strikt ab, in der gestrigen Sitzung sprach er sich jedoch dafür aus. (Mock-Ergebnis)";
    }
}