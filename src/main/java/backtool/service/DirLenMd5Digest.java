package backtool.service;

import backtool.util.Md5Util;

import java.io.File;
import java.util.LinkedHashMap;

/** 目录 长度+MD5（LEN_MD5）摘要计算 */
public final class DirLenMd5Digest {

    private DirLenMd5Digest() {
    }

    public static LinkedHashMap<File, String> getFileMap(String baseDirStr) {
        return getFileMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<File, String> getFileMap(String baseDirStr, FilterRules rules) {
        return DigestWalker.walkAndCalculate(new File(baseDirStr), rules,
                file -> Md5Util.getMD5(file) + "," + file.length());
    }

    public static LinkedHashMap<String, String> getRelativePathMap(String baseDirStr) {
        return getRelativePathMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<String, String> getRelativePathMap(String baseDirStr, FilterRules rules) {
        return DigestWalker.toRelativePath(getFileMap(baseDirStr, rules), baseDirStr);
    }
}
