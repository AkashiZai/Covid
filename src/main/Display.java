package main;

import java.awt.Color;
import javax.swing.JFrame;

public class Display {

    public Display(String title) {
        GamePanel gamePanel = new GamePanel();

        JFrame frame = new JFrame(title);
        frame.add(gamePanel);
        frame.pack();
        frame.setBackground(Color.black);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        gamePanel.startGameThread();
        gamePanel.requestFocusInWindow();
    }
}
