package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Main {
    enum Ucebna {
        kod_ucebny,
        nazev,
        budova,
        patro,
        kapacita_mist,
        typ_ucebny,
        pocet_pocitacu,
        projektor,
        interaktivni_tabule,
        wifi,
        klimatizace,
        ozvuceni,
        videokonference,
        spravce,
        poznamka,
        aktivni
    }

    enum Predmet {
        kod_predmetu,
        nazev,
        typ,
        kredity,
        semestr,
        vyucujici,
        katedra,
        kapacita,
        forma,
        vyuka,
        anotace
    }

    record UcebnaRow(
            String kodUcebny,
            String nazev,
            String budova,
            int patro,
            int kapacitaMist,
            String typUcebny,
            int pocetPocitacu,
            boolean projektor,
            boolean interaktivniTabule,
            boolean wifi,
            boolean klimatizace,
            boolean ozvuceni,
            boolean videokonference,
            String spravce,
            String poznamka,
            boolean aktivni) {

        static UcebnaRow fromRow(Map<Ucebna, String> row) {
            return new UcebnaRow(
                    row.get(Ucebna.kod_ucebny).trim(),
                    row.get(Ucebna.nazev).trim(),
                    row.get(Ucebna.budova).trim(),
                    Integer.parseInt(row.get(Ucebna.patro).trim()),
                    Integer.parseInt(row.get(Ucebna.kapacita_mist).trim()),
                    row.get(Ucebna.typ_ucebny).trim(),
                    Integer.parseInt(row.get(Ucebna.pocet_pocitacu).trim()),
                    isOne(row.get(Ucebna.projektor)),
                    isOne(row.get(Ucebna.interaktivni_tabule)),
                    isOne(row.get(Ucebna.wifi)),
                    isOne(row.get(Ucebna.klimatizace)),
                    isOne(row.get(Ucebna.ozvuceni)),
                    isOne(row.get(Ucebna.videokonference)),
                    row.get(Ucebna.spravce).trim(),
                    row.get(Ucebna.poznamka).trim(),
                    isOne(row.get(Ucebna.aktivni)));
        }

        private static boolean isOne(String cell) {
            return "1".equals(cell.trim());
        }
    }

    static <E extends Enum<E>> void validateHeader(String header, Class<E> enumClass) {
        E[] enumVariants = enumClass.getEnumConstants();
        if (enumVariants == null || enumVariants.length == 0) {
            throw new IllegalArgumentException("Not an enum type: " + enumClass.getName());
        }
        String[] columns = header.split(",");
        if (columns.length != enumVariants.length) {
            throw new IllegalArgumentException(
                    "CSV header has " + columns.length + " columns, but " + enumClass.getSimpleName() + " has "
                            + enumVariants.length + " values");
        }
        for (int i = 0; i < enumVariants.length; i++) {
            String expected = enumVariants[i].name();
            String actual = columns[i].trim();
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException(
                        "Column " + (i + 1) + ": expected " + expected + " but was " + actual);
            }
        }
    }

    static <E extends Enum<E>> List<HashMap<E, String>> parseCsvToMaps(Path path, Class<E> enumClass)
            throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = reader.readLine();
            if (header == null) {
                return List.of();
            }
            validateHeader(header, enumClass);
            List<HashMap<E, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                rows.add(readLine(line, enumClass));
            }
            return rows;
        }
    }

    static <E extends Enum<E>> void forEachCsvRow(InputStream in, Charset charset, Class<E> enumClass,
            Consumer<HashMap<E, String>> rowConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset))) {
            String header = reader.readLine();
            if (header == null) {
                return;
            }
            validateHeader(header, enumClass);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                rowConsumer.accept(readLine(line, enumClass));
            }
        }
    }

    static <E extends Enum<E>> void forEachCsvRow(Path path, Class<E> enumClass, Consumer<HashMap<E, String>> rowConsumer)
            throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            forEachCsvRow(in, Charset.defaultCharset(), enumClass, rowConsumer);
        }
    }


    static <E extends Enum<E>> HashMap<E, String> readLine(String line, Class<E> enumClass) {
        E[] enumVariants = enumClass.getEnumConstants();
        String[] cells = line.split(",");

        if (cells.length != enumVariants.length) {
            throw new IllegalArgumentException();
        }

        HashMap<E,String> values = new HashMap<E, String>(cells.length);

        for (int i = 0; i < cells.length; i++) {
            values.put(enumVariants[i], cells[i]);
        }

        return values;
    }

    public static void main(String[] args) {
        try {
            Path csv = args.length > 0 ? Path.of(args[0]) : Path.of("src/main/java/org/example/tridy.csv");

            System.out.println(parseCsvToMaps(csv, Ucebna.class));

            forEachCsvRow(csv, Ucebna.class, row -> {
                String kod = row.get(Ucebna.kod_ucebny);
                String nazev = row.get(Ucebna.nazev);
                System.out.println(kod + " — " + nazev);
            });

            // Parse the same CSV into typed records (via parseCsvToMaps → UcebnaRow.fromRow)
            List<UcebnaRow> ucebnaRows = new ArrayList<>();
            for (HashMap<Ucebna, String> row : parseCsvToMaps(csv, Ucebna.class)) {
                ucebnaRows.add(UcebnaRow.fromRow(row));
            }
            System.out.println("First row as record: " + ucebnaRows.get(0));
            System.out.println("Total records: " + ucebnaRows.size());

            Path predmetyCsv =
                    args.length > 1 ? Path.of(args[1]) : Path.of("src/main/java/org/example/predmety.csv");
            int predmetyOk = parseCsvToMaps(predmetyCsv, Predmet.class).size();
            System.out.println("predmety.csv with Predmet header: " + predmetyOk + " data rows");
            try {
                parseCsvToMaps(predmetyCsv, Ucebna.class);
            } catch (IllegalArgumentException e) {
                System.out.println("predmety.csv with Ucebna header (wrong structure) fails: " + e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
