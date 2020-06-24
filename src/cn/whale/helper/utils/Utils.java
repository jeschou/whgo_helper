package cn.whale.helper.utils;

import com.intellij.openapi.project.Project;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Utils {

    public static boolean isNotEmpty(String str) {
        return str != null && str.length() != 0;
    }

    public static String readText(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            copy(is, baos);
        } finally {
            safeClose(is);
        }

        return new String(baos.toByteArray());
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buff = new byte[8 * 1024];
        int len;
        while ((len = is.read(buff)) != -1) {
            os.write(buff, 0, len);
        }
    }

    public static void safeClose(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ignored) {

        }
    }

    public static BufferedReader reader(File f) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(f)));
    }

    public static List<String> readLines(File f) throws IOException {
        return readLines(new FileInputStream(f));
    }

    public static List<String> readLines(InputStream inputStream) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        }
        return list;
    }

    public static List<String> readLines(File f, String filter) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = reader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (filter == null || line.contains(filter))
                    list.add(line);
            }
        }
        return list;
    }

    public static List<String> readLinesBefore(File f, Function<String, Boolean> breakTest) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = reader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (breakTest.apply(line))
                    return list;
                list.add(line);
            }
        }
        return list;
    }

    public static String readFirstLine(File f, Predicate<String> predicate) throws IOException {
        try (BufferedReader br = reader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (predicate.test(line))
                    return line;
            }
        }
        return null;
    }

    public static void writeFile(File f, String text) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(text.getBytes());
        }
    }

    public static String substrBetweenQuote(String str) {
        int idx0 = str.indexOf('"');
        int idx1 = str.lastIndexOf('"');
        return str.substring(idx0 + 1, idx1);
    }

    public static String quote(String str) {
        return '"' + str + '"';
    }

    public static List<String> quote(List<String> strs) {
        List<String> list = new ArrayList<>(strs.size());

        for (String s : strs) {
            list.add(quote(s));
        }
        return list;
    }

    public static String substringBefore(String str, String sep) {
        int idx = str.indexOf(sep);
        if (idx == -1) {
            return str;
        }
        return str.substring(0, idx);
    }

    public static String relativePath(File root, File f) {
        Path rootPath = root.toPath();
        Path fPath = f.toPath();
        Path rPath = fPath.subpath(rootPath.getNameCount(), fPath.getNameCount());
        return rPath.toString().replace('\\', '/');
    }

    public static String join(String[] arr, int begin, int end, String sep) {
        StringBuilder sb = new StringBuilder();
        int last = Math.min(end, arr.length) - 1;
        for (int i = begin; i <= last; i++) {
            sb.append(arr[i]);
            if (i != last) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }

    public static String join(Collection<?> collection, String sep) {
        StringBuilder sb = new StringBuilder();
        int last = collection.size() - 1;
        int i = 0;
        for (Object obj : collection) {
            sb.append(obj);
            if (i < last) {
                sb.append(sep);
            }
            i++;
        }
        return sb.toString();
    }

    public static String getStackTrace(Throwable thr) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter pw = new PrintWriter(stringWriter)) {
            thr.printStackTrace(pw);
        }
        return stringWriter.toString();
    }

    public static void dump(Object... args) {
        File targetFile = new File(System.getProperty("user.home"), ".whgo_dump.log");
        if (!targetFile.exists()) {
            try {
                writeFile(targetFile, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            targetFile.deleteOnExit();
        }
        StringBuilder sb = new StringBuilder();
        try {

            for (Object obj : args) {
                sb.append(new Date().toString()).append("\t");
                sb.append(obj.toString()).append("\n\n");
            }
            appendFile(targetFile, sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendFile(File f, String text) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            raf.seek(f.length());
            raf.writeChars(text);
        }
    }

    public static File getWhgoProjectRoot(Project project) {
        File dir = new File(project.getBasePath());
        if (dir.isFile()) {
            dir = dir.getParentFile();
        }
        while (dir != null && !new File(dir, "go.mod").exists()) {
            dir = dir.getParentFile();
        }
        return dir;
    }

    public static String getProjectGoModuleName(File projectRoot) {
        String moduleDeclareLine = null;
        try {
            moduleDeclareLine = readFirstLine(new File(projectRoot, "go.mod"), (s) -> s.startsWith("module "));
        } catch (IOException e) {
            e.printStackTrace();
            return "whgo";
        }
        return moduleDeclareLine.trim().split(" ")[1];
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
