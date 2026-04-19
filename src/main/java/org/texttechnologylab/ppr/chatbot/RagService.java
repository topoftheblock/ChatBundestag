package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
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

    public RagService(Driver neo4jDriver) {
        this.openAiKey = System.getenv("OPENAI_API_KEY"); // Make sure to set this!

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
            // Grab the text of the speech
            String text = rede.getText();
            if (text == null || text.trim().isEmpty()) continue;

            // Add metadata so the LLM knows who is speaking
            Metadata metadata = new Metadata();
            metadata.put("redeId", rede.getId());
            if (rede.getRedner() != null) {
                metadata.put("speaker", rede.getRedner().getName());
            }

            documents.add(Document.from(text, metadata));
        }

        // Ingestor handles chunking the text and saving it to Neo4j automatically
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);
        System.out.println("Ingestion complete!");
    }

    /**
     * Builds and returns the conversational agent.
     */
    public ParliamentAssistant getAssistant() {
        // The LLM that will generate the final text
        OpenAiChatModel chatModel = OpenAiChatModel.withApiKey(openAiKey);

        // The retriever that searches Neo4j for the closest matching vectors
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // Fetch the top 3 most relevant speeches
                .minScore(0.75) // Minimum relevance score
                .build();

        return AiServices.builder(ParliamentAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // Remembers context
                .contentRetriever(retriever)
                .build();
    }
}