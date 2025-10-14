package com.unscrewit;

import com.unscrewit.logic.GameController;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 顶层窗口外壳：承载画布并在启动时初始化关卡与控制器.
 */
public class GameFrame extends JFrame {

    /** 游戏画布. */
    private final GamePanel gamePanel;

    /** 关卡状态. */
    private LevelState levelState;

    /**
     * 创建游戏主窗口并设置基础属性.
     */
    public GameFrame() {
        super("Unscrew It! – Stage 1 · Step B3");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        this.gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        initLevelAndController();
    }

    /**
     * 获取画布组件.
     *
     * @return 画布组件.
     */
    public GamePanel getGamePanel() {
        return gamePanel;
    }

    /**
     * 初始化关卡与控制器并完成接线.
     */
    private void initLevelAndController() {
        int w = Math.max(getWidth(), 900);
        int h = Math.max(getHeight(), 600);
        this.levelState = new LevelState(w, h);
        gamePanel.setLevelState(levelState);

        Runnable onWin = () -> SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, "You win!", "Unscrew It!", JOptionPane.INFORMATION_MESSAGE)
        );
        Runnable onFail = () -> SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, "Buffer overflow – You lose.", "Unscrew It!", JOptionPane.ERROR_MESSAGE)
        );

        GameController controller = new GameController(levelState, onWin, onFail);
        gamePanel.setController(controller);
    }
}
