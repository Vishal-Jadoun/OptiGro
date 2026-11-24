package com.optigro.util;

import java.util.Scanner;

/**
 * Defensive console input scanner utility that prevents runtime crashes on malformed inputs.
 * 
 * @author Vishal (M1) / Harnoor (M2) Integration Contract
 */
public class ConsoleScannerUtil {
    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleScannerUtil() {}

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return 0;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter a valid integer.");
            }
        }
    }

    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            if (!scanner.hasNextLine()) {
                return max; // Safe fallback on stream closure (e.g. logout)
            }
            int val = readInt(prompt);
            if (val >= min && val <= max) {
                return val;
            }
            System.out.printf("[!] Please enter a number between %d and %d.%n", min, max);
        }
    }

    public static String readString(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }
}
