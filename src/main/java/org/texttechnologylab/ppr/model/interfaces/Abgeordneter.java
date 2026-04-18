package org.texttechnologylab.ppr.model.interfaces;

/**
 *  das Interface für einen Abgeordneten wird definiert
 * Ein Abgeordneter ist ein Redner, also erweitert Redner.Dieses Interface für Typ-Differenzierung.
 */
public interface Abgeordneter extends Redner {
    // This Interface erbt all Methoden von Redner
    // und dient als Interface zur Identifizierung von Abgeordneten. Ist wahrscheinlich nicht opitmal :)
}