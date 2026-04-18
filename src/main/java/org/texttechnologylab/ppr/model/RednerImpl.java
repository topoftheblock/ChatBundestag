package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Redner;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Repräsentiert einen Redner in einem Plenarprotokoll.
 * ist die Basisklasse. Ein Redner kann ein Abgeordneter sein
 * oder eine andere Rolle haben (z.B. Minister oder so).
 * (Hier löse ich die Anforderungen für Aufgabe 2a: Implementierung der Klassenstrukturen)
 * (Hier löse ich die Anforderungen für Aufgabe 2c: Implementierung des Interfaces)
 */
public class RednerImpl implements Redner {

    // Aufgabe 2e: Größtmögliche Datenkapselung
    private String id;
    private String vorname;
    private String nachname;
    private String titel;
    private String fraktion; // Ist null, wenn kein Abgeordneter (z.B. Minister)

    /**
     * Konstruiert einen neuen {@code Redner}.
     * (Hier löse ich die Anforderungen für Aufgabe 2a: geeignete Konstruktoren)
     */
    public RednerImpl(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public void setFraktion(String fraktion) {
        this.fraktion = fraktion;
    }

    /**
     * Gibt den vollständigen Namen des Redners inklusive Titel zurück. Kann man auch weglassen :)
     */
    @Override
    public String getVollerName() {
        return (titel != null && !titel.isEmpty() ? titel + " " : "") + vorname + " " + nachname;
    }

    /**
     * Vergleicht diesen Redner mit einem anderen Objekt auf Equivalence.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (o instanceof Redner) {
            return Objects.equals(id, ((Redner) o).getId());
        }
        return false;
    }

    /**
     * Generiert einen Hashcode für den Redner, basierend auf seiner  id.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Rückgabe Eine menschenlesbare String-Summary des Redners.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toString() Methode)
     */
    @Override
    public String toString() {
        return "Redner " + id + ": " + getVollerName() + " (" + (fraktion != null ? fraktion : "N/A") + ")";
    }

    /**
     * Erstellt a  Map von Eigenschaften für Neo4j.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toNode() Methode)
     */
    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> props = new HashMap<>();
        props.put("id", this.id);
        props.put("vorname", this.vorname);
        props.put("nachname", this.nachname);
        props.put("titel", this.titel);
        props.put("fraktion", this.fraktion);
        return props;
    }
    @Override
    public Map<String, Object> toJSON() {
        Map<String, Object> json = new HashMap<>();
        json.put("id", this.id);
        json.put("vollerName", getVollerName());
        json.put("vorname", this.vorname);
        json.put("nachname", this.nachname);
        json.put("titel", this.titel);
        json.put("fraktion", this.fraktion);
        return json;
    }
}