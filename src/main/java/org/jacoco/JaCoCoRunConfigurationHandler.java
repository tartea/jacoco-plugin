package org.jacoco;


import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.internal.statistic.eventLog.util.StringUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import org.jacoco.util.PluginUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SpringBoot运行配置补丁：自动注入JaCoCo Agent参数
 */
public class JaCoCoRunConfigurationHandler extends JavaProgramPatcher {
    // 端口分配基础范围（10000-19999）
    private static final int BASE_PORT = 10000;
    private static final int PORT_RANGE_END = 19999;
    // 项目-端口映射（确保同一项目使用固定端口）
    private static final Map<String, Integer> PROJECT_PORT_MAP = new HashMap<>();

    @Override
    public void patchJavaParameters(Executor executor, RunProfile runProfile, JavaParameters javaParameters) {

        RunConfiguration runConfiguration = (RunConfiguration) runProfile;

        Project project = ((RunConfiguration) runProfile).getProject();
        if (runConfiguration instanceof ApplicationConfiguration) {
            JaCoCoPortSettings settings = JaCoCoPortSettings.getInstance(project);
            // 决定是否加载
            if (!settings.isEnableJaCoCoAgent()) {
                return;
            }


            // 判断文件中是否有指定内容
            PsiClass mainClass = ((ApplicationConfiguration) runConfiguration).getMainClass();
            if (Objects.nonNull(mainClass)) {
                if (mainClass.hasAnnotation("org.springframework.boot.autoconfigure.SpringBootApplication")
                        || mainClass.hasAnnotation("org.springframework.cloud.client.SpringCloudApplication")) {
                    // 3. 获取项目唯一标识和根目录
                    String projectId = getProjectUniqueId(project);
                    String projectPath = project.getBasePath();
                    if (projectPath == null || StringUtil.isEmpty(projectId)) {
                        return;
                    }

                    // 4. 为项目分配唯一端口（同一项目固定端口）
                    int tcpPort = getProjectPort(projectId);
                    if (tcpPort == -1) {
                        return;
                    }

                    // 5. 构建Jacoco数据目录
                    Path jacocoDataDir = Paths.get(projectPath).resolve(Constant.JACOCO_DATA_DIR);
                    File dataDirFile = jacocoDataDir.toFile();
                    if (!dataDirFile.exists()) {
                        dataDirFile.mkdirs();
                    }

                    // 6. 获取JaCoCo Agent Jar包路径
                    String agentJarPath = PluginUtil.getJacocoAgentPath();
                    if (agentJarPath == null || !new File(agentJarPath).exists()) {
                        return;
                    }

                    // 7. 保存路径和端口到独立配置
                    String execFilePath = FileUtil.toSystemDependentName(jacocoDataDir.resolve("jacoco.exec").toString());
                    settings.setOutputPath(execFilePath);
                    settings.setTcpserverPort(tcpPort);

                    String packagePath = getPackage(javaParameters);
                    // 8. 构建动态端口的TCPServer Agent参数
                    String jacocoAgentParams = String.format(
                            "-javaagent:%s=output=tcpserver,port=%d,address=127.0.0.1,includes=%s,destfile=%s,append=true",
                            FileUtil.toSystemDependentName(agentJarPath),
                            tcpPort,
                            packagePath,
                            execFilePath
                    );

                    // 9. 注入JVM参数（避免重复注入）
                    if (!javaParameters.getVMParametersList().hasParameter(jacocoAgentParams)) {
                        javaParameters.getVMParametersList().add(jacocoAgentParams);
                    }
                }
            }
        }

    }

    /**
     * 获取项目唯一标识（项目名称+路径哈希，确保唯一性）
     */
    private String getProjectUniqueId(Project project) {
        String projectName = project.getName();
        String projectPath = project.getBasePath();
        if (StringUtil.isEmpty(projectName) || StringUtil.isEmpty(projectPath)) {
            return null;
        }
        // 生成唯一标识（路径哈希避免同名项目冲突）
        return projectName + "_" + Math.abs(projectPath.hashCode());
    }

