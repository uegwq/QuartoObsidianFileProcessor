package model;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownFileStructureGenerator {

    private Map<String, String> fileLinksMap;
    private Path sourceRootPath;
    private Path targetRootPath;
    private final MessageObserver observer;

    public MarkdownFileStructureGenerator(MessageObserver observer) {
        this.observer = observer;
    }

    public void generateFileStructure(String rootPath) throws InvalidPathException, IOException {
        observer.notify("Processing path: <" + rootPath + ">");
        initializePaths(rootPath);
        fileLinksMap = new HashMap<>();

        deleteOldExportFilesDirectory();
        createExportFilesDirectory();

        Files.walkFileTree(sourceRootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (shouldCopyFile(file)) {
                    copyFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        observer.notify("[info] Finished creating file structure");
        updateMarkdownFiles(targetRootPath);
    }

    private void initializePaths(String rootPath) {
        sourceRootPath = Paths.get(rootPath);
        targetRootPath = Paths.get(rootPath, "exportFiles");
    }

    private void deleteOldExportFilesDirectory() throws IOException {
        if (Files.exists(targetRootPath)) {
            Files.walkFileTree(targetRootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            observer.notify("[info] Deleted old /_exportFiles directory");
        }
    }

    private void createExportFilesDirectory() {
        try {
            Files.createDirectories(targetRootPath);
            observer.notify("[info] Created new /_exportFiles directory at " + targetRootPath.toString());
        } catch (IOException e) {
            observer.notify("[warning] /_exportFiles could not be created");
        }
    }

    private boolean shouldCopyFile(Path file) {
        String fileName = file.getFileName().toString();
        return !file.toString().contains("exportFiles") &&
                (file.getParent().equals(sourceRootPath) && (fileName.equals("_quarto.yml") || fileName.equals("references.bib")) ||
                        fileName.endsWith(".qmd") || fileName.endsWith(".md") || fileName.endsWith(".png") || fileName.endsWith(".jpg"));
    }

    private void copyFile(Path file) throws IOException {
        Path targetPath = targetRootPath.resolve(sourceRootPath.relativize(file));

        Files.createDirectories(targetPath.getParent());
        Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(targetPath, Files.getLastModifiedTime(file));

        String fileName = file.getFileName().toString();
        String relativePath = sourceRootPath.relativize(file).toString().replace("\\", "/");
        fileLinksMap.put(fileName.substring(0, fileName.lastIndexOf('.')), "</" + relativePath + ">");

        observer.notify("[info] Copied file from <" + file.toString() + "> to <" + targetPath.toString() + ">");
    }

    private void updateMarkdownFiles(Path directoryPath) throws IOException {
        Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".md") || file.toString().endsWith(".qmd")) {
                    observer.notify("[info] Editing file: " + file.toString());
                    editMarkdownFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void editMarkdownFile(Path file) {
        String inputFilePath = file.toString();
        String outputFilePath = file.getParent().toString() + "/temp_" + file.getFileName();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            boolean isInTags = false;
            boolean madeChanges = false;
            boolean hasSeenTags = false;
            int calloutDepth = 0;
            String line;

            writer.write("---");
            writer.newLine();
            writer.write("date: " + Files.getLastModifiedTime(file).toString().substring(0,10));
            writer.newLine();
            //If file is empty then this is necessary
            if (!reader.ready()) {
                writer.write("---");
            }

            while ((line = reader.readLine()) != null) {
                if (line.equals("<!--IGNORED_FILE-->")) {
                    observer.notify("[info] File ignored");
                    return;
                }
                if (line.equals("<!--TAGS-->")) {
                    isInTags = true;
                    hasSeenTags = true;
                    reader.readLine();
                    observer.notify("[info] Started reading tags");
                    continue;
                }
                if (line.equals("<!--/TAGS-->")) {
                    isInTags = false;
                    observer.notify("[info] Done reading tags");
                    continue;
                }
                if (!hasSeenTags) {
                    while (line.isEmpty() && reader.ready() && (line = reader.readLine()).isEmpty()) {
                    }
                    writer.write("---");
                    writer.newLine();
                    hasSeenTags = true;
                }

                if (line.equals("TARGET DECK")) {
                    madeChanges = applyTargetDeckTemplate(reader, writer);
                    continue;
                }

                if (isInTags) {
                    writer.write(line);
                    writer.newLine();
                    continue;
                }

                String modifiedLine = replaceMarkdownLinks(line);

                Pattern calloutPattern = Pattern.compile("\\s*>\\s*\\[!(\\w+)]\\s*(.*)");
                Pattern nestedCalloutPattern = Pattern.compile("\\s*>\\s*>\\s*\\[!(\\w+)]\\s*(.*)");

                // Prüfen, ob die Zeile ein Callout beginnt
                Matcher matcher = calloutPattern.matcher(modifiedLine);
                Matcher nestedMatcher = nestedCalloutPattern.matcher(modifiedLine);

                if (nestedMatcher.find()) {
                    // Verschachtelter Callout gefunden
                    String calloutType = nestedMatcher.group(1);  // z.B. "info" oder "warning"
                    String title = calloutType;
                    if (matcher.groupCount() > 1) {
                        title = matcher.group(2).trim();  // Titel des Callouts
                    }
                    modifiedLine = "::: {.callout-"+calloutType+" title=\""+title+"\"}";
                    calloutDepth = 2;
                    writer.write(modifiedLine);
                    writer.newLine();
                    continue;
                }
                else if (matcher.find()) {
                    // Einfache Callout-Zeile gefunden
                    String calloutType = matcher.group(1);  // z.B. "info" oder "warning"
                    String title = calloutType;
                    title = matcher.group(2).trim();  // Titel des Callouts
                    modifiedLine = "::: {.callout-"+calloutType+" title=\""+title+"\"}";
                    calloutDepth = 1;
                    writer.write(modifiedLine);
                    writer.newLine();
                    continue;
                }
                    int geCharCount = modifiedLine.length() - modifiedLine.replace(">", "").length();
                if (geCharCount < calloutDepth) {
                    for (int i = 0; i < calloutDepth - geCharCount; i++) {
                        writer.write(":::");
                        writer.newLine();
                    }
                    calloutDepth = geCharCount;
                }

                writer.write(modifiedLine.replace(">", ""));
                writer.newLine();

                if (!line.isEmpty()) {
                    madeChanges = true;
                    writer.newLine();
                }
            }

            if (madeChanges) {
                observer.notify("[info] Made changes to " + file.toString());
            }
            reader.close();
            writer.close();
            replaceOriginalFile(file, outputFilePath);

        } catch (IOException e) {
            observer.notify("[error] Error editing file: " + file.toString());
            e.printStackTrace();
        }
    }

    private boolean applyTargetDeckTemplate(BufferedReader reader, BufferedWriter writer) throws IOException {
        reader.readLine();
        reader.readLine();
        String line = reader.readLine();
        line = "## " + line;
        writer.write(line);
        writer.newLine();
        writer.newLine();
        reader.readLine();
        observer.notify("[info] Applied TARGET DECK template changes");
        return true;
    }

    private String replaceMarkdownLinks(String line) {
        Pattern pattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
        Matcher matcher = pattern.matcher(line);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String linkText = matcher.group(1);
            if (fileLinksMap.containsKey(linkText)) {
                String replacement = "[" + linkText + "](" + fileLinksMap.get(linkText) + ")";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else if (linkText.endsWith(".png") || linkText.endsWith(".jpg")) {
                String replacement = "[](</" + linkText + ">)";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void replaceOriginalFile(Path originalFile, String tempFilePath) throws IOException {
        Path tempPath = Path.of(tempFilePath);

        // Check if the original file is writable
        if (Files.isWritable(originalFile)) {
            Files.move(tempPath, originalFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            observer.notify("[warning] The original file is not (over)writable: " + originalFile.toString());
        }
    }
}
