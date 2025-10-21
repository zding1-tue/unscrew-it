package com.unscrewit;

import java.awt.*;
import javax.swing.*;


/**
 * 程序入口：设置系统外观，在事件派发线程 (EDT) 上创建窗口.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 忽略外观设置失败（非关键）
        }

        EventQueue.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}
