package org.jacoco;


import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JaCoCoToolWindowPanel {
    private final JPanel mainPanel;
    private final JCheckBox enableAgentCheckBox;
    private final JButton generateHtmlButton;
    private final JButton openHtmlButton;
    private final JButton deleteFilesButton;
    private final JTextArea parseResultArea;
    private final Project project;
    private final JaCoCoFileProcessor jacocoFileProcessor;
    private final JaCoCoPortSettings settings;

    public JaCoCoToolWindowPanel(Project project) {
        this.project = project;
        this.settings = JaCoCoPortSettings.getInstance(project);

        // 初始化处理器
        Path projectRoot = Paths.get(project.getBasePath());
        String execFilePath = projectRoot.resolve(Constant.JACOCO_DATA_DIR).resolve("jacoco.exec").toString();
        settings.setOutputPath(execFilePath);
        this.jacocoFileProcessor = new JaCoCoFileProcessor(project, Constant.JACOCO_DATA_DIR);

        // 初始化UI组件
        enableAgentCheckBox = new JCheckBox("启用JaCoCo Agent", settings.isEnableJaCoCoAgent());
        generateHtmlButton = new JButton("生成报告");
        deleteFilesButton = new JButton("删除报告");
        openHtmlButton = new JButton("打开报告");

        parseResultArea = new JTextArea(15, 40);
        parseResultArea.setEditable(false);
        parseResultArea.setLineWrap(true);
        parseResultArea.setWrapStyleWord(true);
        JScrollPane resultScrollPane = new JScrollPane(parseResultArea);
        resultScrollPane.setBorder(BorderFactory.createEtchedBorder());

        // 配置样式
        setButtonStyle(generateHtmlButton);
        setButtonStyle(openHtmlButton);
        setButtonStyle(deleteFilesButton);

        // 实时同步配置
        enableAgentCheckBox.addActionListener(e -> {
            settings.setEnableJaCoCoAgent(enableAgentCheckBox.isSelected());
        });

        // 构建布局
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部配置区域
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createTitledBorder("基础配置"));

        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkBoxPanel.add(enableAgentCheckBox);
        topPanel.add(checkBoxPanel);

        // 显示当前项目端口
        JLabel portLabel = new JLabel();
        updatePortLabel(portLabel);
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portPanel.add(portLabel);
        topPanel.add(portPanel);

        // 功能按钮区域
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4, 10, 0));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("功能操作"));
        buttonPanel.add(generateHtmlButton);
        buttonPanel.add(openHtmlButton);
        buttonPanel.add(deleteFilesButton);

        // 结果展示区域
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("解析结果"));
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);

        // 组装主面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(topPanel);

        JSeparator separator1 = new JSeparator();
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(separator1);
        contentPanel.add(Box.createVerticalStrut(8));

        contentPanel.add(buttonPanel);

        JSeparator separator2 = new JSeparator();
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(separator2);
        contentPanel.add(Box.createVerticalStrut(8));

        contentPanel.add(resultPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 绑定事件
        bindButtonEvents(portLabel);
    }

    // 更新端口显示标签
    private void updatePortLabel(JLabel portLabel) {
        int port = settings.getTcpserverPort();
        if (port == -1) {
            portLabel.setText("当前项目TCPServer端口：未分配（请启动项目）");
        } else {
            portLabel.setText(String.format("当前项目TCPServer端口：%d（专属端口，无冲突）", port));
        }
    }

    // 配置按钮样式
    private void setButtonStyle(JButton button) {
        button.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        button.setPreferredSize(new Dimension(120, 30));
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(UIManager.getColor("Button.foreground"));
    }

    // 绑定按钮事件
    private void bindButtonEvents(JLabel portLabel) {
        // 生成HTML报告
        generateHtmlButton.addActionListener(e -> {
            String result = jacocoFileProcessor.dumpTcpserverData();
            parseResultArea.setText(result);
            updatePortLabel(portLabel);
        });
        // 生成HTML报告
        openHtmlButton.addActionListener(e -> {
            jacocoFileProcessor.openHtml();
        });

        // 删除生成文件
        deleteFilesButton.addActionListener(e -> {
            String result = jacocoFileProcessor.deleteJacocoGeneratedFiles();
            parseResultArea.setText(result);
            updatePortLabel(portLabel);
        });
    }

    // 获取主面板
    public JPanel getMainPanel() {
        return mainPanel;
    }
}