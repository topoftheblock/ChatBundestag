package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SitzungImpl implements Sitzung {

    private String wahlperiode;
    private String sitzungNr;
    private LocalDate datum;
    private List<Rede> reden = new ArrayList<>();

    private LocalTime startZeit;
    private LocalTime endeZeit;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    /**
     * construktor
     */
    public SitzungImpl(String wp, String nr, LocalDate datum) {
        this.wahlperiode = wp;
        this.sitzungNr = nr;
        this.datum = datum;
    }

    @Override
    public String getWahlperiode() {
        return wahlperiode;
    }

    @Override
    public String getSitzungNr() {
        return sitzungNr;
    }

    @Override
    public LocalDate getDatum() {
        return datum;
    }

    @Override
    public void addRede(Rede rede) {
        this.reden.add(rede);
    }

    @Override
    public List<Rede> getReden() {
        return this.reden;
    }

    @Override
    public void setStartZeit(LocalTime startZeit) {
        this.startZeit = startZeit;
    }

    @Override
    public void setEndeZeit(LocalTime endeZeit) {
        this.endeZeit = endeZeit;
    }

    @Override
    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    @Override
    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    @Override
    public LocalDateTime getStartDateTime() {
        return this.startDateTime;
    }

    @Override
    public LocalDateTime getEndDateTime() {
        return this.endDateTime;
    }


    /**
     * toNode() Methode
     */
    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> node = new HashMap<>();
        node.put("wahlperiode", this.wahlperiode);
        node.put("sitzungNr", this.sitzungNr);
        node.put("datum", this.datum.toString());

        if (this.startDateTime != null) {
            node.put("startDateTime", this.startDateTime.toString());
        } else {
            node.put("startDateTime", null);
        }

        if (this.endDateTime != null) {
            node.put("endDateTime", this.endDateTime.toString());
        } else {
            node.put("endDateTime", null);
        }

        return node;
    }

    @Override
    public Map<String, Object> toJSON() { // Nur weil in der Aufgabe gefordert :)
        Map<String, Object> json = new HashMap<>();
        json.put("wahlperiode", this.wahlperiode);
        json.put("sitzungNr", this.sitzungNr);
        json.put("datum", this.datum.toString());

        if (this.startDateTime != null) {
            json.put("startDateTime", this.startDateTime.toString());
        } else {
            json.put("startDateTime", null);
        }

        if (this.endDateTime != null) {
            json.put("endDateTime", this.endDateTime.toString());
        } else {
            json.put("endDateTime", null);
        }

        // Liste der Reden
        json.put("reden", this.reden.stream()
                .map(Rede::toJSON)
                .collect(Collectors.toList()));

        return json;
    }
}