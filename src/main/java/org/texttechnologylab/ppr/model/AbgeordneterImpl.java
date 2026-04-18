package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Abgeordneter;

/**
 * Repräsentiert einen Abgeordneten  spezialisierte Form eines Redners (erbt von RednerImpl)
 * und wird vom Parser verwendet, wenn eine Fraktion im XML gefunden wird.
 */
public class AbgeordneterImpl extends RednerImpl implements Abgeordneter {
    /**
     * Konstruiert einen neue Abgeordneter.
     * and Ruft den construktor der Basisklasse  RednerImpl auf.
     */
    public AbgeordneterImpl(String id) {
        super(id);
    }
}