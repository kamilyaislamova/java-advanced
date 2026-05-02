package info.kgeorgiy.ja.islamova.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

public class RecursiveWalk {

    private final MessageDigest digest;
    private final int maxDepth;

    public RecursiveWalk() {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        maxDepth = Integer.MAX_VALUE;
    }

    public RecursiveWalk(int depth) {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        maxDepth = depth;
    }

    final static String zeros = "0".repeat(16);

    private String getHash(Path file) {
        digest.reset();
        try (InputStream reader = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] b = new byte[1024];
            int c = 0;
            while ((c = reader.read(b)) >= 0) {
                digest.update(b, 0, c);
            }
            final byte[] hash = Arrays.copyOf(digest.digest(), 8);
            StringBuilder stringBuilder = new StringBuilder();
            for (byte value : hash) {
                stringBuilder.append(String.format("%02x", value));
            }
            return stringBuilder.toString();
        } catch (IOException | SecurityException | InvalidPathException e) {
            return zeros;
        }
    }

    private void goAround(String line, BufferedWriter writer) throws IOException {
        try {
            Files.walkFileTree(Paths.get(line), EnumSet.of(FOLLOW_LINKS), maxDepth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    writer.write(getHash(file) + ' ' + file.toString());
                    writer.newLine();
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    writer.write(zeros + ' ' + file.toString());
                    writer.newLine();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | SecurityException | InvalidPathException e) {
            writer.write(zeros + ' ' + line);
            writer.newLine();
        }

    }

    public void walks(String[] args) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            Path parentDir = Paths.get(args[1]).getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(args[1]), StandardCharsets.UTF_8)) {
                String line = reader.readLine();
                while (line != null) {
                    goAround(line, writer);
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (SecurityException e) {
            System.err.println("there is no access to one of files in args:" + ' ' + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("path string is invalid" + ' ' + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("wrong arguments");
            return;
        }
        RecursiveWalk walk = new RecursiveWalk();
        walk.walks(args);
    }
}
