package org.jacoco;

public interface Constant {


    /**
     * 插件ID
     */
    String PLUGIN_ID = "com.jacoco.jacoco-plugin";


    /**
     * 核心jar名称
     */
    // JaCoCo Agent Jar包名称（自动从依赖中获取）
    String JACOCO_AGENT_JAR_NAME = "jacocoagent.jar";


    // 统一目录：项目根目录下的jacoco-data
    String JACOCO_DATA_DIR = "jacoco-data";
}
