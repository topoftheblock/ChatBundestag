# Parliamentary Protocol Parser & Advanced GraphRAG Assistant

A Java application for parsing XML protocols from the German Bundestag, loading them into an embedded Neo4j graph database, and enabling highly contextual, natural-language querying via an advanced **GraphRAG (Retrieval-Augmented Generation)** pipeline.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [GraphRAG Architecture](#graphrag-architecture)
    - [Three-Layered Intelligence](#three-layered-intelligence)
    - [How the Pipeline Works](#how-the-pipeline-works)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Execution Workflow](#execution-workflow)
- [Data Model](#data-model)
- [Known Data Issues](#known-data-issues)

---

## Overview

This project processes 214 XML protocol files from German Bundestag parliamentary sessions. It extracts structured information about sessions, speakers, speeches, and interjections, and loads everything into an embedded Neo4j graph database.

Beyond standard data ingestion, this application serves as an **Advanced AI GraphRAG system**. By combining OpenAI's vector embeddings with Neo4j's relational graph traversals, the AI assistant doesn't just read isolated text snippets — it understands the political context, the timeline, the speaker's party affiliation, and the real-time reactions (heckling/comments) from the parliament floor.

Example queries you can ask:
- *"Welche Position vertritt Olaf Scholz zum Thema Klimaschutz?"*
- *"Wie sind Friedrich Merz und Annalena Baerbock im Graph vernetzt?"*
- *"Wer sind die aktivsten Redner der Opposition?"*

---

## Key Features

| Feature | Description |
|---|---|
| **Advanced GraphRAG** | Custom LangChain4j integration combining semantic vector search with Cypher graph traversals for highly accurate AI answers. |
| **Graph Network Analytics** | Calculates Degree Centrality, detects cross-party interaction networks, and traces topic evolution over time. |
| **Embedded Graph Database** | Runs Neo4j locally with an explicitly enabled Bolt Connector (Port 7687) to support direct AI retrieval. |
| **Robust XML Parsing** | DOM-based parser with duplicate detection, caching, and comprehensive error handling. |
| **High Performance** | Batch operations via `UNWIND` and `MERGE` ensure idempotent, atomic data loading. |

---

## GraphRAG Architecture

Unlike standard RAG pipelines that only pass raw text chunks to an LLM, this application uses a **Hybrid GraphRAG** approach — combining semantic vector search with structural graph traversals and native graph algorithms to provide the AI with deep, layered context.

### Three-Layered Intelligence

**1. Semantic Vector Search (Textual RAG)**
Speeches are converted into 1536-dimensional vector embeddings using OpenAI's `text-embedding-ada-002` and stored in a Neo4j Vector Index. This allows the assistant to find relevant content based on *meaning and intent*, not just keywords.

**2. Structural Graph RAG (Contextual RAG)**
By anchoring retrieved text segments to their parent `Rede`, `Redner`, and `Sitzung` nodes, the assistant understands the full environment of a statement:
- **Who** said it? (Party affiliation, role)
- **When** was it said? (Chronological context)
- **What was the reaction?** (Comments and heckling related to a specific speech)

**3. Graph Algorithm Tools (Analytical RAG)**
A specialized toolset performs real-time network analysis on the parliamentary graph:
- **Shortest Path:** Discovering connections between politicians through shared sessions or topics
- **Centrality Measures:** Identifying the most influential or active speakers using Degree Centrality
- **Complex Relations:** Understanding interjection dynamics — "who interrupted whom"

### How the Pipeline Works

1. **Vector Ingestion** — Speeches are embedded and stored in Neo4j with their `redeId` as metadata.

2. **Semantic Search** — On a user query (e.g., *"How did the CDU/CSU position themselves on economic policy before 2025?"*), the system embeds the question and fetches the top 3 most semantically relevant speeches.

3. **Graph Traversal (The Secret Sauce)** — A custom `GraphRAGRetriever` uses the matched `redeId` to run a native Cypher traversal across the surrounding graph:
    - `(r:Rede)-[:GEHALTEN_IN]->(s:Sitzung)` → Extracts the exact **Date**
    - `(redner:Redner)-[:HAT_GESPROCHEN]->(r:Rede)` → Extracts **Speaker Name** and **Party** (`Fraktion`)
    - `(r:Rede)-[:BEINHALTET]->(k:Kommentar)` → Extracts any **Interruptions/Heckling** during that speech

4. **Context Assembly & Generation** — The retrieved text alongside its rich graph context is formatted into a "Protocol Excerpt" and sent to the LLM. The resulting answer is highly accurate, context-aware, and resistant to typical RAG hallucinations.

---

## Project Structure

```
org.texttechnologylab.ppr/
├── Main.java                    # App entry point (coordinates parsing, DB, analytics, AI)
├── AppFactory.java              # Singleton factory for services
├── parser/
│   └── XMLParser.java           # DOM-based XML parser with caching
├── chatbot/
│   ├── RagService.java          # Initializes embeddings, vector store, and AI services
│   ├── GraphRAGRetriever.java   # Custom LangChain4j retriever executing Cypher traversals
│   └── ParliamentAssistant.java # AI agent interface with system prompts
├── model/
│   ├── interfaces/              # Data model contracts (Sitzung, Rede, Redner, etc.)
│   └── *.Impl.java              # Concrete implementations providing toNode() logic
└── db/
    ├── DatabaseConnection.java
    └── Neo4jConnection.java     # Neo4j implementation (Embedded + Bolt + Graph Algorithms)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **AI Framework** | LangChain4j |
| **Database** | Neo4j (Graph Database & Vector Store) |
| **LLM** | OpenAI GPT-4o-mini |
| **Embeddings** | OpenAI `text-embedding-ada-002` |

---

## Getting Started

### Prerequisites

- JDK 21
- Apache Maven
- Neo4j 5.13.0 (included as a Maven dependency)
- OpenAI API Key (**required** for the RAG assistant)

### Build & Run

1. Clone the repository and place your XML protocol files in `src/main/resources/`.

2. Export your OpenAI key to your environment:

```bash
export OPENAI_API_KEY="sk-your-api-key-here"
```

3. Build and run the application:

```bash
mvn clean package
mvn exec:java -Dexec.mainClass="org.texttechnologylab.ppr.Main"
```

The embedded Neo4j database is created automatically at `target/neo4j-db`. The application will process the data, print graph analytics to the console, and drop you into the interactive Parliament AI Chat.

---

## Execution Workflow

1. **Parse XMLs** — Transforms XML files into Java objects, detecting duplicates and caching speakers.
2. **Start Database** — Initializes the embedded Neo4j instance and opens the Bolt port (7687) for LangChain4j.
3. **Load Graph** — Clears old data, creates constraints, and uses batch Cypher queries (`UNWIND`/`MERGE`) to load nodes and relationships.
4. **Run Analytics** — Executes statistical and advanced graph network algorithms.
5. **Start GraphRAG AI** — Embeds speeches into the Vector Store and starts the interactive CLI chat loop.

---

## Data Model

The graph is composed of the following structure:

**Nodes:** `Redner` (Speaker), `Rede` (Speech), `Sitzung` (Session), `Kommentar` (Comment/Interjection)

**Relationships:** `HAT_GESPROCHEN`, `GEHALTEN_IN`, `BEINHALTET`

```
(Redner)-[:HAT_GESPROCHEN]->(Rede)-[:GEHALTEN_IN]->(Sitzung)
                                  (Rede)-[:BEINHALTET]->(Kommentar)
```

---

## Demo

![screen1](Screenshot%202026-04-23%20at%2015.33.51.png)
This screenshot showcases the user-friendly web interface where users can interact directly with the Parliament Assistant. Users can ask natural language questions regarding politicians, specific speeches, or debate topics. The assistant processes these queries and returns contextually accurate answers based on the parsed parliamentary protocols.
![screen2](Screenshot%202026-04-23%20at%2015.51.18.png)
Here is a look under the hood at the Neo4j graph database visualization. The parsed XML data is structured into interconnected nodes such as `Redner` (Speakers), `Rede` (Speeches), and `Sitzung` (Sessions). These are linked by structural relationships (e.g., `HAT_GESPROCHEN`, `GEHALTEN_IN`), allowing the AI to traverse complex connections and retrieve highly relevant context that standard text searches might miss.
This terminal output demonstrates the application's backend processing using LangChain4j. It highlights the GraphRAG pipeline in action:
1. The user's query is embedded and searched within the vector space.
2. The Agent fetches the structural graph context.
3. The LLM generates and executes a custom Cypher query (e.g., `MATCH (redner:Redner {vorname: 'Markus', nachname: 'Söder'})...`) to fetch precise, grounded data directly from the Neo4j database to formulate the final answer.