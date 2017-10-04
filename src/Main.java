import com.sun.istack.internal.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class Main {

    // TODO: make gui for the app
    // '─' '├' '└' '│'
    private static final String CONTINUED_BRANCH = "├─── ";
    private static final String END_BRANCH = "└─── ";
    private static final String TAB = "   ";
    private static final String FOLDER_CHAR = "\uD83D\uDDC1";
    private static int filesCount = 0;
    private static String root;
    private static String filesTypeLabel;
    private static String filesTypeRegex;
    private static boolean printAbsoluteRootEnabled;
    private static boolean printFilesCountEnabled;
    private static boolean foldersFirst;
    private static FileWriter writer;

    public static void main(String[] args) {
        initializeProperties();
        List<Boolean> ancestorTails = makeAncestorTailsList();
        printRoot();
        String[] rootContents = new File(root).list();
        if (rootContents == null || rootContents.length < 1) {
            write(String.format("%s%n   %s %s%n", root, "└─── ", "Empty Directory..."));
        } else if (foldersFirst) {
            listFoldersFirst(root, 1, ancestorTails);
        } else {
            listAlphabetically(root, 1, ancestorTails);
        }
        printFilesCount();
        closeWriter();
    }

    private static void initializeProperties() {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream("app-config.properties"));
            String resultFilePath = properties.getProperty("result-file-path");
            writer = new FileWriter(resultFilePath, false);
            root = properties.getProperty("root");
            filesTypeRegex = properties.getProperty("files-type-regex") + ".*";
            filesTypeLabel = properties.getProperty("files-type-label") + "s";
            printAbsoluteRootEnabled = Boolean.valueOf(properties.getProperty("print-absolute-root"));
            printFilesCountEnabled = Boolean.valueOf(properties.getProperty("print-files-count"));
            foldersFirst = Boolean.valueOf(properties.getProperty("folders-first"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Boolean> makeAncestorTailsList() {
        List<Boolean> list = new ArrayList<>();
        list.add(true);
        return list;
    }

    private static void printRoot() {
        if (printAbsoluteRootEnabled) {
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

    private static void listFoldersFirst(String path, int depth, List<Boolean> ancestorsTails) {
        Iterator<Path> directoryIterator = getDirectoryIterator(path);
        Iterator<Path> fileIterator = getFileIterator(path);
        if (directoryIterator != null && fileIterator != null) {
            listDirectories(depth, ancestorsTails, directoryIterator, fileIterator);
            listFiles(depth, ancestorsTails, fileIterator);
        }
    }

    private static void listDirectories(int depth, List<Boolean> ancestorsTails, Iterator<Path> directoryIterator, Iterator<Path> fileIterator) {
        while (directoryIterator.hasNext()) {
            Path nextEntry = directoryIterator.next();
            write(TAB); // indent the begging of all of the lines
            indent(depth, ancestorsTails);
            write(directoryIterator.hasNext() || fileIterator.hasNext() ? CONTINUED_BRANCH : END_BRANCH);
            write(String.format("%s %s%n", FOLDER_CHAR, nextEntry.getFileName()));
            setAncestorTail(depth, ancestorsTails, directoryIterator.hasNext() || fileIterator.hasNext());
            listFoldersFirst(nextEntry.toString(), depth + 1, ancestorsTails);
        }
    }

    private static void listFiles(int depth, List<Boolean> ancestorsTails, Iterator<Path> fileIterator) {
        while (fileIterator.hasNext()) {
            Path nextEntry = fileIterator.next();
            write(TAB); // indent the begging of all of the lines
            indent(depth, ancestorsTails);
            write(fileIterator.hasNext() ? CONTINUED_BRANCH : END_BRANCH);
            incrementFileCount(nextEntry);
            write(String.format("%s%n", nextEntry.getFileName()));
            setAncestorTail(depth, ancestorsTails, fileIterator.hasNext());
        }
    }

    private static void listAlphabetically(String path, int depth, List<Boolean> ancestorsTails) {
        Iterator<Path> iterator = getGenericIterator(path);
        if (iterator != null) {
            while (iterator.hasNext()) {
                write(TAB); // indent begging of all of the lines
                indent(depth, ancestorsTails);
                Path nextEntry = iterator.next();
                write(iterator.hasNext() ? CONTINUED_BRANCH : END_BRANCH);
                if (Files.isRegularFile(nextEntry)) {
                    incrementFileCount(nextEntry);
                    write(String.format("%s%n", nextEntry.getFileName()));
                } else if (Files.isDirectory(nextEntry)) {
                    write(String.format("%s %s%n", FOLDER_CHAR, nextEntry.getFileName()));
                    setAncestorTail(depth, ancestorsTails, iterator.hasNext());
                    listAlphabetically(nextEntry.toString(), depth + 1, ancestorsTails);
                }
            }
        }
    }

    private static void setAncestorTail(int depth, List<Boolean> ancestorsTails, boolean moreElementsAvailable) {
        if (ancestorsTails.size() <= depth) {
            ancestorsTails.add(moreElementsAvailable);
        } else {
            ancestorsTails.set(depth, moreElementsAvailable);
        }
    }

    private static void write(String string) {
        try {
            writer.write(string);
            System.out.print(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printFilesCount() {
        if (printFilesCountEnabled) {
            write(String.format("%n---------------------%n%nNumber of %s: %d%n", filesTypeLabel, filesCount));
        }
    }

    private static void indent(int depth, List<Boolean> ancestorsTails) {
        boolean shouldInsertTab = false;
        for (int i = 0; i < (depth - 1) * 2; i++) {
            if (shouldInsertTab) {
                write(TAB);
            } else {
                if (!ancestorsTails.get(i / 2 + 1)) {
                    write(" ");
                } else {
                    write("│");
                }
            }
            shouldInsertTab = !shouldInsertTab;
        }
    }

    private static void incrementFileCount(Path nextEntry) {
        try {
            if (printFilesCountEnabled) {
                String fileType = Files.probeContentType(nextEntry);
                if (fileType != null && fileType.matches(filesTypeRegex)) {
                    filesCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void closeWriter() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Iterator<Path> getGenericIterator(@Nullable String path) {
        try {
            Stream<Path> stream = Files.list(Paths.get(path)).filter(file -> {
                try {
                    return Files.isReadable(file) && !Files.isHidden(file);
                } catch (Exception e) {
                    return false;
                }
            });
            return stream.iterator();
        } catch (Exception e) {
            return null;
        }
    }

    private static Iterator<Path> getDirectoryIterator(@Nullable String path) {
        try {
            Stream<Path> stream = Files.list(Paths.get(path)).filter(file -> {
                try {
                    return Files.isReadable(file) && !Files.isHidden(file) && Files.isDirectory(file);
                } catch (Exception e) {
                    return false;
                }
            });
            return stream.iterator();
        } catch (Exception e) {
            return null;
        }
    }

    private static Iterator<Path> getFileIterator(@Nullable String path) {
        try {
            Stream<Path> stream = Files.list(Paths.get(path)).filter(entry -> {
                try {
                    return Files.isReadable(entry) && !Files.isHidden(entry) && Files.isRegularFile(entry);
                } catch (Exception e) {
                    return false;
                }
            });
            return stream.iterator();
        } catch (Exception e) {
            return null;
        }
    }
}


