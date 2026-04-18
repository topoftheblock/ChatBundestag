package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Kommentar;
import java.util.Map;

/**
 * Repräsentiert einen einzelnen Kommentar eg einen Zwischenruf oder Beifall).
 * (Hier löse ich die Anforderungen für Aufgabe 2a: Implementierung der Klassenstrukturen)
 * (Hier löse ich die Anforderungen für Aufgabe 2c: Implementierung des Interfaces)
 */
public class KommentarImpl implements Kommentar {

    // Aufgabe 2e: Größtmögliche Datenkapselung
    private String inhalt;

    /**
     * Create a new Kommentar(Hier löse ich die Anforderungen für Aufgabe 2a: geeignete Konstruktoren)
     */
    public KommentarImpl(String inhalt) {
        this.inhalt = inhalt;
    }

    /**
     * Gibt eine gekürzte, readableString des Kommentars zurück.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toString() Methode)
     */
    @Override
    public String toString() {
        return "Kommentar: \"" + (inhalt.length() > 50 ? inhalt.substring(0, 50) + "..." : inhalt) + "\"";
    }

    /**
     * Erstellt eine Map from  Eigenschaften für Neo4j.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toNode() Methode)
     */
    @Override
    public Map<String, Object> toNode() {
        return Map.of("text", this.inhalt);
    }

    @Override
    public Map<String, Object> toJSON() {
        return Map.of("text", this.inhalt);
    }
}