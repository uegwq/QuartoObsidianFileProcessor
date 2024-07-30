package controler;

import model.MarkdownFileStructureGenerator;

import java.io.IOException;

public class SuperMain {
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            if (args[0].equals("ui--false") && args.length == 1) {
                throw new IllegalArgumentException("wrong input! if please provide the path for processing.");
            }
            if (args[0].equals("ui--false")) {
                MarkdownFileStructureGenerator generator = new MarkdownFileStructureGenerator(new PrintLineMessageObserver());
                generator.generateFileStructure(args[1]);
            }
        } else {
            System.out.println("ERROR PLEASE PROVIDE PATH AND \"ui--false\"");
        }
    }
}
