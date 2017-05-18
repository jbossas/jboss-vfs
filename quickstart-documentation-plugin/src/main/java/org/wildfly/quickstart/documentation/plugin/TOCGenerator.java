package org.wildfly.quickstart.documentation.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class TOCGenerator {
    private static final List<String> IGNORED_DIRS = Arrays.asList("target", "dist", "template", "guide");

    public static void main(String[] args) throws IOException {

        Path root = Paths.get(".").normalize();
        Set<MetaData> allMetaData = new TreeSet<>(Comparator.comparing(o -> o.name));
        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(root, entry -> Files.isDirectory(entry)
                && (!entry.toString().startsWith("."))
                && (!IGNORED_DIRS.contains(entry.toString())))) {
            dirs.forEach(path -> {
                try {
                    allMetaData.add(parseReadme(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        StringBuffer sb = generateTOC(allMetaData);
        Path tocFile = root.resolve("target/docs/README.md");
        String tocFileContent = new String(Files.readAllBytes(tocFile), StandardCharsets.UTF_8);
        tocFileContent = tocFileContent.replace("[TOC-quickstart]", sb.toString());
        Files.write(tocFile, tocFileContent.getBytes(StandardCharsets.UTF_8));
    }

    private static StringBuffer generateTOC(Set<MetaData> metaDataList) {
        /*

| Tables        | Are           | Cool  |
| ------------- |:-------------:| -----:|
| col 3 is      | right-aligned | $1600 |
         */
        StringBuffer sb = new StringBuffer();
        sb.append("| *Quickstart Name* | *Demonstrated Technologies* | *Description* | *Experience Level Required* | *Prerequisites* |\n");
        sb.append("| --- | --- | --- | --- |\n");
        for (MetaData md : metaDataList) {
            sb.append("| ")
                    .append("[").append(md.name).append("]").append("(").append(md.name).append("/README.md) |")
                    .append(md.getTechnologiesAsString()).append(" | ")
                    .append(md.summary).append(" | ")
                    .append(md.level).append(" | ")
                    .append(md.prerequisites)
                    .append(" |")
                    .append("\n");

        }

        return sb;
    }

    private static MetaData parseReadme(Path quickstartDir) throws IOException {
        Path path = quickstartDir.resolve("README.md");
        MetaData metaData = new MetaData(quickstartDir.toString());
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<String> result = new ArrayList<>();
            for (; ; ) {
                String line = reader.readLine();
                if (line == null) { break; }
                result.add(line);
                metaData.parseLine(line);
                if (result.size() > 10) {
                    break;
                }
            }
        }
        return metaData;
    }


    private static class MetaData {
        private final String name;
        private String author;
        private String level;
        private String summary;
        private String targetProduct;
        private String source;
        private String prerequisites;
        private String[] technologies;

        private MetaData(String name) {
            this.name = name;
        }

        void parseLine(String line) {
            if (line.startsWith("Author")) {
                author = line.substring(line.indexOf(" ")).trim();
            } else if (line.startsWith("Technologies")) {
                technologies = line.substring(line.indexOf(" ")).trim().split(",");
            } else if (line.startsWith("Level")) {
                level = line.substring(line.indexOf(" ")).trim();
            } else if (line.startsWith("Summary")) {
                summary = line.substring(line.indexOf(" ")).trim();
            } else if (line.startsWith("Target Product")) {
                targetProduct = line.substring(line.indexOf(" ", 14)).trim();
            } else if (line.startsWith("source")) {
                source = line.substring(line.indexOf(" ")).trim();
            } else if (line.startsWith("Prerequisites")) {
                prerequisites = line.substring(line.indexOf(" ")).trim();
            }
        }

        String getTechnologiesAsString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < technologies.length; i++) {
                sb.append(technologies[i].trim());
                if (i < technologies.length - 1) {
                    sb.append(", ");
                }

            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "{" +
                    "name='" + name + '\'' +
                    ", author='" + author + '\'' +
                    ", level='" + level + '\'' +
                    ", summary='" + summary + '\'' +
                    ", targetProduct='" + targetProduct + '\'' +
                    ", source='" + source + '\'' +
                    ", prerequisites='" + prerequisites + '\'' +
                    ", technologies=" + Arrays.toString(technologies) +
                    '}';
        }
    }
}
