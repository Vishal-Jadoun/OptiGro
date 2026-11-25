package com.optigro.util;

import java.util.List;

/**
 * Reusable ASCII table, invoice and divider formatter.
 * 
 * @author Harnoor (M2)
 */
public class AsciiTableFormatter {

    private AsciiTableFormatter() { }

    public static void printTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];

        for (int i = 0; i < cols; i++) {
            widths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < cols && i < row.length; i++) {
                widths[i] = Math.max(widths[i], (row[i] != null ? row[i].length() : 0));
            }
        }

        printSeparator(widths);
        printRow(headers, widths);
        printSeparator(widths);

        for (String[] row : rows) {
            printRow(row, widths);
        }
        printSeparator(widths);
    }

    private static void printSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        System.out.println(sb);
    }

    private static void printRow(String[] items, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String val = (i < items.length && items[i] != null) ? items[i] : "";
            sb.append(String.format(" %-" + widths[i] + "s |", val));
        }
        System.out.println(sb);
    }

    public static void printDivider(int width) {
        System.out.println("═".repeat(Math.max(1, width)));
    }
}
