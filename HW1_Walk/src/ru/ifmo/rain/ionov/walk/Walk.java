package ru.ifmo.rain.ionov.walk;

import java.io.*;

public class Walk {
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
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));
        ) {
            String s = "";
            while ((s = bufferedReader.readLine()) != null) {
                int blockSize = 1024;
                int x0 = 0x01000193;
                int hval = 0x811c9dc5;
                try (InputStream reader = new FileInputStream(s)) {
                    while (reader.available() > 0) {
                        byte[] block = new byte[blockSize];
                        int size = reader.read(block, 0, blockSize);
                        for (int i = 0; i < size; i++)
                            hval = (hval * x0) ^ (block[i] & 255);
                    }
                    bufferedWriter.write(String.format("%08x", hval) + " " + s + "\n");
                } catch (IOException e) {
                    bufferedWriter.write(String.format("%08x", 0) + " " + s + "\n");
                }
            }
        } catch (IOException ex) {
            System.out.println("Error: Can't find input/output file");
        }
    }
}