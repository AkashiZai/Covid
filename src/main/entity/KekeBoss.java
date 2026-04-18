package main.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import main.GamePanel;

public class KekeBoss extends PlayerValue {

    public static final int STATE_IDLE    = 0;
    public static final int STATE_ATTACK  = 1;
    public static final int STATE_DEATH   = 2;
    public static final int STATE_WALK    = 3;
    public static final int STATE_SPECIAL = 4;

    GamePanel gp;
    public int state;
    int frameIndex;
    int animationCounter;
    int frameDelay;

    final int BASE_FRAME_DELAY = 10;

    public BufferedImage[] idleImages;
    public BufferedImage[] walkImages;
    public BufferedImage[] specialImages;
    public BufferedImage[] attackImages;
    public BufferedImage[] deathImages;

    public int maxHp;
    public int hp;
    public boolean isDead;

    public KekeBoss(GamePanel gp) {
        this.gp = gp;
        this.x = (gp.screenWidth / 2) - 150;
        this.y = 50;
        state            = STATE_IDLE;
        frameIndex       = 0;
        animationCounter = 0;
        frameDelay       = BASE_FRAME_DELAY;
        maxHp  = 150;
        hp     = maxHp;
        isDead = false;

        getBossImage();
    }

    public void getBossImage() {
        idleImages = new BufferedImage[6];
        for (int i = 0; i < 6; i++) {
            try { idleImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/kekeboss/ZKekeBoss" + (i + 1) + ".png")); }
            catch(Exception e) { idleImages[i] = null; }
        }

        walkImages = new BufferedImage[12];
        for (int i = 0; i < 12; i++) {
            try { walkImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/kekeboss/ZKekeBoss" + (i + 8) + ".png")); }
            catch(Exception e) { walkImages[i] = null; }
        }

        specialImages = new BufferedImage[8];
        for (int i = 0; i < 8; i++) {
            try { specialImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/kekeboss/ZKekeBoss" + (i + 21) + ".png")); }
            catch(Exception e) { specialImages[i] = null; }
        }

        attackImages = new BufferedImage[6];
        for (int i = 0; i < 6; i++) {
            try { attackImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/kekeboss/ZKekeBoss" + (i + 30) + ".png")); }
            catch(Exception e) { attackImages[i] = null; }
        }

        deathImages = new BufferedImage[6];
        for (int i = 0; i < 6; i++) {
            try { deathImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/kekeboss/ZKekeBoss" + (i + 37) + ".png")); }
            catch(Exception e) { deathImages[i] = null; }
        }
    }

    public void triggerAttack() {
        if (!isDead && state == STATE_IDLE) {
            state = STATE_ATTACK;
            frameDelay = 8;
            frameIndex       = 0;
            animationCounter = 0;
        }
    }

    public void stopAttack() {
        state      = STATE_IDLE;
        frameIndex = 0;
        frameDelay = BASE_FRAME_DELAY;
    }

    public void update() {
        if (isDead) return;

        if (hp <= 0 && state != STATE_DEATH) {
            state            = STATE_DEATH;
            frameDelay       = BASE_FRAME_DELAY;
            frameIndex       = 0;
            animationCounter = 0;
            return;
        }

        animationCounter++;
        if (animationCounter >= frameDelay) {
            animationCounter = 0;

            if (state == STATE_IDLE) {
                frameIndex++;
                if (idleImages != null && frameIndex >= idleImages.length) frameIndex = 0;
            } else if (state == STATE_WALK) {
                frameIndex++;
                if (walkImages != null && frameIndex >= walkImages.length) {
                    frameIndex = 0;
                }
            } else if (state == STATE_SPECIAL) {
                frameIndex++;
                if (specialImages != null && frameIndex >= specialImages.length) {
                    frameIndex = 0;
                }
            } else if (state == STATE_ATTACK) {
                frameIndex++;
                if (attackImages != null && frameIndex >= attackImages.length) {
                    frameIndex = 0;
                }
            } else if (state == STATE_DEATH) {
                frameIndex++;
                if (deathImages != null && frameIndex >= deathImages.length) {
                    frameIndex = deathImages.length - 1;
                    isDead     = true;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage imageToDraw = null;

        switch (state) {
            case STATE_IDLE: if (idleImages != null && frameIndex < idleImages.length) imageToDraw = idleImages[frameIndex]; break;
            case STATE_WALK: if (walkImages != null && frameIndex < walkImages.length) imageToDraw = walkImages[frameIndex]; break;
            case STATE_SPECIAL: if (specialImages != null && frameIndex < specialImages.length) imageToDraw = specialImages[frameIndex]; break;
            case STATE_ATTACK: if (attackImages != null && frameIndex < attackImages.length) imageToDraw = attackImages[frameIndex]; break;
            case STATE_DEATH: if (deathImages != null && frameIndex < deathImages.length) imageToDraw = deathImages[frameIndex]; break;
        }

        if (imageToDraw != null) {
            int originalWidth  = imageToDraw.getWidth();
            int originalHeight = imageToDraw.getHeight();
            int desiredWidth   = 300;
            int desiredHeight  = 200;

            double scale       = Math.min((double) desiredWidth / originalWidth, (double) desiredHeight / originalHeight);
            int scaledWidth    = (int)(originalWidth  * scale);
            int scaledHeight   = (int)(originalHeight * scale);
            int drawX          = this.x + (desiredWidth  - scaledWidth)  / 2;
            int drawY          = this.y + (desiredHeight - scaledHeight) / 2;

            g2.drawImage(imageToDraw, drawX, drawY, scaledWidth, scaledHeight, null);

        } else {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString("[ KEKE BOSS IMAGE MISSING ]", this.x - 20, this.y + 100);
        }
    }
}