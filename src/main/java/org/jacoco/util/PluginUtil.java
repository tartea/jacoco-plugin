package org.jacoco.util;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jacoco.Constant;


public class PluginUtil {

    private static final IdeaPluginDescriptor IDEA_PLUGIN_DESCRIPTOR;

    static {
        PluginId pluginId = PluginId.getId(Constant.PLUGIN_ID);
        IDEA_PLUGIN_DESCRIPTOR = PluginManagerCore.getPlugin(pluginId);
    }

    /**
     * 获取插件路径
     *
     * @return String
     */
    public static String getPluginPath() {
        return IDEA_PLUGIN_DESCRIPTOR.getPluginPath().toString();
    }

    /**
     * 获取jacoco agent包路径
     *
     * @return String
     */
    public static String getJacocoAgentPath() {
        String pluginPath = getPluginPath();
        return pluginPath +"/lib/jacocoagent.jar";
    }


}