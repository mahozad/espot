import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class Main {

    // TODO: make gui for the app
    // Box drawing: '─' '┌' '┐' '└' '┘' '│' '┼' '├' '┤' '┬' '┴'

    // Multiline parenthesis: '⎛' '⎜' '⎝' '⎞' '⎟' '⎠'
    // Multiline bracket: '⎡' '⎢' '⎣' '⎤' '⎥' '⎦'
    // Multiline brace: '⎧' '⎨' '⎩' '⎫' '⎬' '⎭'
    // See https://en.wikipedia.org/wiki/Bracket#Encoding_in_digital_media
    private static final String CONTINUED_BRANCH = "├─── ";
    private static final String END_BRANCH = "└─── ";
    private static final String TAB = "   ";
    private static final String FOLDER_CHAR = "\uD83D\uDDC1";
    private static int filesCount = 0;
    private static String root;
    private static String filesTypeLabel;
    private static String filesTypeRegex;
    private static boolean absoluteRootPrinted;
    private static boolean filesCountPrinted;
    private static boolean foldersFirst;
    private static BufferedWriter bufferedWriter;

    public static void main(String[] args) throws Exception {
        new ProgressWindow().setVisible(true);
        initializeProperties();
        List<Boolean> ancestorsBranch = makeAncestorsBranchList();
        printRoot();
        String[] rootContents = new File(root).list();
        if (rootContents == null || rootContents.length == 0) {
            write(String.format("%s%s %s%n", TAB, "└─── ", ".: Empty :."));
        } else if (foldersFirst) {
            listDirectoriesFirst(root, 1, ancestorsBranch);
        } else {
            listAlphabetically(root, 1, ancestorsBranch);
        }
        printFilesCount();
        bufferedWriter.close();
        System.exit(0);
    }

    private static void initializeProperties() {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream("app-config.properties"));
            Path resultPath = Paths.get(properties.getProperty("result-file-path"));
            bufferedWriter = Files.newBufferedWriter(resultPath, Charset.forName("UTF-8"));
            root = properties.getProperty("root");
            filesTypeRegex = properties.getProperty("files-type-regex") + ".*";
            filesTypeLabel = properties.getProperty("files-type-label") + "s";
            absoluteRootPrinted = Boolean.valueOf(properties.getProperty("print-absolute-root"));
            filesCountPrinted = Boolean.valueOf(properties.getProperty("print-files-count"));
            foldersFirst = Boolean.valueOf(properties.getProperty("folders-first"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Boolean> makeAncestorsBranchList() {
        List<Boolean> list = new ArrayList<>();
        list.add(true);
        return list;
    }

    private static void printRoot() {
        if (absoluteRootPrinted) {
            write(String.format("%s%n", root));
        } else {
            int start = root.lastIndexOf('\\');
            if (start == -1) {
                write(String.format("%s%n", root));
            } else {
                write(String.format(".%s%n", root.substring(start)));
            }
        }
    }

    private static void listDirectoriesFirst(String path, int depth, List<Boolean> ancestorsBranch) {
        try {
            Iterator<Path> directoryIterator = getIterator(path, Filter.DIRECTORY);
            Iterator<Path> fileIterator = getIterator(path, Filter.FILE);
            listDirectories(depth, ancestorsBranch, directoryIterator, fileIterator);
            listFiles(depth, ancestorsBranch, fileIterator);
        } catch (Exception ignored) {
        }
    }

    private static void listDirectories(int depth, List<Boolean> ancestorsBranch, Iterator<Path> directoryIterator, Iterator<Path> fileIterator) {
        while (directoryIterator.hasNext()) {
            Path nextEntry = directoryIterator.next();
            indent(depth, ancestorsBranch);
            write(directoryIterator.hasNext() || fileIterator.hasNext() ? CONTINUED_BRANCH : END_BRANCH);
            write(String.format("%s %s%n", FOLDER_CHAR, nextEntry.getFileName()));
            setAncestorBranch(depth, ancestorsBranch, directoryIterator.hasNext() || fileIterator.hasNext());
            listDirectoriesFirst(nextEntry.toString(), depth + 1, ancestorsBranch);
        }
    }

    private static void listFiles(int depth, List<Boolean> ancestorsBranch, Iterator<Path> fileIterator) {
        while (fileIterator.hasNext()) {
            Path nextEntry = fileIterator.next();
            indent(depth, ancestorsBranch);
            write(fileIterator.hasNext() ? CONTINUED_BRANCH : END_BRANCH);
            incrementFileCount(nextEntry);
            write(String.format("%s%n", nextEntry.getFileName()));
            setAncestorBranch(depth, ancestorsBranch, fileIterator.hasNext());
        }
    }

    private static void listAlphabetically(String path, int depth, List<Boolean> ancestorsBranch) {
        try {
            Iterator<Path> iterator = getIterator(path, Filter.GENERIC);
            while (iterator.hasNext()) {
                indent(depth, ancestorsBranch);
                Path nextEntry = iterator.next();
                write(iterator.hasNext() ? CONTINUED_BRANCH : END_BRANCH);
                if (Files.isRegularFile(nextEntry)) {
                    incrementFileCount(nextEntry);
                    write(String.format("%s%n", nextEntry.getFileName()));
                } else if (Files.isDirectory(nextEntry)) {
                    write(String.format("%s %s%n", FOLDER_CHAR, nextEntry.getFileName()));
                    setAncestorBranch(depth, ancestorsBranch, iterator.hasNext());
                    listAlphabetically(nextEntry.toString(), depth + 1, ancestorsBranch);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void setAncestorBranch(int depth, List<Boolean> ancestorsBranch, boolean moreElementsAvailable) {
        if (ancestorsBranch.size() <= depth) {
            ancestorsBranch.add(moreElementsAvailable);
        } else {
            ancestorsBranch.set(depth, moreElementsAvailable);
        }
    }

    private static void write(String string) {
        try {
            bufferedWriter.write(string);
            System.out.print(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printFilesCount() {
        if (filesCountPrinted) {
            write(String.format("%n-----------------------%n%n"));
            write(String.format("Number of %s: %d%n", filesTypeLabel, filesCount));
        }
    }

    private static void indent(int depth, List<Boolean> ancestorsBranch) {
        write(TAB); // indent begging of all of the lines
        boolean shouldInsertTab = false;
        for (int i = 0; i < (depth - 1) * 2; i++) {
            if (shouldInsertTab) {
                write(TAB);
            } else {
                if (!ancestorsBranch.get(i / 2 + 1)) {
                    write(" ");
                } else {
                    write("│");
                }
            }
            shouldInsertTab = !shouldInsertTab;
        }
    }

    private static Iterator<Path> getIterator(String path, Filter filter) throws Exception {
        return Files.list(Paths.get(path)).filter(file -> {
            try {
                if (filter == Filter.DIRECTORY) {
                    return Files.isReadable(file) && !Files.isHidden(file) && Files.isDirectory(file);
                } else if (filter == Filter.FILE) {
                    return Files.isReadable(file) && !Files.isHidden(file) && Files.isRegularFile(file);
                } else { // filter == Filter.GENERIC
                    return Files.isReadable(file) && !Files.isHidden(file);
                }
            } catch (Exception e) {
                return false; // do not include this file
            }
        }).iterator();
    }

    private static void incrementFileCount(Path nextEntry) {
        try {
            if (filesCountPrinted) {
                String fileType = Files.probeContentType(nextEntry);
                if (fileType != null && fileType.matches(filesTypeRegex)) {
                    filesCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
