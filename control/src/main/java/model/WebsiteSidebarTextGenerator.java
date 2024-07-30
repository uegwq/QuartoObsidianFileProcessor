package model;

import java.io.File;
import java.util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class WebsiteSidebarTextGenerator {
    private static String startingDirectory = "";
    private static final String OUTPUT_REGEX = "- \"%s\"";
    private static String output;
    private static List<String> illegalPaths = new ArrayList<>();
    private static void addIllegalPaths() {
        illegalPaths = new ArrayList<>();
        illegalPaths.add(".obsidian");
        illegalPaths.add(".quarto");
        illegalPaths.add("_book");
        illegalPaths.add("public");
        illegalPaths.add("_site");
        illegalPaths.add("TEMPLATES");
    }
    public static String process(String args) {
        addIllegalPaths();
        // Replace with your starting directory
        startingDirectory = args;

        startingDirectory.replace("\"", "");

        output = "";


        File root = new File(startingDirectory);
        List<String> markdownFiles = new ArrayList<>();

        if (root.exists() && root.isDirectory()) {
            // Perform DFS to find all .md files
            findMarkdownFiles(root, markdownFiles, 6);

            // Sort the files lexicographically
            Collections.sort(markdownFiles);

            // Print the files
            for (String file : markdownFiles) {
                System.out.println(file);
            }
        } else {
            return "The provided directory does not exist or is not a directory:" + System.lineSeparator() + "<" + startingDirectory + ">";
        }
        return output;
    }

    private static void findMarkdownFiles(File directory, List<String> markdownFiles, int depth) {
        String depthString = "";
        for (int i = 0; i < depth; i++) {
            depthString = depthString + " ";
        }
        List<AbstractMap.SimpleEntry<String, File>>
                stringFileList = new ArrayList<>();
        for (File file : directory.listFiles()) {
            stringFileList.add(new AbstractMap.SimpleEntry<>(file.getName(), file));
        }
        if (stringFileList.isEmpty()) {
            return;
        }
        stringFileList.sort(new Comparator<AbstractMap.SimpleEntry<String, File>>() {
            @Override
            public int compare(AbstractMap.SimpleEntry<String, File> o1, AbstractMap.SimpleEntry<String, File> o2) {
                String firstName = o1.getKey();
                String secondName = o2.getKey();
                Integer firstInt = null;
                Integer secondInt = null;
                try {
                    firstInt = Integer.parseInt(firstName.split("\\s+")[0]);
                    secondInt = Integer.parseInt(secondName.split("\\s+")[0]);
                } catch (Exception exception) {
                    return firstName.compareTo(secondName);
                }
                if (secondInt > firstInt) {
                    return -1;
                }
                if (secondInt.equals(firstInt)) {
                    return 0;
                }
                return 1;
            }
        });

        for (AbstractMap.SimpleEntry<String, File> fileEntry : stringFileList) {
            File file = fileEntry.getValue();
            if (file.isDirectory()) {
                //Skip file if the file is an illegal directory that shall not be processed
                if (illegalPaths.contains(file.getName())) {
                    continue;
                }
                output = output + depthString + "- section: \"" + file.getName() + "\"" + System.lineSeparator()
                        + depthString + "  contents:" + System.lineSeparator();
                findMarkdownFiles(file, markdownFiles, depth + 2);
            } else if (file.isFile() && file.getName().endsWith(".md")) {
                output = output + depthString + OUTPUT_REGEX.formatted(
                        file.getPath().substring(startingDirectory.length() + 1))
                        .replace("\\", "/" )
                + System.lineSeparator();
            }
        }
    }
}