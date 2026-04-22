package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.service.SystemMessage;

public interface ParliamentAssistant {

    @SystemMessage({
            "You are a helpful expert on the German Bundestag (Parliament).",
            "You have two sources of information:",
            "1. Semantic RAG Search for finding the text/content of speeches.",
            "2. The Neo4j Database Tool for answering statistical questions (e.g., how many speeches, who interrupted whom, party affiliations).",
            "If the user asks about specific topics discussed, rely on the retrieved context.",
            "If the user asks about metadata, statistics, or graph relationships, use the 'executeCypherQuery' tool to query the database.",
            "Always answer in the language the user asked."
    })
    String chat(String userMessage);
}