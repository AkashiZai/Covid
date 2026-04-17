package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Key implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean spacePressed, enterPressed;
    public boolean cPressed;      // C — open/close upgrade screen
    public boolean anyKeyPressed;

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        anyKeyPressed = true;
        setKey(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        anyKeyPressed = false;
        setKey(e.getKeyCode(), false);
    }

    private void setKey(int code, boolean pressed) {
        switch (code) {
            case KeyEvent.VK_W     -> upPressed     = pressed;
            case KeyEvent.VK_S     -> downPressed   = pressed;
            case KeyEvent.VK_A     -> leftPressed   = pressed;
            case KeyEvent.VK_D     -> rightPressed  = pressed;
            case KeyEvent.VK_SPACE -> spacePressed  = pressed;
            case KeyEvent.VK_ENTER -> enterPressed  = pressed;
            case KeyEvent.VK_C     -> cPressed      = pressed;
        }
    }
}
