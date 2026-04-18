package org.texttechnologylab.ppr.model.interfaces;

import java.util.List;
import java.util.Map;

/**
 *Definiert das Interface für eine einzelne Rede.(Hier löse ich die Anforderungen für Aufgabe 2c: Interfaces)
 */
public interface Rede {
    String getId();
    Redner getRedner();
    void setRedner(Redner redner);
    void addAbsatz(String text);
    List<Kommentar> getKommentare();
    void addKommentar(Kommentar kommentar);
    String getVolltext();

    /**
     * IchErstelle eine Map vonEigenschaften für dieSpeicherung als Neo4j-Knoten.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toNode() Methode)
     */
    Map<String, Object> toNode();
    Map<String, Object> toJSON();//nur weil in Aufgabe gefordert :)
}