package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;
import org.neo4j.driver.Driver;
import org.texttechnologylab.ppr.model.interfaces.Rede;

import java.util.ArrayList;
import java.util.List;

public class RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final String openAiKey;
    // We now keep a reference to the driver to query the graph
    private final Driver neo4jDriver;

    public RagService(Driver neo4jDriver) {
        this.openAiKey = System.getenv("OPENAI_API_KEY");
        this.neo4jDriver = neo4jDriver; // Store for the Graph Retriever and Tools

        // 1. Initialize the Embedding Model (Converts text to vectors)
        this.embeddingModel = OpenAiEmbeddingModel.withApiKey(openAiKey);

        // 2. Initialize Neo4j as our Vector Store
        this.embeddingStore = Neo4jEmbeddingStore.builder()
                .driver(neo4jDriver)
                .dimension(1536) // Size of OpenAI text-embedding-ada-002
                .label("RedeSegment") // Node label in Neo4j
                .indexName("speeches_vector_index")
                .build();
    }

    /**
     * Call this once after parsing your XML files to push embeddings to Neo4j.
     */
    public void ingestSpeeches(List<Rede> speeches) {
        System.out.println("Generating embeddings and uploading to Neo4j...");
        List<Document> documents = new ArrayList<>();

        for (Rede rede : speeches) {
            String text = rede.getText();
            if (text == null || text.trim().isEmpty()) continue;

            Metadata metadata = new Metadata();
            metadata.put("redeId", rede.getId());
            // The metadata ID is crucial so our GraphRAG can anchor to the right node later!

            documents.add(Document.from(text, metadata));
        }

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);
        System.out.println("Ingestion complete!");
    }

    /**
     * Builds and returns the conversational agent using Graph RAG and Tools.
     */
    public ParliamentAssistant getAssistant() {
        OpenAiChatModel chatModel = OpenAiChatModel.withApiKey(openAiKey);

        // --- Using the Custom GraphRAG Retriever ---
        // Instead of the standard EmbeddingStoreContentRetriever, we use our own.
        GraphRAGRetriever retriever = new GraphRAGRetriever(
                embeddingStore,
                embeddingModel,
                neo4jDriver,
                3,     // Fetch the top 3 most relevant speeches
                0.75   // Minimum relevance score
        );

        // --- Using Graph Database Tools for Analytics ---
        // Allows the LLM to write and execute Cypher queries
        GraphDatabaseTools dbTools = new GraphDatabaseTools(neo4jDriver);

        return AiServices.builder(ParliamentAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(retriever)
                .tools(dbTools) // Inject the Cypher execution tool
                .build();
    }
}