    /**
     * 为项目分配唯一端口（优先使用已分配端口，无则自动查找可用端口）
     */
    private int getProjectPort(String projectId) {
        // 1. 检查项目是否已有分配的端口
        if (PROJECT_PORT_MAP.containsKey(projectId)) {
            int port = PROJECT_PORT_MAP.get(projectId);
            // 验证端口是否为当前项目的JaCoCo服务占用
            if (isPortAvailable(port)) {
                // 端口已释放，重新分配
                PROJECT_PORT_MAP.remove(projectId);
            } else if (isJaCoCoTcpserverPort(port)) {
                // 端口仍被当前项目的JaCoCo占用，复用
                return port;
            } else {
                // 端口被其他服务占用，移除映射
                PROJECT_PORT_MAP.remove(projectId);
            }
        }

        // 2. 自动查找可用端口（从BASE_PORT开始）
        for (int port = BASE_PORT; port <= PORT_RANGE_END; port++) {
            if (isPortAvailable(port)) {
                PROJECT_PORT_MAP.put(projectId, port);
                System.out.println("为项目[" + projectId + "]分配JaCoCo端口：" + port);
                return port;
            }
        }

        // 3. 无可用端口
        System.err.println("端口范围[" + BASE_PORT + "-" + PORT_RANGE_END + "]内无可用端口！");
        return -1;
    }

    /**
     * 检查端口是否可用
     */
    private boolean isPortAvailable(int port) {
        // 第一步：尝试绑定端口（基础检测）
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("127.0.0.1", port), 1);
            return true; // 端口未被占用，可用
        } catch (BindException e) {
            // 端口已被占用，第二步：探活验证是否为JaCoCo TCPServer
            return !isJaCoCoTcpserverPort(port);
        } catch (Exception e) {
            // 其他异常（如权限不足），判定为不可用
            return false;
        }
    }

    /**
     * 探活验证：检测指定端口是否运行JaCoCo TCPServer
     *
     * @param port 待检测端口
     * @return true=是JaCoCo TCPServer，false=非JaCoCo服务
     */
    private boolean isJaCoCoTcpserverPort(int port) {
        Socket socket = null;
        try {
            // 建立TCP连接（超时1秒）
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);

            // 发送JaCoCo握手指令（0x00），验证服务类型
            OutputStream out = socket.getOutputStream();
            out.write(0x00);
            out.flush();

            // 读取响应（JaCoCo TCPServer会返回0x00）
            InputStream in = socket.getInputStream();
            if (in.available() > 0 && in.read() == 0x00) {
                return true; // 是JaCoCo TCPServer
            }
        } catch (Exception e) {
            // 连接失败/响应异常，判定为非JaCoCo服务
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
        return false;
    }

    /**
     * 判断是否为SpringBoot项目
     */
    private boolean isSpringBootProject(Project project) {
        File pomFile = new File(project.getBasePath() + "/pom.xml");
        File buildGradleFile = new File(project.getBasePath() + "/build.gradle");

        if (pomFile.exists()) {
            try {
                return FileUtil.loadFile(pomFile).contains("spring-boot-starter");
            } catch (Exception e) {
                return false;
            }
        }
        if (buildGradleFile.exists()) {
            try {
                return FileUtil.loadFile(buildGradleFile).contains("spring-boot-starter");
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 查找JaCoCo Agent Jar包
     */
    private String findJaCoCoAgentJar() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL agentUrl = classLoader.getResource(Constant.JACOCO_AGENT_JAR_NAME);

            if (agentUrl != null) {
                String path = agentUrl.getPath();
                path = URLDecoder.decode(path, "UTF-8");

                if (path.startsWith("jar:file:")) {
                    path = path.substring(9, path.indexOf("!"));
                }

                return path;
            }

            String ideaHome = System.getProperty("idea.home.path");
            if (ideaHome != null) {
                File jacocoAgentFile = new File(ideaHome + "/plugins/jacoco/lib/" + Constant.JACOCO_AGENT_JAR_NAME);
                if (jacocoAgentFile.exists()) {
                    return jacocoAgentFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 获取包路径
     *
     * @param javaParameters
     * @return
     */
    private String getPackage(JavaParameters javaParameters) {
        String mainClass = javaParameters.getMainClass();
        if (mainClass == null) {
            return "*";
        }
        // 2. 推断 includes 包名（例如从 main class 包名）
        // 例如：com.example.demo.DemoApplication → com.example.demo
        int lastDot = mainClass.lastIndexOf('.');
        if (lastDot > 0) {
            return mainClass.substring(0, lastDot) + ".*";
        }
        return "*"; // fallback
    }

}
