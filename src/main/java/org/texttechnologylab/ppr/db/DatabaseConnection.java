package org.texttechnologylab.ppr.db;

import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.util.Collection;
import java.util.List;

/**
 * Definiert die Schnittstelle für eineDatenbankverbindung.Lösen Aufgabe 3 und vier.
 * (Hier löse ich die Anforderungen für Aufgabe 2c: Nutzung von Interfaces)
 */
// Aufgabe 2c: Adaptieren Sie ... die Prinzipien der ... Interfaces
// Aufgabe 3a: Erstellen Sie eine Klasse Neo4jConnection; das ist das Interface dafür
public interface DatabaseConnection extends AutoCloseable {

    /**
     *  alle vorhandenen Daten -Knoten und Beziehungen, aus der Datenbank löschen
     * (Hier löse ich die Anforderungen für einen Teil von Aufgabe drei a: Löschen von Knoten)
     */
    void loescheDatenbank();

    /**
     * Erstellt notwendige Indizes-Consttraints und Eindeutigkeits-Constraints in der Datenbank.
     * (Hier löse ich die Anforderungen für einenTeil von Aufgabe 3a: Erstellen von Knoten/Strukturen)
     */
    void erstelleConstraints();

    /**
     * Lädt eine Liste von Sitzung-Objekten als Knoten in die Datenbank.
     * (Hier löse ich Anforderungen für einenTeil von Aufgabe 3b: Übertragen der Datenstrukturen)
     */
    void ladeSitzungen(List<Sitzung> sitzungen);

    /**
     * Loads Sammlung von {Redner-Objekten alsKnoten in die Datenbank.
     * (Hier löse ich die Anforderungen für einen Teil von Aufgabe 3b:Übertragen der Datenstrukturen)
     */
    void ladeRedner(Collection<Redner> redner);

    /**
     * äAll Reden und Kommentare, aus den gegebene nSitzungen als Knoten in die Datenbank geladen
     * (Hier löse ich die Anforderungen für einen Teil von Aufgabe 3b: Übertragen der Datenstrukturen)
     */
    void ladeRedenUndKommentare(List<Sitzung> sitzungen);

    /**
     *  die Beziehungen(Kanten) zwischen den bereits geladenen Knoten werden erstellt
     * (Hier löse ich die Anforerungen für einen Teil von Aufgabe 3a: Erstellen von Relationen)
     * (Hier löse ich die Anforderungen für  einen Teil von Aufgabe 3b: Übertragen der Datenstrukturen)
     */
    void erstelleBeziehungen(List<Sitzung> sitzungen);

    /**
     * executes von alle vordefinierten statistischen Abfragen (Aufgabe vier aus.
     * (Hier löse ich die Anforderungen für Aufgabe 4: Statistische Auswertung)
     */
    void fuehreStatistikenAus();

    /**
     * Closing: Schließt die Datenbankverbindung und gibt alle zugehörigen Ressourcen frei.
     */
    @Override
    void close();
}