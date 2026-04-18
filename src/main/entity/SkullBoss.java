package main.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import main.GamePanel;

public class SkullBoss extends PlayerValue {

    public static final int STATE_IDLE       = 0;
    public static final int STATE_ATTACK     = 1;
    public static final int STATE_DEATH      = 2;
    public static final int STATE_FIGHT_LOOP = 3;

    GamePanel gp;
    public int state;
    int frameIndex;
    int animationCounter;
    int frameDelay;
    int loopCount = 0;

    final int BASE_FRAME_DELAY = 7;

    public BufferedImage[] idleImages;
    public BufferedImage[] attackImages;
    public BufferedImage[] fightLoopImages;
    public BufferedImage[] deathImages;

    public int maxHp;
    public int hp;
    public boolean isDead;

    public SkullBoss(GamePanel gp) {
        this.gp = gp;
        this.x = (gp.screenWidth / 2) - 150;
        this.y = 50;
        state            = STATE_IDLE;
        frameIndex       = 0;
        animationCounter = 0;
        frameDelay       = BASE_FRAME_DELAY;
        maxHp  = 200;
        hp     = maxHp;
        isDead = false;

        getBossImage();
    }

    public void getBossImage() {
        idleImages = new BufferedImage[7];
        for (int i = 0; i < 7; i++) {
            try { idleImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/skullboss/SkullBoss" + (i + 4) + ".png")); }
            catch (Exception e) { idleImages[i] = null; }
        }

        attackImages = new BufferedImage[15];
        for (int i = 0; i < 15; i++) {
            try { attackImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/skullboss/SkullBoss" + (i + 12) + ".png")); }
            catch (Exception e) { attackImages[i] = null; }
        }

        fightLoopImages = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            try { fightLoopImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/skullboss/SkullBoss" + (i + 28) + ".png")); }
            catch (Exception e) { fightLoopImages[i] = null; }
        }

        deathImages = new BufferedImage[14];
        for (int i = 0; i < 14; i++) {
            try { deathImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/skullboss/SkullBoss" + (i + 34) + ".png")); }
            catch (Exception e) { deathImages[i] = null; }
        }
    }

    public void triggerAttack() {
        if (!isDead && state == STATE_IDLE) {
            int randomMove = (int) (Math.random() * 3);
            if (randomMove == 0) {
                state = STATE_ATTACK;
                frameDelay = 5;
            } else if (randomMove == 1) {
                state = STATE_FIGHT_LOOP;
                frameDelay = 5;
                loopCount = 0;
            } else {
                state = STATE_ATTACK;
                frameDelay = 3;
            }
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
            } else if (state == STATE_ATTACK) {
                frameIndex++;
                if (attackImages != null && frameIndex >= attackImages.length) {
                    frameIndex = 0;
                }
            } else if (state == STATE_FIGHT_LOOP) {
                frameIndex++;
                if (fightLoopImages != null && frameIndex >= fightLoopImages.length) {
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
            case STATE_ATTACK: if (attackImages != null && frameIndex < attackImages.length) imageToDraw = attackImages[frameIndex]; break;
            case STATE_FIGHT_LOOP: if (fightLoopImages != null && frameIndex < fightLoopImages.length) imageToDraw = fightLoopImages[frameIndex]; break;
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
            g2.drawString("[ SKULL BOSS IMAGE MISSING ]", this.x - 20, this.y + 100);
        }
    }
}