package org.jacoco;


import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.*;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JaCoCo文件处理器（终极兼容版）
 * 解决所有已知问题：
 * 1. ExecutionDataWriter.writeSessionInfo 方法不存在
 * 2. ExecutionDataWriter 不兼容 AutoCloseable
 * 3. data.writeTo 方法不存在
 * 4. 多项目端口冲突
 * 5. final类无法继承
 * 6. 资源管理不当
 */
public class JaCoCoFileProcessor {

    private static final String TARGET_HOST = "localhost"; // 目标应用 IP

    private final Project project;
    private final JaCoCoPortSettings settings;
    private final String jacocoDataDir;
    private final String htmlDirName = "html-report";

    public JaCoCoFileProcessor(Project project, String jacocoDataDir) {
        this.project = project;
        this.settings = JaCoCoPortSettings.getInstance(project);
        this.jacocoDataDir = jacocoDataDir;
    }

    // 获取项目数据目录
    private String getJacocoDataDirPath() {
        Path projectRoot = Paths.get(project.getBasePath());
        return projectRoot.resolve(jacocoDataDir).toAbsolutePath().toString();
    }


    // 从TCPServer导出数据（终极兼容版）
    public String dumpTcpserverData() {
        int tcpPort = settings.getTcpserverPort();
        if (tcpPort == -1) {
            System.out.println("错误：未找到项目分配的TCPServer端口！请先启动项目");
        }

        try (Socket socket = new Socket(TARGET_HOST, tcpPort)) {
            RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());

            ExecutionDataStore currentExecutionData = new ExecutionDataStore();
            SessionInfoStore currentSessionInfos = new SessionInfoStore();

            reader.setSessionInfoVisitor(currentSessionInfos);
            reader.setExecutionDataVisitor(currentExecutionData);

            writer.visitDumpCommand(true, false); // reset=false, dump=true
            reader.read();
            return createReport(currentExecutionData, currentSessionInfos);
        } catch (Exception e) {
            return "报告生成失败";
        }
    }

    /**
     * 生成报告
     *
     * @param executionData
     * @param sessionInfos
     * @throws IOException
     */
    public String createReport(ExecutionDataStore executionData, SessionInfoStore sessionInfos) throws IOException {
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);


        String dataDirPath = getJacocoDataDirPath();
        String htmlOutputDirPath = Paths.get(dataDirPath).resolve(htmlDirName).toString();
        String classesDirPath = Paths.get(project.getBasePath()).resolve("target/classes").toString();
        String srcDirPath = Paths.get(project.getBasePath()).resolve("src/main/java").toString();

        File classesDir = new File(classesDirPath); // 替换为实际路径
        if (!classesDir.exists()) {
            return "class 目录不存在，无法生成详细报告";
        }
        analyzer.analyzeAll(classesDir);

        IBundleCoverage bundleCoverage = coverageBuilder.getBundle("My Application");

        // 创建报告输出目录
        File reportDirFile = new File(htmlOutputDirPath);
        reportDirFile.mkdirs();

        HTMLFormatter htmlFormatter = new HTMLFormatter();
        FileMultiReportOutput output = new FileMultiReportOutput(reportDirFile);


        try {
            IReportVisitor visitor = htmlFormatter.createVisitor(output);

            // ✅ 关键修复：传入真实的 sessionInfos，不能为 null！
            visitor.visitInfo(sessionInfos.getInfos(), executionData.getContents());

            visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(new File(srcDirPath), "utf-8", 4));
            visitor.visitEnd();

        } finally {
            output.close();
        }
        return "HTML 报告已生成: \n" + reportDirFile.getAbsolutePath();
    }

    // 删除生成文件
    public String deleteJacocoGeneratedFiles() {
        int confirmResult = JOptionPane.showConfirmDialog(null,
                String.format("确定要删除%s目录下所有Jacoco生成的文件吗？\n项目端口：%d",
                        jacocoDataDir, settings.getTcpserverPort()),
                "删除确认",
                JOptionPane.YES_NO_OPTION);

        if (confirmResult != JOptionPane.YES_OPTION) {
            return "";
        }

        String dataDirPath = getJacocoDataDirPath();
        File dataDir = new File(dataDirPath);
        if (!dataDir.exists()) {
            return "未找到" + jacocoDataDir + "目录，无需删除！";
        }
        boolean deleteSuccess = deleteDir(dataDir);
        if (deleteSuccess) {
            return jacocoDataDir + "目录下所有Jacoco文件删除成功！";
        } else {
            return "部分文件删除失败，请手动清理" + dataDirPath + "目录！";
        }
    }

    // 递归删除目录
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDir(child);
                }
            }
        }
        return dir.delete();
    }

    /**
     * 打开报告文件
     */
    public String openHtml() {
        // 假设 HTML 文件在项目根目录下

        String dataDirPath = getJacocoDataDirPath();
        File dataDir = new File(dataDirPath);
        if (!dataDir.exists()) {
            return "未找到" + jacocoDataDir + "目录！";
        }

        String htmlOutputDirPath = Paths.get(dataDirPath).resolve(htmlDirName).toString();
        File htmlDataDir = new File(htmlOutputDirPath);
        if (!htmlDataDir.exists()) {
            return "未找到报告目录！";
        }

        File htmlFile = new File(htmlOutputDirPath, "index.html");
        if (!htmlFile.exists()) {
            // 可选：提示文件不存在
            return "未找到报告文件！";
        }

        try {
            String url = htmlDataDir.toURI().toString();
            BrowserUtil.browse(url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "报告成功打开";
    }
}