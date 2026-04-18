package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Kommentar;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repräsentiert eine einzelne Rede innerhalb einer Plenarsitzung.
 * (Hier löse ich die Anforderungen für Aufgabe 2a: Implementierung der Klassenstrukturen)
 * (Hier löse ich die Anforderungen für Aufgabe 2c: Implementierung des Interfaces)
 */
public class RedeImpl implements Rede {
    // task 2e: Größtmögliche Datenkapselung
    private String id;
    private Redner redner;
    // Aufgabe zwei e: Nutzung von Collections
    private List<String> absaetze = new ArrayList<>();
    private List<Kommentar> kommentare = new ArrayList<>();

    /**
     * Konstruiert eine neue Rede
     * (für Aufgabe 2a: geeignete Konstruktoren)
     */
    public RedeImpl(String id) {
        this.id = id;
    }

    @Override
    public void addAbsatz(String text) {
        this.absaetze.add(text);
    }

    @Override
    public void addKommentar(Kommentar kommentar) {
        this.kommentare.add(kommentar);
    }

    @Override
    public String getVolltext() {
        return String.join("\n", absaetze);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Redner getRedner() {
        return redner;
    }

    @Override
    public void setRedner(Redner redner) {
        this.redner = redner;
    }

    @Override
    public List<Kommentar> getKommentare() {
        return kommentare;
    }

    /**
     * Eine  String-Zusammenfassung der Rede.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toString() Methode)
     */
    @Override
    public String toString() {
        String rednerName = (redner != null) ? redner.getVollerName() : "Unbekannt";
        return "Rede " + id + " von " + rednerName + " (" + kommentare.size() + " Kommentare)";
    }

    /**
     * Ich Erstellen eine Map von Eigenschaften für Neo4j.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toNode() Methode)
     */
    @Override
    public Map<String, Object> toNode() {
        String volltext = getVolltext();
        return Map.of(
                "id", this.id,
                "text", volltext,
                "textLaenge", volltext.length()
        );
    }
    @Override
    public Map<String, Object> toJSON() {
        Map<String, Object> json = new HashMap<>();
        json.put("id", this.id);
        json.put("volltext", getVolltext());

        // Nested das Redner-Objekt
        if (this.redner != null) {
            json.put("redner", this.redner.toJSON());
        } else {
            json.put("redner", null);
        }

        // Nested die Liste der Kommentare
        json.put("kommentare", this.kommentare.stream()
                .map(Kommentar::toJSON)
                .collect(Collectors.toList()));

        return json;
    }
}