package org.texttechnologylab.ppr;

import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;
import org.texttechnologylab.ppr.parser.XMLParser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Starting point für die Anwendung zur Verarbeitung von Plenarprotokollen
 * Diese Klasse koordiniert den  Process:
 * 1. Finden der XMLdateien im resourcesOrdner.
 * 2. Parsen der XML-Dateien in Java-Objekte Modelle über die {@link AppFactory}.
 * 3. Laden der Objekte in die Neo4j-Datenbank.
 * 4. Ausführen der statistischen Auswertungen auf der Database.
 */
// Aufgabe 2a: Implementierung der Klassenstrukturen
public class Main {
    /**
     * Relativer Pfad zum Verzeichnis der Neo4j-Datenbank.
     */
    private static final String DB_PATH = "target/neo4j-db";

    /**
     * Main-Methode und Einstiegspunkt der Anwendung.
     */
    public static void main(String[] args) {

        System.out.println("Gucke nach XML-Protokollen in Ordner /resources...");
        List<String> xmlDateien;
        try {
            // Aufgabe 2d: Parametrisierte Übergabe der Dateien;hier als Liste von Ressourcennamen)
            xmlDateien = findXmlResources();
        } catch (IOException | URISyntaxException e) {
            System.err.println("Fehler beim Lesen des Ordners: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (xmlDateien.isEmpty()) {
            System.err.println("Warning: Keine .xml-Dateien im Ordner 'src/main/resources' gefunden!");
            return;
        }

        System.out.println("Starte Verarbeitung für " + xmlDateien.size() + " Dateien...");

        try {
            // Aufgabe 2b: Nutzen einer Factory für zentralen Zugriff
            AppFactory factory = AppFactory.getInstance();
            XMLParser parser = factory.getParser();

            // Der Parser verarbeitet die parametrisierte Liste (2d) und nutzt Streams (2e)
            List<Sitzung> sitzungen = parser.parseFiles(xmlDateien);
            System.out.println("Parsing abgeschlossen. " + sitzungen.size() + " Sitzungen gefunden.");

            if (sitzungen.isEmpty()) {
                System.err.println("Keine Sitzungen geladen. Datenbank-Upload wird übersprungen.");
                return;
            }

            // Aufgabe zwei b (Factory) und 2c (Interface DatabaseConnection)
            try (DatabaseConnection db = factory.createDatabaseConnection(DB_PATH)) {

                System.out.println("Starte Datenbank-Upload (Aufgabe 3)...");

                // Aufgabe 2f: Laden aller Daten in eine Datenbank
                // Aufgabe 3b: übermittelm der eingelesenen Protokolle
                db.loescheDatenbank();
                db.erstelleConstraints();

                db.ladeSitzungen(sitzungen);
                db.ladeRedner(parser.getRednerCache().values());
                db.ladeRedenUndKommentare(sitzungen);
                db.erstelleBeziehungen(sitzungen);

                System.out.println("Datenbank-Upload abgeschlossen.");

                // Aufgabe Vier: Statistische Auswertung. Weitere Details in der Methode
                db.fuehreStatistikenAus();

            } catch (Exception e) {
                System.err.println("Fehler bei der Datenbankverarbeitung:");
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("Kritischer Fehler bei der Initialisierung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Durchsucht das resourcesVerzeichnis nach XML-Dateien;Implementierung von Aufgabe 2d.Dateien werdden parametrisiert übergeben.
     *
     * return: Eine sortierte {@link List} der gefundenen XML-Dateinamen.
     * IOException:          Wenn ein I/O-Fehler beim Zugriff auf das Verzeichnis auftritt.
     * URISyntaxException:   Wenn die URL des Ressourcen-Verzeichnisses fehlerhaft ist.
     */
    // Aufgabe 2d: Stellt sicher, dass alle einzulesenden Dateien parametrisiert übergeben werden können.
    private static List<String> findXmlResources() throws IOException, URISyntaxException {
        ClassLoader classLoader = Main.class.getClassLoader();
        URL resourceUrl = classLoader.getResource("");
        if (resourceUrl == null) {
            System.err.println("Konnte den 'resources'-Ordner nicht finden.");
            return Collections.emptyList();
        }

        URI resourceUri = resourceUrl.toURI();
        Path resourcePath;

        if (resourceUri.getScheme().equals("jar")) {
            FileSystem fileSystem = FileSystems.newFileSystem(resourceUri, Collections.emptyMap());
            resourcePath = fileSystem.getPath("");
        } else {
            resourcePath = Paths.get(resourceUri);
        }

        List<String> xmlFiles;
        // Aufgabe 2e: Nutzung von Streams und Collections
        try (Stream<Path> walk = Files.walk(resourcePath, 1)) {
            xmlFiles = walk
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".xml"))
                    .filter(name -> !name.equalsIgnoreCase("pom.xml"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Fehler beim Durchsuchen des Ordners: " + resourcePath);
            throw e;
        }

        return xmlFiles;
    }
}