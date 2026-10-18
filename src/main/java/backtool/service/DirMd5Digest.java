package backtool.service;

import backtool.util.Md5Util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** 目录 MD5 摘要计算，另含按 MD5 分组 */
public final class DirMd5Digest {

    private DirMd5Digest() {
    }

    public static LinkedHashMap<File, String> getFileMap(String baseDirStr) {
        return getFileMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<File, String> getFileMap(String baseDirStr, FilterRules rules) {
        return DigestWalker.walkAndCalculate(new File(baseDirStr), rules, Md5Util::getMD5);
    }

    public static LinkedHashMap<String, String> getRelativePathMap(String baseDirStr) {
        return getRelativePathMap(baseDirStr, FilterRules.DEFAULT);
    }

    public static LinkedHashMap<String, String> getRelativePathMap(String baseDirStr, FilterRules rules) {
        return DigestWalker.toRelativePath(getFileMap(baseDirStr, rules), baseDirStr);
    }

    /** 按 MD5 分组 */
    public static LinkedHashMap<String, List<File>> groupFilesByMd5(List<File> files) {
        LinkedHashMap<String, List<File>> md5RevertMap_md5 = new LinkedHashMap<>();
        files.forEach(file -> {
            String md5 = Md5Util.getMD5(file);
            List<File> md5Files = md5RevertMap_md5.computeIfAbsent(md5, key -> new ArrayList<>());
            md5Files.add(file);
        });
        return md5RevertMap_md5;
    }
}
