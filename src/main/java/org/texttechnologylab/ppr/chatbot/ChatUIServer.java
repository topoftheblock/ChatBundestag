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

        server.createContext("/", new UIHandler());
        server.createContext("/api/chat", new ChatHandler(assistant));

        server.setExecutor(null);
        server.start();
        System.out.println("==================================================");
        System.out.println("Web UI successfully started!");
        System.out.println("Open your browser and navigate to: http://localhost:" + port);
        System.out.println("==================================================");
    }

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
                    "<html lang=\"de\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>WhatsApp Web</title>\n" +
                    "    \n" +
                    "    <script src=\"https://cdn.jsdelivr.net/npm/marked/marked.min.js\"></script>\n" +
                    "    <style>\n" +
                    "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #d1d7db; display: flex; justify-content: center; padding: 20px; margin: 0; height: 100vh; box-sizing: border-box; }\n" +
                    "        #chat-container { width: 100%; max-width: 800px; background: #e5ddd5; box-shadow: 0 1px 3px rgba(0,0,0,0.1); display: flex; flex-direction: column; overflow: hidden; height: 100%; }\n" +
                    "        #header { background: #00a884; color: white; padding: 10px 16px; display: flex; align-items: center; font-size: 16px; font-weight: 500; }\n" +
                    "        #header .avatar { width: 40px; height: 40px; background: #dfe5e7; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 20px; margin-right: 15px; }\n" +
                    "        #messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; background-image: url('https://user-images.githubusercontent.com/15075759/28719144-86dc0f70-73b1-11e7-911d-60d70fcded21.png'); background-color: #efeae2; }\n" +
                    "        .message { margin-bottom: 12px; padding: 6px 7px 8px 9px; border-radius: 7.5px; max-width: 65%; line-height: 19px; font-size: 14.2px; position: relative; box-shadow: 0 1px 0.5px rgba(11,20,26,.13); word-wrap: break-word; }\n" +
                    "        .user { background: #dcf8c6; color: #111b21; align-self: flex-end; border-top-right-radius: 0; }\n" +
                    "        .bot { background: #ffffff; color: #111b21; align-self: flex-start; border-top-left-radius: 0; }\n" +
                    "        .user::after { content: ''; position: absolute; top: 0; right: -8px; width: 0; height: 0; border-top: 10px solid #dcf8c6; border-right: 10px solid transparent; }\n" +
                    "        .bot::after { content: ''; position: absolute; top: 0; left: -8px; width: 0; height: 0; border-top: 10px solid #ffffff; border-left: 10px solid transparent; }\n" +
                    "        /* CSS für die Markdown-Formatierungen in den Bubbles */\n" +
                    "        .message p { margin: 0 0 5px 0; }\n" +
                    "        .message p:last-child { margin: 0; }\n" +
                    "        .message ul, .message ol { margin: 5px 0; padding-left: 20px; }\n" +
                    "        .message strong { font-weight: bold; }\n" +
                    "        .message code { font-family: monospace; background: rgba(0,0,0,0.05); padding: 2px 4px; border-radius: 4px; }\n" +
                    "        #input-area { display: flex; padding: 10px 16px; background: #f0f2f5; align-items: center; }\n" +
                    "        input { flex: 1; padding: 12px 15px; border: none; border-radius: 8px; outline: none; font-size: 15px; background: #ffffff; margin-right: 10px; }\n" +
                    "        button { background: transparent; color: #54656f; border: none; font-size: 20px; cursor: pointer; display: flex; justify-content: center; align-items: center; font-weight: bold; }\n" +
                    "        .loading { align-self: center; background: #e2f5fd; padding: 5px 12px; border-radius: 12px; font-size: 12.5px; color: #54656f; margin-bottom: 10px; box-shadow: 0 1px 0.5px rgba(11,20,26,.13); display: none; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div id=\"chat-container\">\n" +
                    "        <div id=\"header\">\n" +
                    "            <div class=\"avatar\">🏛️</div>\n" +
                    "            <div>Parliament AI</div>\n" +
                    "        </div>\n" +
                    "        <div id=\"messages\">\n" +
                    "            <div class=\"message bot\">Hallo! Ich bin dein KI-Experte für den Deutschen Bundestag. Was möchtest du wissen?</div>\n" +
                    "        </div>\n" +
                    "        <div id=\"loading\" class=\"loading message\">schreibt...</div>\n" +
                    "        <div id=\"input-area\">\n" +
                    "            <input type=\"text\" id=\"user-input\" placeholder=\"Tippe eine Nachricht\" autocomplete=\"off\" onkeypress=\"if(event.key === 'Enter') sendMessage()\">\n" +
                    "            <button onclick=\"sendMessage()\">➤</button>\n" +
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
                    "            scrollToBottom();\n" +
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
                    "                appendMessage('bot', 'Fehler bei der Verbindung zum Server.');\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        function appendMessage(sender, text) {\n" +
                    "            const msgsDiv = document.getElementById('messages');\n" +
                    "            const msgDiv = document.createElement('div');\n" +
                    "            msgDiv.className = 'message ' + sender;\n" +
                    "            // Hier wird das Markdown der KI live in HTML konvertiert\n" +
                    "            msgDiv.innerHTML = marked.parse(text);\n" +
                    "            msgsDiv.appendChild(msgDiv);\n" +
                    "            scrollToBottom();\n" +
                    "        }\n" +
                    "\n" +
                    "        function scrollToBottom() {\n" +
                    "            const msgsDiv = document.getElementById('messages');\n" +
                    "            msgsDiv.scrollTop = msgsDiv.scrollHeight;\n" +
                    "        }\n" +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";
        }
    }

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

                String aiResponse = assistant.chat(userQuery);

                t.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                byte[] responseBytes = aiResponse.getBytes(StandardCharsets.UTF_8);
                t.sendResponseHeaders(200, responseBytes.length);

                OutputStream os = t.getResponseBody();
                os.write(responseBytes);
                os.close();
            } else {
                t.sendResponseHeaders(405, -1);
            }
        }
    }
}