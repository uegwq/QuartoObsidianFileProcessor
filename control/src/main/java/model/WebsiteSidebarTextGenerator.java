package model;

import java.io.File;
import java.util.*;

public class WebsiteSidebarTextGenerator {
    private static final String OUTPUT_FORMAT = "- \"%s\"";
    private static final List<String> EXCLUDED_DIRECTORIES = Arrays.asList(
            ".obsidian", ".quarto", "_book", "public", "_site",
            "TEMPLATES", "exportFiles", ".git"
    );

    private static String baseDirectory = "";
    private static StringBuilder output = new StringBuilder();

    public static String generateSidebarText(String directoryPath) {
        baseDirectory = directoryPath.replace("\"", "");
        output.setLength(0); // Clear output

        File rootDirectory = new File(baseDirectory);
        List<String> markdownFiles = new ArrayList<>();

        if (rootDirectory.exists() && rootDirectory.isDirectory()) {
            findMarkdownFiles(rootDirectory, markdownFiles, 6);
            Collections.sort(markdownFiles);
            markdownFiles.forEach(System.out::println);
        } else {
            return "The provided directory does not exist or is not a directory:"
                    + System.lineSeparator() + "<" + baseDirectory + ">";
        }
        return output.toString();
    }

    private static void findMarkdownFiles(File directory, List<String> markdownFiles, int depth) {
        String indentation = " ".repeat(depth);

        List<File> sortedFiles = getSortedFiles(directory);
        if (sortedFiles.isEmpty()) {
            return;
        }

        for (File file : sortedFiles) {
            if (file.isDirectory()) {
                if (EXCLUDED_DIRECTORIES.contains(file.getName())) {
                    continue;
                }
                output.append(indentation).append("- section: \"")
                        .append(file.getName()).append("\"").append(System.lineSeparator())
                        .append(getHref(file, indentation))
                        .append(indentation).append("  contents:")
                        .append(System.lineSeparator());
                findMarkdownFiles(file, markdownFiles, depth + 2);
            } else if (isMarkdownFile(file)) {
                output.append(indentation)
                        .append(String.format(OUTPUT_FORMAT,
                                file.getPath().substring(baseDirectory.length() + 1)
                                        .replace("\\", "/")))
                        .append(System.lineSeparator());
            }
        }
    }

    private static List<File> getSortedFiles(File directory) {
        List<File> fileList = Arrays.asList(directory.listFiles());
        fileList.sort((file1, file2) -> {
            try {
                int firstNum = Integer.parseInt(file1.getName().split("\\s+")[0]);
                int secondNum = Integer.parseInt(file2.getName().split("\\s+")[0]);
                return Integer.compare(firstNum, secondNum);
            } catch (NumberFormatException e) {
                return file1.getName().compareTo(file2.getName());
            }
        });
        return fileList;
    }

    private static String getHref(File directory, String indentation) {
        for (File file : directory.listFiles()) {
            String fileName = file.getName();
            String dirName = directory.getName();
            if (fileName.endsWith(".md") && fileName.startsWith(dirName)) {
                return indentation + "  href: " + fileName + System.lineSeparator();
            }
            if (fileName.endsWith(".qmd") && fileName.startsWith(dirName)) {
                return indentation + "  href: " + fileName + System.lineSeparator();
            }
        }
        return "";
    }

    private static boolean isMarkdownFile(File file) {
        String fileName = file.getName();
        return fileName.endsWith(".md") || fileName.endsWith(".qmd");
    }
}
