package com.unscrewit;

import java.awt.*;
import javax.swing.*;


/**
 * 顶层窗口外壳：承载 GamePanel，并设置基础窗口属性.
 */
public class GameFrame extends JFrame {
    private final GamePanel gamePanel;

    /**
     * 创建游戏主窗口，设置基本属性并加入主画布 {@link GamePanel}.
     * <p>
     * 该窗口在程序启动时由 {@link Main} 创建。
     */
    public GameFrame() {
        super("Unscrew It! – Stage 1 · Step B2");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null); // 居中

        this.gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }
}
