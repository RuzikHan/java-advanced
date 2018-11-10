package ru.ifmo.rain.ionov.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Visitor extends SimpleFileVisitor<Path> {
    private BufferedWriter bufferedWriter;

    public Visitor(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        int blockSize = 1024;
        int x0 = 0x01000193;
        int hval = 0x811c9dc5;
        try (InputStream reader = new FileInputStream(path.toString())) {
            while (reader.available() > 0) {
                byte[] block = new byte[blockSize];
                int size = reader.read(block, 0, blockSize);
                for (int i = 0; i < size; i++)
                    hval = (hval * x0) ^ (block[i] & 255);
            }
            bufferedWriter.write(String.format("%08x", hval) + " " + path + "\n");
        } catch (IOException e) {
            bufferedWriter.write(String.format("%08x", 0) + " " + path + "\n");
        }
        return FileVisitResult.CONTINUE;
    }
}
