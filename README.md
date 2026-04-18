A Java-based application for parsing XML parliamentary protocols from the German Bundestag, transforming them into a graph structure, and analyzing them using an embedded Neo4j database.

## Overview

This project processes 214 XML protocol files from parliamentary sessions, extracts structured information about sessions, speakers, speeches, and comments, and loads them into a Neo4j graph database for advanced analysis. The application automatically performs statistical analyses on speech patterns, comment frequencies, and session durations.

**Disclaimer**: The code contains comments indicating where each assignment subquestion is solved.

## Key Features

- **Robust XML Parsing**: DOM-based parser with duplicate detection and error handling
- **Graph Database Integration**: Embedded Neo4j database with optimized batch loading
- **Statistical Analysis**: Automated Cypher queries for speech and session analytics
- **High Performance**: Batch operations using `UNWIND` and `MERGE` for idempotent data loading

## Project Architecture

### Design Patterns

#### Interface-Driven Design
All core components (`Sitzung`, `Rede`, `Redner`, `DatabaseConnection`) are defined by interfaces, decoupling application logic from specific implementations.

#### Factory & Singleton Pattern
The `AppFactory` class serves as a Singleton for accessing core services like `XMLParser` and creating `DatabaseConnection` instances.

#### Model-to-Node Mapping
Each model class includes a `toNode()` method, making classes responsible for their own database representation.

### Efficient Batch Loading
Data is loaded in batches using Neo4j's `UNWIND` and `MERGE` commands, ensuring:
- High performance
- Data idempotency (no duplicates)
- Atomic operations

## Project Structure

```
org.texttechnologylab.ppr/
├── Main.java                    # Application entry point
├── AppFactory.java              # Singleton factory for services
├── parser/
│   └── XMLParser.java           # DOM-based XML parser with caching
├── model/
│   ├── interfaces/              # Data model contracts
│   │   ├── Sitzung.java
│   │   ├── Rede.java
│   │   ├── Redner.java
│   │   ├── Abgeordneter.java
│   │   └── Kommentar.java
│   └── *.Impl.java              # Concrete implementations
└── db/
    ├── DatabaseConnection.java   # Database operation interface
    └── Neo4jConnection.java      # Neo4j-specific implementation
```

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Apache Maven** (for dependency management)
- **Neo4j 5.13.0** (included as Maven dependency)

## Execution Workflow

The application executes the following pipeline:

1.  Find Files: Scans `src/main/resources/` for `.xml` files
2.  Parse XMLs: Transforms XML into Java object models (Sitzung, Rede, Redner, etc.)
   - Implements intelligent `rednerCache` to avoid duplicate speaker objects
   - Detects and skips duplicate sessions
3.  Start Database: Initializes embedded Neo4j at `target/neo4j-db`
4.  Clear Database: Runs `MATCH (n) DETACH DELETE n` to remove old data
5.  Create Constraints: Applies UNIQUE constraints on Sitzung, Redner, and Rede nodes
6.  Load Nodes**:
   - `ladeSitzungen()` - Loads all parliamentary sessions
   - `ladeRedner()` - Loads speakers and sets `:Abgeordneter` labels
   - `ladeRedenUndKommentare()` - Loads speeches and comments
7.  Create Relationships: Establishes `[:HAT_GESPROCHEN]`, `[:GEHALTEN_IN]`, and `[:BEINHALTET]` connections
8.  Run Statistics: Executes analytical queries and prints results
9.  Shutdown: Safely closes the embedded Neo4j database

## Statistical Analyses
**Disclaimer**: Party-level statistics only include Abgeordnete (parliament members with official party affiliation in the XML). Politicians marked as "Keine Fraktion" are excluded from party aggregations.These excluded politicians still appear in individual speaker statistics (like Markus Söder, Eva Högl, etc.)

The application automatically performs and displays:

### (4a) Average Speech Length (in characters)
- **Per Representative** (Top 10)
- **Per Party**

Example output:
```
Pro Redner (Top 10):
| vorname   | nachname   | Partei         | avgLaenge |
|-----------|------------|----------------|-----------|
| Markus    | Söder      | Keine Fraktion | 10850,00  |
| Friedrich | Merz       | CDU/CSU        | 10593,62  |
```

### (4b) Average Comment Frequency (per speech)
- **Per Representative** (Top 10)
- **Per Party**

Example output:
```
Pro Partei:
| Partei                | avgKommentare |
|-----------------------|---------------|
| CDU/CSU               | 9,11          |
| BÜNDNIS 90/DIE GRÜNEN | 8,73          |
```

### (4c) Longest Session
- **By duration** (in minutes)
- **By total text length** (characters)

Example output:
```
Längste Sitzung (nach Zeit):
| wp | nr  | datum      | dauerMinuten |
|----|-----|------------|--------------|
| 20 | 210 | 2025-01-30 | 1039         |
```

## Processing Statistics

From the last execution:
- **Files processed**: 214 XML files
- **Sessions loaded**: 213 (1 duplicate skipped)
- **Speakers loaded**: 806 (with Abgeordneten labels)
- **Speeches and comments**: Successfully loaded with relationships
### Known Data Issues

1. **Duplicate XML File**: One XML file contains duplicate session data (Session 207 from `7.xml`). The parser automatically detects and skips this duplicate during processing.

2. **Shared Speaker IDs**: Two different politicians share the same ID in the source data:
   - **Alexander Föhr** (in `91.xml`)
   - **Dirk-Ulrich Mende** (in `116.xml`)
   
   Both are assigned the same speaker ID in the XML files, which violates the uniqueness constraint. Due to the `MERGE` operation on speaker IDs, only one of these speakers will be properly represented in the database, potentially causing attribution errors.