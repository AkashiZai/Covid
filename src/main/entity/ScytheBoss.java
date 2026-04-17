package main.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import main.GamePanel;

public class ScytheBoss extends PlayerValue {
    // --- ค่าคงที่สำหรับสถานะของบอส ---
    public static final int STATE_IDLE = 0;
    public static final int STATE_ATTACK = 1;
    public static final int STATE_DEATH = 2;

    // --- ตัวแปรสำหรับควบคุมอนิเมชัน ---
    GamePanel gp;
    public int state;
    int frameIndex;
    int animationCounter;
    int frameDelay;

    // --- อาร์เรย์สำหรับเก็บรูปภาพของแต่ละสถานะ ---
    public BufferedImage[] idleImages;
    public BufferedImage[] attackImages;
    public BufferedImage[] deathImages;

    // --- คุณสมบัติของบอส ---
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
        try {
            // 1. สถานะยืน (Idle): รูปที่ 4 ถึง 10 (รวม 7 รูป)
            idleImages = new BufferedImage[7];
            for (int i = 0; i < 7; i++) {
                idleImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/scytheboss/ScytheBoss" + (i + 4) + ".png"));
            }

            // 2. สถานะโจมตี (Attack): รูปที่ 12 ถึง 20 (รวม 9 รูป)
            attackImages = new BufferedImage[9];
            for (int i = 0; i < 9; i++) {
                attackImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/scytheboss/ScytheBoss" + (i + 12) + ".png"));
            }

            deathImages = new BufferedImage[16];
            for (int i = 0; i < 16; i++) {
                // i = 0 จะโหลดรูป ScytheBoss22.png, i = 1 โหลด ScytheBoss23.png ไปเรื่อยๆ
                deathImages[i] = ImageIO.read(getClass().getResourceAsStream("/main/scytheboss/ScytheBoss" + (i + 22) + ".png"));
            }

        } catch (Exception e) {
            System.err.println("เกิดข้อผิดพลาดในการโหลดรูปภาพบอส: " + e.getMessage());
        }
    }

    public void triggerAttack() {
        if (!isDead && state == STATE_IDLE) {
            state = STATE_ATTACK;
            frameIndex = 0;
            animationCounter = 0;
        }
    }

    public void update() {
        if (isDead) {
            return;
        }

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
                frameIndex++;
                if (frameIndex >= idleImages.length) {
                    frameIndex = 0;
                }
            } else if (state == STATE_ATTACK) {
                frameIndex++;
                if (frameIndex >= attackImages.length) {
                    state = STATE_IDLE;
                    frameIndex = 0;
                }
            } else if (state == STATE_DEATH) {
                frameIndex++;
                // โค้ดส่วนนี้จะทำงานถูกต้องโดยอัตโนมัติ เพราะเราเช็คความยาวของ deathImages.length
                if (frameIndex >= deathImages.length) {
                    frameIndex = deathImages.length - 1; // ล้มแล้วค้างที่เฟรมสุดท้าย
                    isDead = true;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage imageToDraw = null;

        switch (state) {
            case STATE_IDLE:
                if (idleImages != null && frameIndex < idleImages.length) {
                    imageToDraw = idleImages[frameIndex];
                }
                break;
            case STATE_ATTACK:
                if (attackImages != null && frameIndex < attackImages.length) {
                    imageToDraw = attackImages[frameIndex];
                }
                break;
            case STATE_DEATH:
                if (deathImages != null && frameIndex < deathImages.length) {
                    imageToDraw = deathImages[frameIndex];
                }
                break;
        }

        if (imageToDraw != null) {
            int originalWidth = imageToDraw.getWidth();
            int originalHeight = imageToDraw.getHeight();
            int desiredWidth = 300;
            int desiredHeight = 200;

            double scale = Math.min((double) desiredWidth / originalWidth, (double) desiredHeight / originalHeight);
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

            int drawX = this.x + (desiredWidth - scaledWidth) / 2;
            int drawY = this.y + (desiredHeight - scaledHeight) / 2;

            g2.drawImage(imageToDraw, drawX, drawY, scaledWidth, scaledHeight, null);

        } else if (state == STATE_IDLE) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            g2.drawString("[ SCYTHE BOSS ]", this.x + 20, this.y + 100);
        }
    }
}