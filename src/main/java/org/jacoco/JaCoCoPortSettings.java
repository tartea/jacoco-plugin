package org.jacoco;


import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 独立的JaCoCo端口配置组件（不继承任何final类）
 * 存储项目的TCPServer端口和启用状态
 */
@State(
        name = "JaCoCoPortSettings",
        storages = @Storage("jacoco-port-settings.xml")
)
public class JaCoCoPortSettings implements PersistentStateComponent<JaCoCoPortSettings.State> {

    private State state = new State();

    // 获取项目的端口配置实例
    public static JaCoCoPortSettings getInstance(Project project) {
        return project.getService(JaCoCoPortSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // ========== 配置操作方法 ==========
    // 获取启用状态
    public boolean isEnableJaCoCoAgent() {
        return state.enableAgent;
    }

    // 设置启用状态
    public void setEnableJaCoCoAgent(boolean enable) {
        state.enableAgent = enable;
    }

    // 获取TCPServer端口
    public int getTcpserverPort() {
        return state.tcpserverPort;
    }

    // 设置TCPServer端口
    public void setTcpserverPort(int port) {
        state.tcpserverPort = port;
    }

    // 获取Exec文件输出路径
    public String getOutputPath() {
        return state.outputPath;
    }

    // 设置Exec文件输出路径
    public void setOutputPath(String path) {
        state.outputPath = path;
    }

    // ========== 状态类 ==========
    public static class State {
        // 启用状态
        public boolean enableAgent = false;
        // TCPServer端口
        public int tcpserverPort = -1;
        // Exec文件输出路径
        public String outputPath =  "jacoco.exec";
    }
}
