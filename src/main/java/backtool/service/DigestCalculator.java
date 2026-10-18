package backtool.service;

import backtool.util.FileUtil;
import backtool.util.LinkedHashMapUtil;
import backtool.util.Md5Util;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class DigestCalculator {
    private static final Logger logger = Logger.getLogger(DigestCalculator.class.getName());

    // ============== 公共能力：目录遍历 + 过滤 + 计算 ==============

    /**
     * 遍历目录，对每个文件应用 calculator，返回 File -> 摘要值 的有序 Map。
     * 目录过滤（dirFilter）和文件过滤（fileFilter）分离，分别在不同阶段生效。
     * 这是 MD5 / LEN / LEN_MD5 三种计算复用的公共组件。
     */
    private static LinkedHashMap<File, String> walkAndCalculate(File baseDir, FilterRules rules, Function<File, String> calculator) {
        LinkedHashMap<File, String> resultMap = new LinkedHashMap<>();
        Predicate<File> dirFilter = rules.dirPredicate();
        Predicate<File> fileFilter = rules.filePredicate();

        try {
            Files.walkFileTree(baseDir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public java.nio.file.FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 目录过滤：命中则跳过整棵子树（如 .Trashes / .fseventsd / .idea / .git）
                    if (dirFilter != null && dirFilter.test(dir.toFile())) {
                        return java.nio.file.FileVisitResult.SKIP_SUBTREE;
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    // 文件过滤：命中则跳过该文件（如 .DS_Store / ._ 前缀 / iml 后缀）
                    if (fileFilter != null && fileFilter.test(path.toFile())) {
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                    File file = path.toFile();
                    resultMap.put(file, calculator.apply(file));
                    return java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "遍历目录计算文件摘要失败: baseDir=" + baseDir.getAbsolutePath(), e);
            throw new RuntimeException("遍历目录计算文件摘要失败: " + baseDir.getAbsolutePath(), e);
        }

        return resultMap;
    }

    /**
     * 将 File -> 摘要值 的 Map 转换为 相对路径 -> 摘要值 的 Map。
     * 公共路径转换组件，三种计算复用。
     */
    private static LinkedHashMap<String, String> toRelativePath(LinkedHashMap<File, String> fileMap, String baseDirStr) {
        return LinkedHashMapUtil.alterKey(fileMap,
                file -> FileUtil.getSubPathUnix(file.getAbsolutePath(), baseDirStr));
    }

    // ============== MD5 计算 ==============

    public static LinkedHashMap<File, String> getDirMd5FileMap(String baseDirStr) {
        return getDirMd5FileMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<File, String> getDirMd5FileMap(String baseDirStr, FilterRules rules) {
        return walkAndCalculate(new File(baseDirStr), rules, Md5Util::getMD5);
    }

    public static LinkedHashMap<String, String> getDirMd5RelativePathMap(String baseDirStr) {
        return getDirMd5RelativePathMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<String, String> getDirMd5RelativePathMap(String baseDirStr, FilterRules rules) {
        return toRelativePath(getDirMd5FileMap(baseDirStr, rules), baseDirStr);
    }

    // ============== 长度（LEN）计算 ==============

    public static LinkedHashMap<File, String> getDirLenFileMap(String baseDirStr) {
        return getDirLenFileMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<File, String> getDirLenFileMap(String baseDirStr, FilterRules rules) {
        return walkAndCalculate(new File(baseDirStr), rules, file -> String.valueOf(file.length()));
    }

    public static LinkedHashMap<String, String> getDirLenRelativePathMap(String baseDirStr) {
        return getDirLenRelativePathMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<String, String> getDirLenRelativePathMap(String baseDirStr, FilterRules rules) {
        return toRelativePath(getDirLenFileMap(baseDirStr, rules), baseDirStr);
    }

    // ============== 长度+MD5（LEN_MD5）计算 ==============

    public static LinkedHashMap<File, String> getDirLenMd5FileMap(String baseDirStr) {
        return getDirLenMd5FileMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<File, String> getDirLenMd5FileMap(String baseDirStr, FilterRules rules) {
        return walkAndCalculate(new File(baseDirStr), rules,
                file -> Md5Util.getMD5(file) + "," + file.length());
    }

    public static LinkedHashMap<String, String> getDirLenMd5RelativePathMap(String baseDirStr) {
        return getDirLenMd5RelativePathMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<String, String> getDirLenMd5RelativePathMap(String baseDirStr, FilterRules rules) {
        return toRelativePath(getDirLenMd5FileMap(baseDirStr, rules), baseDirStr);
    }

    // ============== 按 MD5 分组 ==============

    public static LinkedHashMap<String, List<File>> groupFilesByMd5(List<File> files) {
        LinkedHashMap<String, List<File>> md5RevertMap_md5 = new LinkedHashMap<>();
        files.forEach(file -> {
            String md5 = Md5Util.getMD5(file);
            List<File> md5Files = md5RevertMap_md5.computeIfAbsent(md5, key -> new ArrayList<>());
            md5Files.add(file);
        });
        return md5RevertMap_md5;
    }

    // ============== 按路径比对 ==============

    /**
     * 按路径比对两个 Map，分类为 4 类（保持插入顺序）
     */
    public static class PathDiffResult {
        public final LinkedHashSet<String> samePaths = new LinkedHashSet<>();
        public final LinkedHashSet<String> modifiedPaths = new LinkedHashSet<>();
        public final LinkedHashSet<String> leftOnlyPaths = new LinkedHashSet<>();
        public final LinkedHashSet<String> rightOnlyPaths = new LinkedHashSet<>();
    }

    /**
     * 核心比对算法：以路径为键，比对两个 Map<String, ?> 的值
     * 内部统一转为 String 进行比较
     */
    public static PathDiffResult compareByPath(Map<String, ?> leftMap, Map<String, ?> rightMap) {
        PathDiffResult result = new PathDiffResult();

        // 遍历左侧：找出 SAME / MODIFIED / LEFT_ONLY
        for (Map.Entry<String, ?> entry : leftMap.entrySet()) {
            String path = entry.getKey();
            String leftValue = String.valueOf(entry.getValue());
            if (rightMap.containsKey(path)) {
                String rightValue = String.valueOf(rightMap.get(path));
                if (leftValue.equals(rightValue)) {
                    result.samePaths.add(path);
                } else {
                    result.modifiedPaths.add(path);
                }
            } else {
                result.leftOnlyPaths.add(path);
            }
        }

        // 遍历右侧：找出 RIGHT_ONLY
        for (String path : rightMap.keySet()) {
            if (!leftMap.containsKey(path)) {
                result.rightOnlyPaths.add(path);
            }
        }

        return result;
    }

    // ============== 清单读写 ==============

    /**
     * 读取清单文件，返回 LinkedHashMap (保持文件内顺序)
     * 支持格式：
     *   - .size:    path,size
     *   - .md5/.version: name,md5,length (跳过 // 开头的注释行)
     */
    public static LinkedHashMap<String, String> readManifest(Path file) throws IOException {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            boolean isSizeFile = file.toString().endsWith(".size");
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) {
                    continue;
                }
                if (isSizeFile) {
                    // .size 格式: path,size
                    int lastSep = line.lastIndexOf(',');
                    if (lastSep > 0) {
                        String path = line.substring(0, lastSep);
                        String size = line.substring(lastSep + 1);
                        result.put(path, size);
                    }
                } else {
                    // .md5/.version 格式: name,md5,length
                    String[] parts = line.split(",", 2); // 只拆成 name 和 value 两部分
                    if (parts.length >= 2) {
                        result.put(parts[0], parts[1]); // value 为 "md5,length" 或 "md5"
                    }
                }
            }
        }
        return result;
    }

    /**
     * 将 Map 写入为清单文件
     * 格式: key,value
     */
    public static void writeVersionStr(Path file, Map<String, ?> map) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        }
    }

    /**
     * 从字符串内容解析清单（格式同 readManifest 的非 .size 分支）
     */
    public static LinkedHashMap<String, String> parseVersionStr(String content) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("//")) {
                continue;
            }
            String[] parts = line.split(",", 2);
            if (parts.length >= 2) {
                result.put(parts[0], parts[1]);
            }
        }
        return result;
    }

}
