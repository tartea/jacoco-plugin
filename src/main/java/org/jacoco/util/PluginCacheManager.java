package org.jacoco.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jacoco.PathResult;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginCacheManager {

    private static final Map<String, PathResult> cache = new ConcurrentHashMap<>();

    /**
     * （可选）获取 .idea 下的缓存目录（适合小量、项目相关配置）
     */
    public static PathResult getProjectIdeaCacheDir(Project project) {
        return cache.get(project.getBasePath());
    }

    public static void saveCache(Project project, PathResult pathResult) {
        cache.put(project.getBasePath(), pathResult);
    }

}
