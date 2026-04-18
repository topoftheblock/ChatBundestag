package org.texttechnologylab.ppr.model.interfaces;

import java.util.Map;

/**
 * Definiert das Interface für einen Redner(Hier löse ich die Anforderungen für Aufgabe 2c: Interfaces)
 */
public interface Redner {
    String getId();
    String getVollerName();

    /**
     *  A Mapvon Eigenschaften für die Speicherung als Neo4j-Knoten wird erstellt
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toNode() Methode)
     */
    Map<String, Object> toNode();
    Map<String, Object> toJSON();//nur weil in Aufgabe gefordert :)
}