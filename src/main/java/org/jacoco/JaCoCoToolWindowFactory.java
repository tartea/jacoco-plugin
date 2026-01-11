package org.jacoco;



import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class JaCoCoToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建ToolWindow面板实例
        JaCoCoToolWindowPanel toolWindowPanel = new JaCoCoToolWindowPanel(project);

        // ========== 修复过时API：替换ContentFactory.SERVICE.getInstance() ==========
        // 新API：通过project获取ContentFactory（兼容所有IDEA版本）
        ContentFactory contentFactory = ContentFactory.getInstance();

        // 原过时代码（已注释）
        // ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content content = contentFactory.createContent(toolWindowPanel.getMainPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}