package backtool.service;

import backtool.util.FileUtil;
import backtool.util.LinkedHashMapUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 目录遍历引擎：遍历 + 过滤 + 注入计算函数，MD5 / LEN / LEN_MD5 三种计算复用。
 * 包私有，业务方只通过 DirMd5Digest / DirLenDigest / DirLenMd5Digest 使用。
 */
final class DigestWalker {
    private static final Logger logger = Logger.getLogger(DigestWalker.class.getName());

    private DigestWalker() {
    }

    /**
     * 遍历目录，对每个文件应用 calculator，返回 File -> 摘要值 的有序 Map。
     * 目录过滤（dirFilter）和文件过滤（fileFilter）分离，分别在不同阶段生效。
     */
    static LinkedHashMap<File, String> walkAndCalculate(File baseDir, FilterRules rules, Function<File, String> calculator) {
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
     */
    static LinkedHashMap<String, String> toRelativePath(LinkedHashMap<File, String> fileMap, String baseDirStr) {
        return LinkedHashMapUtil.alterKey(fileMap,
                file -> FileUtil.getSubPathUnix(file.getAbsolutePath(), baseDirStr));
    }
}
