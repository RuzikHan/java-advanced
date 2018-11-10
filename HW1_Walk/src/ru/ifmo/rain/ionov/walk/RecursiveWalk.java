package ru.ifmo.rain.ionov.walk;

import java.io.*;
import java.nio.file.*;

public class RecursiveWalk {
    private static void recursiveWalk(String s, BufferedWriter bufferedWriter) throws IOException {
        try {
            Path filePath = Paths.get(s);
            try {
                Files.walkFileTree(filePath, new Visitor(bufferedWriter));
            } catch (IOException e) {
                bufferedWriter.write(String.format("%08x", 0) + " " + s + "\n");
            }
        } catch (InvalidPathException e) {
            bufferedWriter.write(String.format("%08x", 0) + " " + s + "\n");
        }
    }

    public static void main(String[] args) {
        if (args == null) {
            System.out.println("Error: args = null");
            return;
        }
        if (args.length < 2) {
            System.out.println("Error: expected 2 arguments: input file name and output file name");
            return;
        }
        if (args[0] == null) {
            System.out.println("Error: input file null");
            return;
        }
        if (args[1] == null) {
            System.out.println("Error: output file null");
            return;
        }
        try (
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"))
        ) {
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                try {
                    recursiveWalk(s, bufferedWriter);
                } catch (IOException e) {
                    System.out.println("Error: Can't write to output file");
                }
            }
        } catch (IOException ex) {
            System.out.println("Error: Can't find input/output file");
        }
    }
}