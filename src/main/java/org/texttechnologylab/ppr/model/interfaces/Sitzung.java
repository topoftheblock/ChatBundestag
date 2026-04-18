package org.texttechnologylab.ppr.model.interfaces;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface Sitzung {

    String getWahlperiode();
    String getSitzungNr();
    LocalDate getDatum();

    void addRede(Rede rede);
    List<Rede> getReden();

    void setStartZeit(LocalTime startZeit);
    void setEndeZeit(LocalTime endeZeit);
    void setStartDateTime(LocalDateTime startDateTime);
    void setEndDateTime(LocalDateTime endDateTime);
    LocalDateTime getStartDateTime(); //Für die Vollständigkeit, keine Benutzung
    LocalDateTime getEndDateTime(); //Für die Vollständigkeit, keine Benutzung
    Map<String, Object> toNode();
    Map<String, Object> toJSON();//nur weil in Aufgabe gefordert :)
}