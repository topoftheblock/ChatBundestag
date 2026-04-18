package org.texttechnologylab.ppr.model.interfaces;

import java.util.Map;

/**
 * Definiert das Interface für einen Kommentar innerhalb einer Rede.
 * (Hier löse ich die Anforderungen für Aufgabe 2c: Interfaces)
 */
public interface Kommentar {
    /**
     *  Eine MAP von Eigenschaften für die Speicherung als Neo4j-Knoten werde erstellt
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toNode() Methode)
     */
    Map<String, Object> toNode();
    Map<String, Object> toJSON(); //nur weil in Aufgabe gefordert :)
}
