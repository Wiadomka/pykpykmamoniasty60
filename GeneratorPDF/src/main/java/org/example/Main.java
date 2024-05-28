package org.example;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {
    public static final String DEST = "./target/sandbox/tables/WygenerowanyPDF.pdf";
    Connection connection;

    public static void main(String[] args) throws Exception {
        File file = new File(DEST);
        file.getParentFile().mkdirs();

        Main main = new Main();
        main.manipulatePdf(DEST, 1, null, null); // Filtruj wszystkich użytkowników
    }

    public void manipulatePdf(String dest, Integer doctype, Integer filtertype, String filterphrase) throws Exception {
        try {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest));
            Document doc = new Document(pdfDoc);
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/sklepszary", "root", "");

            if (doctype == 1) {
                generateTasksReport(doc, filtertype, filterphrase);
            } else if (doctype == 2) {
                generateUserReport(doc, filtertype, filterphrase);
            } else if (doctype == 3){
                generateEmployeesTaskReport(doc,filterphrase);
            }

            doc.close();
        } catch (Exception e) {
            e.printStackTrace(); // Wyświetlenie pełnego śladu stosu błędu
            System.out.println("Wystąpił błąd podczas tworzenia dokumentu PDF: " + e.getMessage());
        }
    }

    private void generateTasksReport(Document doc, Integer filtertype, String filterphrase) throws Exception {
        String query = "SELECT * FROM Zadania";
        if (filtertype != null && filterphrase != null && !filterphrase.isEmpty()) {
            if (filtertype == 1) {
                query += " WHERE Status = ?";
            } else if (filtertype == 2) {
                query += " WHERE IDKierownika = ?";
            }
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            if (filtertype != null && filterphrase != null && !filterphrase.isEmpty()) {
                preparedStatement.setString(1, filterphrase);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            Table table = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20, 20, 20}));
            Style style = new Style()
                    .setFontSize(25)
                    .setFontColor(ColorConstants.RED)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY);

            Paragraph paragraph = new Paragraph()
                    .add(new Text("Spis Wszystkich Zadan").addStyle(style));
            doc.add(paragraph);
            table.addCell("ID Zadania");
            table.addCell("Opis");
            table.addCell("Status");
            table.addCell("Termin Wykonania");
            table.addCell("Data Utworzenia");
            table.addCell("Data Modyfikacji");
            table.addCell("ID Kierownika");

            while (resultSet.next()) {
                table.addCell(String.valueOf(resultSet.getInt("IDZadania")));
                table.addCell(resultSet.getString("Opis"));
                if(resultSet.getString("Status").equals("Zakończone")) {
                    table.addCell("Zakonczone");
                } else if (resultSet.getString("Status").equals("Oczekujące")){
                    table.addCell("Oczekujace");
                } else {
                    table.addCell(resultSet.getString("Status"));
                }
                table.addCell(resultSet.getString("TerminWykonania"));
                table.addCell(resultSet.getString("DataUtworzenia"));
                table.addCell(resultSet.getString("DataModyfikacji"));
                table.addCell(resultSet.getString("IDKierownika"));
            }
            doc.add(table);
        } catch (Exception e) {
            e.printStackTrace(); // Wyświetlenie pełnego śladu stosu błędu
            System.out.println("Wystąpił błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }

    private void generateUserReport(Document doc, Integer filtertype, String filterphrase) throws Exception {
        if (filtertype == 1) {
            // Generate combined report
            addUserTable(doc, "pracownicy", "Tabela Pracowników");
            addUserTable(doc, "kierownicy", "Tabela Kierowników");
            addUserTable(doc, "administratorzy", "Tabela Administratorów");
        } else if (filtertype == 2) {
            // Generate specific table report
            if (filterphrase.equalsIgnoreCase("administratorzy") ||
                    filterphrase.equalsIgnoreCase("kierownicy") ||
                    filterphrase.equalsIgnoreCase("pracownicy")) {
                addUserTable(doc, filterphrase, "Tabela " + capitalize(filterphrase));
            } else {
                throw new IllegalArgumentException("Invalid table name provided.");
            }
        } else {
            throw new IllegalArgumentException("Invalid filter type provided.");
        }
    }

    private void addUserTable(Document doc, String tableName, String tableTitle) throws Exception {
        String query = "SELECT * FROM " + tableName;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            Table table = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20}));
            Style style = new Style()
                    .setFontSize(25)
                    .setFontColor(ColorConstants.RED)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY);

            Paragraph paragraph = new Paragraph()
                    .add(new Text(tableTitle).addStyle(style));
            doc.add(paragraph);

            String idColumnHeader = "";
            switch (tableName.toLowerCase()) {
                case "pracownicy":
                    idColumnHeader = "IDPracownika";
                    break;
                case "kierownicy":
                    idColumnHeader = "IDKierownika";
                    break;
                case "administratorzy":
                    idColumnHeader = "IDAdministratora";
                    break;
                default:
                    throw new IllegalArgumentException("Invalid table name provided.");
            }

            table.addCell(idColumnHeader);
            table.addCell("Imie");
            table.addCell("Nazwisko");
            table.addCell("E-Mail");

            while (resultSet.next()) {
                table.addCell(resultSet.getString(idColumnHeader));
                table.addCell(resultSet.getString("Imie"));
                table.addCell(resultSet.getString("Nazwisko"));
                table.addCell(resultSet.getString("Email"));
            }
            doc.add(table);
        } catch (Exception e) {
            e.printStackTrace(); // Wyświetlenie pełnego śladu stosu błędu
            System.out.println("Wystąpił błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }
    private void generateEmployeesTaskReport(Document doc, String employeeId) throws Exception {
        String query = "SELECT z.IDZadania, z.Opis, z.Status, z.TerminWykonania, z.DataUtworzenia, z.DataModyfikacji, z.IDKierownika " +
                "FROM zadania z " +
                "JOIN pracownicyzadania pz ON z.IDZadania = pz.IDZadania " +
                "WHERE pz.IDPracownika = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, employeeId);
            ResultSet resultSet = preparedStatement.executeQuery();
            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 25, 10, 15, 15, 15, 15}));
            Style style = new Style()
                    .setFontSize(25)
                    .setFontColor(ColorConstants.RED)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY);

            Paragraph paragraph = new Paragraph()
                    .add(new Text("Zadania Pracownika o ID: " + employeeId).addStyle(style));
            doc.add(paragraph);

            table.addCell("ID Zadania");
            table.addCell("Opis");
            table.addCell("Status");
            table.addCell("Termin Wykonania");
            table.addCell("Data Utworzenia");
            table.addCell("Data Modyfikacji");
            table.addCell("ID Kierownika");

            while (resultSet.next()) {
                table.addCell(resultSet.getString("IDZadania"));
                table.addCell(resultSet.getString("Opis"));
                table.addCell(resultSet.getString("Status"));
                table.addCell(resultSet.getString("TerminWykonania"));
                table.addCell(resultSet.getString("DataUtworzenia"));
                table.addCell(resultSet.getString("DataModyfikacji"));
                table.addCell(resultSet.getString("IDKierownika"));
            }
            doc.add(table);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Wystąpił błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
