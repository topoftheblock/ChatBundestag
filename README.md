# Parliamentary Protocol Parser & AI Assistant

> A Java application for parsing XML protocols from the German Bundestag, loading them into a Neo4j graph database, and enabling natural-language querying via an AI-powered RAG pipeline.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Execution Workflow](#execution-workflow)
- [Statistical Analyses](#statistical-analyses)
- [AI Assistant (RAG)](#ai-assistant-rag)
- [Known Data Issues](#known-data-issues)
- [Processing Statistics](#processing-statistics)

---

## Overview

This project processes **214 XML protocol files** from German Bundestag parliamentary sessions. It extracts structured information about sessions, speakers, speeches, and interjections, loads everything into an embedded Neo4j graph database, and provides automated statistical analysis. A conversational AI assistant powered by LangChain4j and OpenAI allows natural-language querying of the ingested data.

> **Note:** Code comments indicate where each assignment sub-question is solved.

---

## Key Features

| Feature | Description |
|---|---|
| **Robust XML Parsing** | DOM-based parser with duplicate detection and error handling |
| **Graph Database Integration** | Embedded Neo4j with optimized batch loading via `UNWIND` and `MERGE` |
| **AI Parliamentary Assistant** | RAG pipeline for natural-language questions over speech data |
| **Statistical Analysis** | Automated Cypher queries for speech patterns and comment frequencies |
| **High Performance** | Batch operations ensure idempotent, atomic data loading |

---

## Architecture

### Design Patterns

**Interface-Driven Design**
All core components (`Sitzung`, `Rede`, `Redner`, `DatabaseConnection`) are defined by interfaces, decoupling application logic from specific implementations.

**Factory & Singleton Pattern**
`AppFactory` serves as a singleton for accessing core services such as `XMLParser` and creating `DatabaseConnection` instances.

**Model-to-Node Mapping**
Each model class exposes a `toNode()` method, making it responsible for its own database representation.

### Efficient Batch Loading

Data is loaded in batches using Neo4j's `UNWIND` and `MERGE` commands, ensuring:
- High throughput
- Data idempotency (no duplicates on re-runs)
- Atomic operations

---

## Project Structure

```
org.texttechnologylab.ppr/
├── Main.java                      # Application entry point
├── AppFactory.java                # Singleton factory for services
├── parser/
│   └── XMLParser.java             # DOM-based XML parser with caching
├── model/
│   ├── interfaces/                # Data model contracts
│   │   ├── Sitzung.java
│   │   ├── Rede.java
│   │   ├── Redner.java
│   │   ├── Abgeordneter.java
│   │   └── Kommentar.java
│   └── *.Impl.java                # Concrete implementations
└── db/
    ├── DatabaseConnection.java    # Database operation interface
    └── Neo4jConnection.java       # Neo4j-specific implementation
```

---

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Apache Maven**
- **Neo4j 5.13.0** (included as a Maven dependency — no separate installation required)

### Build & Run

```bash
# Clone the repository
git clone <repository-url>
cd <project-directory>

# Build with Maven
mvn clean package

# Run the application
mvn exec:java -Dexec.mainClass="org.texttechnologylab.ppr.Main"
```

XML protocol files should be placed in `src/main/resources/`. The embedded Neo4j database is created automatically at `target/neo4j-db`.

---

## Execution Workflow

The application runs the following pipeline on startup:

1. **Find Files** — Scans `src/main/resources/` for `.xml` files
2. **Parse XMLs** — Transforms XML into Java model objects (`Sitzung`, `Rede`, `Redner`, etc.)
    - Uses a `rednerCache` to deduplicate speaker objects
    - Detects and skips duplicate sessions
3. **Start Database** — Initializes the embedded Neo4j instance at `target/neo4j-db`
4. **Clear Database** — Runs `MATCH (n) DETACH DELETE n` to remove stale data
5. **Create Constraints** — Applies `UNIQUE` constraints on `Sitzung`, `Redner`, and `Rede` nodes
6. **Load Nodes**
    - `ladeSitzungen()` — Parliamentary sessions
    - `ladeRedner()` — Speakers with `:Abgeordneter` labels
    - `ladeRedenUndKommentare()` — Speeches and associated comments
7. **Create Relationships** — Establishes `[:HAT_GESPROCHEN]`, `[:GEHALTEN_IN]`, and `[:BEINHALTET]` edges
8. **Run Statistics** — Executes analytical Cypher queries and prints results
9. **Shutdown** — Safely closes the embedded database

---

## Statistical Analyses

> **Note:** Party-level statistics include only *Abgeordnete* (parliament members with official party affiliation in the XML). Politicians marked as `"Keine Fraktion"` are excluded from party aggregations but still appear in individual speaker statistics.

### (4a) Average Speech Length (in characters)

Calculated per representative (Top 10) and per party.

```
Pro Redner (Top 10):
| vorname   | nachname | Partei         | avgLaenge |
|-----------|----------|----------------|-----------|
| Markus    | Söder    | Keine Fraktion | 10850,00  |
| Friedrich | Merz     | CDU/CSU        | 10593,62  |
```

### (4b) Average Comment Frequency (per speech)

Calculated per representative (Top 10) and per party.

```
Pro Partei:
| Partei                | avgKommentare |
|-----------------------|---------------|
| CDU/CSU               | 9,11          |
| BÜNDNIS 90/DIE GRÜNEN | 8,73          |
```

### (4c) Longest Session

Measured both by duration (minutes) and total text length (characters).

```
Längste Sitzung (nach Zeit):
| wp | nr  | datum      | dauerMinuten |
|----|-----|------------|--------------|
| 20 | 210 | 2025-01-30 | 1039         |
```

---

## AI Assistant (RAG)

The project includes a **Retrieval-Augmented Generation (RAG)** pipeline that answers natural-language questions grounded in the actual parliamentary speeches — not general knowledge.

### How It Works

1. **Ingestion** — Speeches are converted to high-dimensional vector embeddings using OpenAI and stored in a Neo4j Vector Index.
2. **Retrieval** — A user query (e.g., *"What was discussed about nuclear energy?"*) is embedded and matched against the most semantically similar speeches in the database.
3. **Generation** — The AI reads the retrieved speeches and generates a concise, cited answer naming the original speakers.

### Configuration

To use the AI assistant, set your OpenAI API key as an environment variable before running:

```bash
export OPENAI_API_KEY="sk-..."
```

---

## Known Data Issues

### 1. Duplicate XML File
One XML file contains duplicate session data (Session 207 from `7.xml`). The parser automatically detects and skips this duplicate.

### 2. Shared Speaker IDs
Two different politicians share the same speaker ID in the source XML:

| Speaker | File |
|---|---|
| Alexander Föhr | `91.xml` |
| Dirk-Ulrich Mende | `116.xml` |

Because `MERGE` operates on speaker ID, only one of these speakers will be fully represented in the database. This is a data quality issue in the source files and may cause attribution errors for these two individuals.

---

## Processing Statistics

From the most recent full run:

| Metric | Value |
|---|---|
| XML files processed | 214 |
| Sessions loaded | 213 *(1 duplicate skipped)* |
| Speakers loaded | 806 *(with Abgeordneten labels)* |
| Speeches & comments | Successfully loaded with all relationships |