package com.unscrewit;

import com.unscrewit.logic.GameController;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Top-level game window for Unscrew It.
 *
 * <p>Creates and lays out the main canvas and initializes the level state and
 * controller when the frame is shown.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Configure the JFrame (title, size, close behavior, layout).</li>
 *   <li>Host the {@link GamePanel} where rendering and input occur.</li>
 *   <li>Create the {@link LevelState} and wire it to the {@link GameController}.</li>
 *   <li>Provide user feedback dialogs for win/lose events.</li>
 * </ul>
 * </p>
 */
public class GameFrame extends JFrame {

    /** The main drawing and input panel. */
    private final GamePanel gamePanel;

    /** Mutable snapshot of the current level (board, buffer, history, etc.). */
    private LevelState levelState;

    /**
     * Constructs the game window and sets basic properties.
     */
    public GameFrame() {
        super("Unscrew It!");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null); // Center on screen.

        this.gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        initLevelAndController();
    }

    /**
     * Returns the main canvas component.
     *
     * @return the {@link GamePanel}
     */
    public GamePanel getGamePanel() {
        return gamePanel;
    }

    /**
     * Initializes the level state and connects it with the controller and UI.
     *
     * <p>Width/height fall back to a sane minimum on first show, then a controller
     * is created with win/fail callbacks that display message dialogs on the EDT.</p>
     */
    private void initLevelAndController() {
        int w = Math.max(getWidth(), 900);
        int h = Math.max(getHeight(), 600);
        this.levelState = new LevelState(w, h);
        gamePanel.setLevelState(levelState);

        // Define UI feedback callbacks (always run dialog creation on the EDT).
        Runnable onWin = () -> SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                this, 
                "You win!", 
                "Unscrew It!", 
                JOptionPane.INFORMATION_MESSAGE)
        );
        Runnable onFail = () -> SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                this, 
                "Buffer overflow – You lose.", 
                "Unscrew It!", 
                JOptionPane.ERROR_MESSAGE)
        );

        // Create controller and wire it to the panel.
        GameController controller = new GameController(levelState, onWin, onFail);
        gamePanel.setController(controller);
    }
}
