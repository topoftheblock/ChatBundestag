package org.texttechnologylab.ppr.chatbot;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ChatUIServer {

    private final ParliamentAssistant assistant;

    public ChatUIServer(ParliamentAssistant assistant) {
        this.assistant = assistant;
    }

    public void startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Serve the HTML UI
        server.createContext("/", new UIHandler());

        // API endpoint to process chat messages
        server.createContext("/api/chat", new ChatHandler(assistant));

        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("==================================================");
        System.out.println("Web UI successfully started!");
        System.out.println("Open your browser and navigate to: http://localhost:" + port);
        System.out.println("==================================================");
    }

    // Handles the frontend HTML page
    static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = getHtml();
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        private String getHtml() {
            return "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Parliament AI Assistant</title>\n" +
                    "    <style>\n" +
                    "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; display: flex; justify-content: center; padding: 40px; margin: 0;}\n" +
                    "        #chat-container { width: 100%; max-width: 600px; background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); display: flex; flex-direction: column; overflow: hidden; }\n" +
                    "        #header { background: #1a237e; color: white; padding: 20px; text-align: center; font-size: 20px; font-weight: bold; }\n" +
                    "        #messages { height: 500px; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; background: #fafafa; }\n" +
                    "        .message { margin-bottom: 15px; padding: 12px 16px; border-radius: 20px; max-width: 80%; line-height: 1.5; font-size: 15px; }\n" +
                    "        .user { background: #3f51b5; color: white; align-self: flex-end; border-bottom-right-radius: 4px; }\n" +
                    "        .bot { background: #e0e0e0; color: black; align-self: flex-start; border-bottom-left-radius: 4px; }\n" +
                    "        #input-area { display: flex; padding: 15px; background: white; border-top: 1px solid #eee; }\n" +
                    "        input { flex: 1; padding: 12px; border: 1px solid #ccc; border-radius: 24px; outline: none; font-size: 15px; padding-left: 15px; }\n" +
                    "        button { padding: 10px 20px; margin-left: 10px; background: #ff4081; color: white; border: none; border-radius: 24px; cursor: pointer; font-weight: bold; transition: background 0.3s; }\n" +
                    "        button:hover { background: #e91e63; }\n" +
                    "        .loading { align-self: flex-start; color: #888; font-style: italic; font-size: 14px; display: none; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div id=\"chat-container\">\n" +
                    "        <div id=\"header\">🏛️ Parliament GraphRAG Assistant</div>\n" +
                    "        <div id=\"messages\">\n" +
                    "            <div class=\"message bot\">Hello! I am your AI expert on the German Bundestag. Ask me anything about the parliamentary protocols!</div>\n" +
                    "        </div>\n" +
                    "        <div id=\"loading\" class=\"loading message\">AI is querying Neo4j and thinking...</div>\n" +
                    "        <div id=\"input-area\">\n" +
                    "            <input type=\"text\" id=\"user-input\" placeholder=\"e.g., Wer hat über Steuern gesprochen?\" onkeypress=\"if(event.key === 'Enter') sendMessage()\">\n" +
                    "            <button onclick=\"sendMessage()\">Send</button>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "\n" +
                    "    <script>\n" +
                    "        async function sendMessage() {\n" +
                    "            const inputField = document.getElementById('user-input');\n" +
                    "            const text = inputField.value.trim();\n" +
                    "            if (!text) return;\n" +
                    "            \n" +
                    "            appendMessage('user', text);\n" +
                    "            inputField.value = '';\n" +
                    "            document.getElementById('loading').style.display = 'block';\n" +
                    "\n" +
                    "            try {\n" +
                    "                const response = await fetch('/api/chat', {\n" +
                    "                    method: 'POST',\n" +
                    "                    body: text\n" +
                    "                });\n" +
                    "                const botText = await response.text();\n" +
                    "                document.getElementById('loading').style.display = 'none';\n" +
                    "                appendMessage('bot', botText);\n" +
                    "            } catch (error) {\n" +
                    "                document.getElementById('loading').style.display = 'none';\n" +
                    "                appendMessage('bot', 'Error connecting to the server.');\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        function appendMessage(sender, text) {\n" +
                    "            const msgsDiv = document.getElementById('messages');\n" +
                    "            const msgDiv = document.createElement('div');\n" +
                    "            msgDiv.className = 'message ' + sender;\n" +
                    "            msgDiv.innerText = text;\n" +
                    "            msgsDiv.appendChild(msgDiv);\n" +
                    "            msgsDiv.scrollTop = msgsDiv.scrollHeight;\n" +
                    "        }\n" +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";
        }
    }

    // API Handler for the RAG queries
    static class ChatHandler implements HttpHandler {
        private final ParliamentAssistant assistant;

        public ChatHandler(ParliamentAssistant assistant) {
            this.assistant = assistant;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String userQuery = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                System.out.println("UI received query: " + userQuery);

                // Call the LangChain4j RAG Agent
                String aiResponse = assistant.chat(userQuery);

                t.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                byte[] responseBytes = aiResponse.getBytes(StandardCharsets.UTF_8);
                t.sendResponseHeaders(200, responseBytes.length);

                OutputStream os = t.getResponseBody();
                os.write(responseBytes);
                os.close();
            } else {
                t.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }
}