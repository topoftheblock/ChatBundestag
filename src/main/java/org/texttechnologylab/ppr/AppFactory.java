package org.texttechnologylab.ppr;

import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.db.Neo4jConnection;
import org.texttechnologylab.ppr.parser.XMLParser;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Implementiert das Singletonpattern und Factory-Pattern, wie gefordert in Übungblatt, für den zentralen Zugriff auf
 * wichtigsten komponenten der Anwendung, wie denXMLParser} und die  DatabaseConnection}.
 * (Hier löse ich die Anforderungen für Aufgabe 2b)
 */
// Aufgabe 2b: Nutzen des Konzept einer Factory
public class AppFactory {

    private static AppFactory instance;
    private final XMLParser parser;

    /**
     * Privater Konstruktor,
     * Initialisiert den XMLParser.
     *
     * RuntimeException wenn der xMLParser nicht initialisiert werden kann.
     */
    private AppFactory() {
        try {
            this.parser = new XMLParser();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Fehler: XML-Parser konnte nicht initialisiert werden.", e);
        }
    }

    /**
     * return die global eindeutige Instanz der AppFactory zurück.
     * Die Instanz der {@code AppFactory}.
     */
    public static synchronized AppFactory getInstance() {
        if (instance == null) {
            instance = new AppFactory();
        }
        return instance;
    }

    /**
     * Gibt die Instance des XMLParser zurück.
     */
    public XMLParser getParser() {
        return this.parser;
    }

    /**
     * Erstellt und gibt eine neue. Datenbankverbindung zurück.
     * Encapsulates, die Implementierung (Neo4jConnection)hinter dem Interface (DatabaseConnection).
     *
     * Parameter: databasePath Der Datei,Pfad zum Verzeichnis der Neo4j-Datenbank.
     * Return: Eine neue, initialisierte Instanz von DatabaseConnection.
     */
    // Aufgabe 2b: Factory-Methode
    // Aufgabe 2c: Nutzt Interface-Prinzipien
    public DatabaseConnection createDatabaseConnection(String databasePath) {
        return new Neo4jConnection(databasePath);
    }
}