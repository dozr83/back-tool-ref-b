package backtool.service;

import java.io.File;
import java.util.LinkedHashMap;

/** 目录长度（LEN）摘要计算 */
public final class DirLenDigest {

    private DirLenDigest() {
    }

    public static LinkedHashMap<File, String> getFileMap(String baseDirStr) {
        return getFileMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<File, String> getFileMap(String baseDirStr, FilterRules rules) {
        return DigestWalker.walkAndCalculate(new File(baseDirStr), rules, file -> String.valueOf(file.length()));
    }

    public static LinkedHashMap<String, String> getRelativePathMap(String baseDirStr) {
        return getRelativePathMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<String, String> getRelativePathMap(String baseDirStr, FilterRules rules) {
        return DigestWalker.toRelativePath(getFileMap(baseDirStr, rules), baseDirStr);
    }
}
