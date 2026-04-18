package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Key implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean spacePressed, enterPressed;
    public boolean cPressed;
    public boolean oPressed;
    public boolean lPressed;
    public boolean xPressed;
    public boolean anyKeyPressed;

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        setKey(e.getKeyCode(), true);
        updateAnyKeyStatus();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        setKey(e.getKeyCode(), false);
        updateAnyKeyStatus();
    }

    private void setKey(int code, boolean pressed) {
        switch (code) {
            // FIX 11: รองรับทั้ง WASD และปุ่มลูกศร (Arrow Keys)
            case KeyEvent.VK_W: case KeyEvent.VK_UP:    upPressed     = pressed; break;
            case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  downPressed   = pressed; break;
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  leftPressed   = pressed; break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: rightPressed  = pressed; break;

            case KeyEvent.VK_SPACE: spacePressed  = pressed; break;
            case KeyEvent.VK_ENTER: enterPressed  = pressed; break;
            case KeyEvent.VK_C:     cPressed      = pressed; break;
            case KeyEvent.VK_O:     oPressed      = pressed; break;
            case KeyEvent.VK_L:     lPressed      = pressed; break;
            case KeyEvent.VK_X:     xPressed      = pressed; break;
        }
    }

    private void updateAnyKeyStatus() {
        anyKeyPressed = upPressed || downPressed || leftPressed || rightPressed ||
                spacePressed || enterPressed || cPressed || oPressed ||
                lPressed || xPressed;
    }
}