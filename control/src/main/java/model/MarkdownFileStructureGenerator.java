package model;


import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownFileStructureGenerator {
    private Map<String, String> markdownFileNamesWithPath;
    private Path writingRootPath;
    private Path statingPath;
    private final MessageObserver feedbackObserver;

    public MarkdownFileStructureGenerator(MessageObserver inputObserver) {
        this.feedbackObserver = inputObserver;
    }

    public void generateFileStructure(String path) throws InvalidPathException, IOException {
        feedbackObserver.notify("processing path: <" + path + ">");
        statingPath = Paths.get(path);
        writingRootPath = Paths.get(path + "\\exportFiles");
        markdownFileNamesWithPath = new HashMap<>();
        try {
            if (Files.exists(writingRootPath)) {
                Files.walkFileTree(writingRootPath,
                        new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult postVisitDirectory(
                                    Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(
                                    Path file, BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }
                        });

                feedbackObserver.notify("[info] deleted old /_exportFiles directory");
            }

            writingRootPath.toFile().mkdir();
            feedbackObserver.notify("[info] Added new /_exportFiles directory at -" + Files.getFileStore(writingRootPath).toString());
        } catch (SecurityException _) {
            feedbackObserver.notify("[warning] /_exportFiles couldnt be created");
        }

        Files.walkFileTree(statingPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains("exportFiles")) {
                    return FileVisitResult.SKIP_SIBLINGS;
                }
                if (file.getParent().equals(statingPath) && (file.getFileName().toString().equals("_quarto.yml")
                        || file.getFileName().toString().equals("references.bib"))) {
                    copyFile(file);
                    return FileVisitResult.CONTINUE;
                }

                // Bearbeite und kopiere nur .md Dateien
                if (file.toString().endsWith(".md") || file.toString().endsWith(".png") || file.toString().endsWith(".jpg")) {
                    copyFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        feedbackObserver.notify("----------" + System.lineSeparator() + "[info] FINISHED CREATING FILE STRUCTURE" + System.lineSeparator() + "----------");

        editAllFiles(writingRootPath);
    }

    private void copyFile(Path file) throws IOException {
        // Calculate the target path for the .md file
        Path targetPath = writingRootPath.resolve(statingPath.relativize(file));

        // Add the parent directories of the file to the set
        if (!Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }

        // Copy the file
        Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
        String fileName = file.getFileName().toString();
        markdownFileNamesWithPath.put(fileName.substring(0, fileName.length() - 3),
                "</" + statingPath.relativize(file).toString().replace("\\", "/") + ">");

        feedbackObserver.notify("[info] Copyed file from <" + file.toString() + ">" + System.lineSeparator() + "to <" + targetPath.toString() + ">");
    }

    public void editAllFiles(Path editingPath) throws IOException {
        Files.walkFileTree(editingPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".md")) {
                    editFile(file);
                    feedbackObserver.notify("[info] editing file: " + file.toString());
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void editFile(Path file) {
        String inputFilePath = file.toString();
        String outputFilePath = file.getParent().toString() + "\\temp" + file.getFileName();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath)); BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("TARGET DECK")) {
                    reader.readLine();
                    reader.readLine();
                    line = reader.readLine();
                    line = "## " + line;
                    writer.write(line);
                    writer.newLine();
                    writer.newLine();
                    reader.readLine();
                    continue;
                }
                if (line.equals("<!--IGNORED_FILE-->")) {
                    return;
                }

                if (line.contains("[[")) {

                    // Hier folgt toller regex stuff :).
                    // Basically ersetze ich hier das doofe [[tt]] mit dem coolen stuff
                    Pattern pattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
                    Matcher matcher = pattern.matcher(line);
                    StringBuilder result = new StringBuilder();

                    while (matcher.find()) {
                        String linkText = matcher.group(1); // Extrahiert den Text innerhalb der doppelten eckigen Klammern
                        if (markdownFileNamesWithPath.containsKey(linkText)) {
                            String replacement = "[" + linkText + "](" + markdownFileNamesWithPath.get(linkText) + ")";
                            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                        } else if (linkText.endsWith(".png") || linkText.endsWith(".jpg")) {
                            String replacement = "[](</" + linkText + ">)";
                            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                        } else {
                            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0))); // Retain the original if the key is not present
                        }
                    }
                    matcher.appendTail(result); // Fügt den restlichen Teil des Strings hinzu
                    line = result.toString();
                }

                // Write the modified line to the output file
                writer.write(line);
                writer.newLine();
                if (!line.isEmpty()) {
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Optionally replace the original file with the modified file
        try {
            Path originalPath = Path.of(inputFilePath);
            Path tempPath = Path.of(outputFilePath);

            // Check if the original file is writable
            if (Files.isWritable(originalPath)) {
                Files.move(tempPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                System.err.println("The original file is not writable.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
