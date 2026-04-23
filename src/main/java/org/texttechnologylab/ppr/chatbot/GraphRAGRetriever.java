package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom LangChain4j Retriever that combines Vector Similarity Search
 * with Graph Database Traversals to build rich context.
 */
public class GraphRAGRetriever implements ContentRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final Driver neo4jDriver;
    private final int maxResults;
    private final double minScore;

    public GraphRAGRetriever(EmbeddingStore<TextSegment> embeddingStore,
                             EmbeddingModel embeddingModel,
                             Driver neo4jDriver,
                             int maxResults,
                             double minScore) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.neo4jDriver = neo4jDriver;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public List<Content> retrieve(Query query) {
        System.out.println("\n[GraphRAG] 1. Embedde Suchanfrage: " + query.text());
        dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(query.text()).content();

        System.out.println("[GraphRAG] 2. Führe Vektor-Suche in Neo4j aus...");
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults)
                        .minScore(minScore)
                        .build()
        ).matches();

        List<Content> enrichedContents = new ArrayList<>();

        System.out.println("[GraphRAG] 3. Hole strukturellen Graphen-Kontext für gefundene Reden...");
        try (Session session = neo4jDriver.session()) {
            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();

                // Hole die beim Ingest gespeicherte ID
                String redeId = segment.metadata().getString("redeId");

                if (redeId == null) {
                    enrichedContents.add(Content.from(segment.text()));
                    continue;
                }

                // --- CYPHER GRAPH TRAVERSAL ---
                // Hier holen wir Datum, Klarnamen und Fraktion aus dem Graphen
                String cypher = "MATCH (r:Rede {id: $redeId}) " +
                        "OPTIONAL MATCH (r)-[:GEHALTEN_IN]->(s:Sitzung) " +
                        "OPTIONAL MATCH (redner:Redner)-[:HAT_GESPROCHEN]->(r) " +
                        "OPTIONAL MATCH (r)-[:BEINHALTET]->(k:Kommentar) " +
                        "RETURN s.datum AS datum, " +
                        "       redner.vorname + ' ' + redner.nachname AS speakerName, " +
                        "       redner.fraktion AS fraktion, " +
                        "       collect(k.text) AS kommentare";

                Result result = session.run(cypher, Values.parameters("redeId", redeId));

                if (result.hasNext()) {
                    Record record = result.next();
                    String datum = record.get("datum").isNull() ? "Unbekanntes Datum" : record.get("datum").asLocalDate().toString();
                    String speaker = record.get("speakerName").isNull() ? "Unbekannter Redner" : record.get("speakerName").asString();
                    String fraktion = record.get("fraktion").isNull() ? "Keine Fraktion" : record.get("fraktion").asString();
                    List<Object> comments = record.get("kommentare").asList();

                    // --- ZUSAMMENBAU DES KONTEXTES FÜR DIE KI ---
                    // Diese klaren Labels (REDNER:, DATUM:) helfen der KI, die IDs zu ignorieren
                    StringBuilder enrichedText = new StringBuilder();
                    enrichedText.append("--- PROTOKOLL-AUSZUG ---\n");
                    enrichedText.append("REDNER: ").append(speaker).append("\n");
                    enrichedText.append("FRAKTION: ").append(fraktion).append("\n");
                    enrichedText.append("DATUM: ").append(datum).append("\n");
                    enrichedText.append("TEXT: \"").append(segment.text()).append("\"\n");

                    if (!comments.isEmpty() && comments.get(0) != null) {
                        enrichedText.append("\nUnterbrechungen / Zwischenrufe aus dem Plenum während dieser Rede:\n");
                        for (Object comment : comments) {
                            if (comment != null) enrichedText.append("- ").append(comment.toString()).append("\n");
                        }
                    }
                    enrichedText.append("------------------------\n");

                    // Füge den angereicherten Text zur Liste für das LLM hinzu
                    enrichedContents.add(Content.from(enrichedText.toString()));
                } else {
                    enrichedContents.add(Content.from(segment.text()));
                }
            }
        }

        return enrichedContents;
    }
}