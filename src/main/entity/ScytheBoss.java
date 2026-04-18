package main.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import main.GamePanel;

public class ScytheBoss extends PlayerValue {
    public static final int STATE_IDLE = 0;
    public static final int STATE_ATTACK = 1;
    public static final int STATE_DEATH = 2;

    GamePanel gp;
    public int state;
    int frameIndex;
    int animationCounter;
    int frameDelay;

    public BufferedImage[] idleImages;
    public BufferedImage[] attackImages;
    public BufferedImage[] deathImages;

    public int maxHp;
    public int hp;
    public boolean isDead;

    public ScytheBoss(GamePanel gp) {
        this.gp = gp;
        this.x = (gp.screenWidth / 2) - 150;
        this.y = 50;
        state = STATE_IDLE;
        frameIndex = 0;
        animationCounter = 0;
        frameDelay = 10;
        maxHp = 100;
        hp = maxHp;
        isDead = false;

        getBossImage();
    }

    public void getBossImage() {
        // กลับมาใช้ Index เดิมที่ตัดภาพว่างเปล่า (1-3) ทิ้งไป
        idleImages = new BufferedImage[7];
        for (int i = 0; i < 7; i++) {
            try { idleImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/scytheboss/ScytheBoss" + (i + 4) + ".png")); }
            catch (Exception e) { idleImages[i] = null; }
        }

        attackImages = new BufferedImage[9];
        for (int i = 0; i < 9; i++) {
            try { attackImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/scytheboss/ScytheBoss" + (i + 12) + ".png")); }
            catch (Exception e) { attackImages[i] = null; }
        }

        deathImages = new BufferedImage[16];
        for (int i = 0; i < 16; i++) {
            try { deathImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/scytheboss/ScytheBoss" + (i + 22) + ".png")); }
            catch (Exception e) { deathImages[i] = null; }
        }
    }

    public void triggerAttack() {
        if (!isDead && state == STATE_IDLE) {
            state = STATE_ATTACK;
            frameIndex = 0;
            animationCounter = 0;
        }
    }

    public void stopAttack() {
        state = STATE_IDLE;
        frameIndex = 0;
        frameDelay = 10;
    }

    public void update() {
        if (isDead) return;

        if (hp <= 0 && state != STATE_DEATH) {
            state = STATE_DEATH;
            frameIndex = 0;
            animationCounter = 0;
            return;
        }

        animationCounter++;
        if (animationCounter >= frameDelay) {
            animationCounter = 0;

            if (state == STATE_IDLE) {
                if (idleImages != null && idleImages.length > 0) {
                    frameIndex++;
                    if (frameIndex >= idleImages.length) frameIndex = 0;
                }
            } else if (state == STATE_ATTACK) {
                if (attackImages != null && attackImages.length > 0) {
                    frameIndex++;
                    if (frameIndex >= attackImages.length) {
                        frameIndex = 0; // วนลูปท่าโจมตี
                    }
                } else state = STATE_IDLE;
            } else if (state == STATE_DEATH) {
                if (deathImages != null && deathImages.length > 0) {
                    frameIndex++;
                    if (frameIndex >= deathImages.length) {
                        frameIndex = deathImages.length - 1;
                        isDead = true;
                    }
                } else isDead = true;
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage imageToDraw = null;

        switch (state) {
            case STATE_IDLE:   if (idleImages != null && frameIndex < idleImages.length) imageToDraw = idleImages[frameIndex]; break;
            case STATE_ATTACK: if (attackImages != null && frameIndex < attackImages.length) imageToDraw = attackImages[frameIndex]; break;
            case STATE_DEATH:  if (deathImages != null && frameIndex < deathImages.length) imageToDraw = deathImages[frameIndex]; break;
        }

        if (imageToDraw != null) {
            int originalWidth  = imageToDraw.getWidth();
            int originalHeight = imageToDraw.getHeight();
            int desiredWidth   = 300;
            int desiredHeight  = 200;

            double scale       = Math.min((double) desiredWidth / originalWidth, (double) desiredHeight / originalHeight);
            int scaledWidth    = (int) (originalWidth * scale);
            int scaledHeight   = (int) (originalHeight * scale);
            int drawX          = this.x + (desiredWidth - scaledWidth) / 2;
            int drawY          = this.y + (desiredHeight - scaledHeight) / 2;

            g2.drawImage(imageToDraw, drawX, drawY, scaledWidth, scaledHeight, null);

        } else {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString("[ SCYTHE BOSS IMAGE MISSING ]", this.x - 20, this.y + 100);
        }
    }
}