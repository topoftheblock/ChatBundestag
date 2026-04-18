package org.texttechnologylab.ppr.parser;

import org.texttechnologylab.ppr.model.*;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Ein Parser zur Umwandlung von XML-Files.
 * wandelt  in Java-Objekt Sitzung, Rede, Redner).
 * (Hier löse ich die Anforderungen für Aufgabe 2a: Implementierung der Klassenstrukturen)
 */
public class XMLParser {

    // Aufgabe 2e: Nutzung von Collections
    private Map<String, Redner> rednerCache = new HashMap<>();

    private Set<String> verarbeiteteSitzungen = new HashSet<>();
    private int duplikatZaehler = 0; // Zähler für Duplicates

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");
    private DocumentBuilder builder;

    /**
     * Konstruktor für the XMLParser.
     * (Hier löse ich die Anforderungen für Aufgabe 2a: geeignete Konstruktoren)
     */
    public XMLParser() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (ParserConfigurationException e) {
            System.err.println("Warnung: XML-Parser konnte DTD-Validierung nicht deactivate.");
        }
        this.builder = factory.newDocumentBuilder();
    }

    /**
     * Parst eine Liste von XML-Dateien anhand ihrer Ressourcennames.
     * (Hier löse ich die Anforderungen für Aufgabe 2d: alle einzulesenden Dateien parametrisiert übergeben)
     */
    public List<Sitzung> parseFiles(List<String> resourceNames) {
        // Zähler und Set für jeden Lauf zurücksetzen for Duplicates
        this.verarbeiteteSitzungen.clear();
        this.duplikatZaehler = 0;

        // Aufgabe 2e: Nutzung von Streams
        List<Sitzung> sitzungen = resourceNames.stream()
                .map(this::parseResource)
                .filter(sitzung -> sitzung != null)
                .collect(Collectors.toList());

        // Reporting der gefundenen Duplikate
        if (this.duplikatZaehler > 0) {
            System.out.println("Info: " + this.duplikatZaehler + " Duplikat(e) (basierend auf WP/Sitzungs-Nr) wurden beim Parsen übersprungen.");
        }

        return sitzungen;
    }

    /**
     * Lädt und parst eine einzelne XML-Ressourcendatei.
     */
    private Sitzung parseResource(String resourceName) {
        try (InputStream is = XMLParser.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                System.err.println("Fehler: Ressource nicht gefunden: " + resourceName);
                return null;
            }
            System.out.println("Parse Ressource: " + resourceName);
            Document document = builder.parse(is);
            Element root = document.getDocumentElement();

            String wp = root.getAttribute("wahlperiode");
            String nr = root.getAttribute("sitzung-nr");

            // Ich Prüfe auf Duplikate
            String sitzungId = wp + "/" + nr;
            if (!this.verarbeiteteSitzungen.add(sitzungId)) {
                System.err.println("Warnung: Sitzung " + sitzungId + " (aus Datei " + resourceName + ") wurde in diesem Durchlauf bereits verarbeitet und wird übersprungen.");
                this.duplikatZaehler++;
                return null;
            }

            LocalDate datum = LocalDate.parse(root.getAttribute("sitzung-datum"), dateFormatter);

            Sitzung sitzung = new SitzungImpl(wp, nr, datum);

            // Sesssions over midnight
            try {
                String startZeitStr = root.getAttribute("sitzung-start-uhrzeit");
                String endeZeitStr = root.getAttribute("sitzung-ende-uhrzeit");

                LocalTime startZeit = LocalTime.parse(startZeitStr, timeFormatter);
                LocalTime endeZeit = LocalTime.parse(endeZeitStr, timeFormatter);

                // Erstellt LocalDateTime für den Start
                LocalDateTime startDateTime = datum.atTime(startZeit);
                // Erstelle LocalDateTime für das Ende
                LocalDateTime endDateTime;
                if (endeZeit.isBefore(startZeit)) {
                    // wenn Sitzung  über Mitternacht -> Datum des nächsten day
                    endDateTime = datum.plusDays(1).atTime(endeZeit);
                } else {
                    // Sitzung endete am selben Tag
                    endDateTime = datum.atTime(endeZeit);
                }

                sitzung.setStartDateTime(startDateTime);
                sitzung.setEndDateTime(endDateTime);
                sitzung.setStartZeit(startZeit);
                sitzung.setEndeZeit(endeZeit);


            } catch (DateTimeParseException e) {
                System.err.println("Warnung: Uhrzeit konnte nicht geparst werden in " + resourceName);
            }

            NodeList redeNodes = document.getElementsByTagName("rede");
            // Aufgabe 2e: Nutzung von Streams
            IntStream.range(0, redeNodes.getLength())
                    .mapToObj(redeNodes::item)
                    .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                    .map(node -> (Element) node)
                    .forEach(redeElement -> {
                        Rede rede = parseRede(redeElement);
                        sitzung.addRede(rede);
                    });
            return sitzung;
        } catch (SAXException | IOException e) {
            System.err.println("Fehler beim Parsen der Ressource: " + resourceName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parst ein einzel <rede>-XML-Element.
     * (Hier löse ich die Anforderungen für Aufgabe 2e: Nutzung von Collections in der FOrm eines rednerCache))
     */
    private Rede parseRede(Element redeElement) {
        String redeId = redeElement.getAttribute("id");
        Rede rede = new RedeImpl(redeId);

        NodeList rednerNodes = redeElement.getElementsByTagName("redner");
        if (rednerNodes.getLength() > 0) {
            Element rednerElement = (Element) rednerNodes.item(0);
            String rednerId = rednerElement.getAttribute("id");

            Redner redner = rednerCache.computeIfAbsent(rednerId, id -> {
                RednerImpl r;
                String fraktion = getTextContent(rednerElement, "fraktion");

                if (fraktion != null && !fraktion.isEmpty()) {

                    fraktion = fraktion.replaceAll("\\s+", " ").trim();

                    if (fraktion.equals("BÜNDNIS 90/ DIE GRÜNEN")) {//bug in Protokoll abfangen ;)
                        fraktion = "BÜNDNIS 90/DIE GRÜNEN";
                    }
                    if (fraktion.equals("SPDCDU/CSU")) { //bug in Protokoll abfangen ;) Weil Metadaten Dirk-UlrichAlexander Mende Föhr fehlerhaft!
                        fraktion = "SPD";
                    }
                    fraktion = fraktion.toUpperCase();
                    if (fraktion.equals("FRAKTIONSLOSE")) { //bug in Protokoll abfangen ;) wegen inkonsistenz
                        fraktion = "FRAKTIONSLOS";
                    }

                    r = new AbgeordneterImpl(id); // es ist ein Abgeordneter

                } else {
                    fraktion = null; // Stellt sicher, dass es null ist -z.B. bei Ministern)
                    r = new RednerImpl(id); // Es ist ein normaler Redner
                }

                r.setVorname(getTextContent(rednerElement, "vorname"));
                r.setNachname(getTextContent(rednerElement, "nachname"));
                r.setTitel(getTextContent(rednerElement, "titel"));
                r.setFraktion(fraktion);
                return r;
            });
            rede.setRedner(redner);
        }

        NodeList childNodes = redeElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                if (childElement.getTagName().equals("p") && !childElement.getAttribute("klasse").equals("redner")) {
                    rede.addAbsatz(childElement.getTextContent().trim());
                }
                if (childElement.getTagName().equals("kommentar")) {
                    rede.addKommentar(new KommentarImpl(childElement.getTextContent().trim()));
                }
            }
        }
        return rede;
    }

    /**
     * Hilfsmethode, um den Textinhalt eines Child-Elements sicher auszulesen.
     */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }

    /**
     * Gibt den rednerCache zurück.
     * (Hier löse ich die Anforderungen für Aufgabe 2e: Nutzung von Collections)
     */
    public Map<String, Redner> getRednerCache() {
        return rednerCache;
    }
}