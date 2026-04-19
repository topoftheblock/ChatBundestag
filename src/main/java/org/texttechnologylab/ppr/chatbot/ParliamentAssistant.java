package org.texttechnologylab.ppr.chatbot;

import dev.langchain4j.service.SystemMessage;

public interface ParliamentAssistant {

    @SystemMessage({
            "You are a helpful expert on the German Bundestag (Parliament).",
            "You answer questions based ONLY on the provided protocol speeches.",
            "If the information is not in the speeches, say 'I don't know based on the protocols.'",
            "Always cite the speaker if you know it."
    })
    String chat(String userMessage);
}