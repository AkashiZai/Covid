package main.entity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import main.GamePanel;
import main.Key;

public class Player extends PlayerValue {

    GamePanel gp;
    Key keyH;

    public Player(GamePanel gp, Key keyH) {
        this.gp = gp;
        this.keyH = keyH;
        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        x = 100;
        y = 100;
        speed = 5;
        direction = "down";
        groundY = y;
    }

    public void getPlayerImage() {
        try {
            down1  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder34.png"));
            down2  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder35.png"));
            up1    = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder34.png"));
            up2    = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder35.png"));
            left1  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder19.png"));
            left2  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder20.png"));
            left3  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder21.png"));
            left4  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder22.png"));
            left5  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder23.png"));
            left6  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder24.png"));
            left7  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder25.png"));
            right1 = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder26.png"));
            right2 = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder27.png"));
            right3 = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder28.png"));
            right4 = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder29.png"));
            right5 = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder30.png"));
            right6 = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder31.png"));
            right7 = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder32.png"));
            jump1  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder33.png"));
            jump2  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder34.png"));
            jump3  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder35.png"));
            jump4  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder36.png"));
            jump5  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder37.png"));
            jump6  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder38.png"));
            jump7  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder39.png"));
            jump8  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder40.png"));
            jump9  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder41.png"));
            fall1  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder63.png"));
            fall2  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder64.png"));
            fall3  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder65.png"));
            fall4  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder66.png"));
            fall5  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder67.png"));
            fall6  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder68.png"));
            fall7  = ImageIO.read(getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder69.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {

            if (keyH.upPressed) {
                direction = "up";
                y -= speed;
            }
            if (keyH.downPressed) {
                direction = "down";
                y += speed;
            }
            if (keyH.leftPressed) {
                direction = "left";
                x -= speed;
            }
            if (keyH.rightPressed) {
                direction = "right";
                x += speed;
            }

            // --- ลอจิกจำกัดกรอบให้อยู่ในกล่องต่อสู้ (Battle Box Boundary) ---
            if (GamePanel.playState) {
                int charSize = 64;
                int boxThickness = 5;

                // ล็อกแกน X (ซ้าย-ขวา) ให้อยู่ในกรอบ
                if (x < gp.boxX + boxThickness) {
                    x = gp.boxX + boxThickness;
                }
                if (x + charSize > gp.boxX + gp.boxWidth - boxThickness) {
                    x = gp.boxX + gp.boxWidth - charSize - boxThickness;
                }

                // ล็อกแกน Y (บน-ล่าง) ให้อยู่ในกรอบ
                if (y < gp.boxY + boxThickness) {
                    y = gp.boxY + boxThickness;
                }
                if (y + charSize > gp.boxY + gp.boxHeight - boxThickness) {
                    y = gp.boxY + gp.boxHeight - charSize - boxThickness;
                }
            }

            groundY = y;
            spriteCounter++;

            if (!direction.equals("left") && !direction.equals("right")) {
                if (spriteCounter > 12) {
                    spriteNum = (spriteNum == 1) ? 2 : 1;
                    spriteCounter = 0;
                }
            } else if (spriteCounter > 5) {
                spriteNum++;
                if (spriteNum > 7) {
                    spriteNum = 1;
                }
                spriteCounter = 0;
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;

        if (jumping) {
            image = switch (spriteNum) {
                case 1 -> jump1;
                case 2 -> jump2;
                case 3 -> jump3;
                case 4 -> jump4;
                case 5 -> jump5;
                case 6 -> jump6;
                case 7 -> jump7;
                case 8 -> jump8;
                default -> jump9;
            };
        } else if (falling) {
            image = switch (spriteNum) {
                case 1 -> fall1;
                case 2 -> fall2;
                case 3 -> fall3;
                case 4 -> fall4;
                case 5 -> fall5;
                case 6 -> fall6;
                default -> fall7;
            };
        } else {
            switch (direction) {
                case "up"   -> image = (spriteNum == 1) ? up1 : up2;
                case "down" -> image = (spriteNum == 1) ? down1 : down2;
                case "left" -> {
                    image = switch (spriteNum) {
                        case 1 -> left1;
                        case 2 -> left2;
                        case 3 -> left3;
                        case 4 -> left4;
                        case 5 -> left5;
                        case 6 -> left6;
                        case 7 -> left7;
                        default -> left1;
                    };
                }
                case "right" -> {
                    image = switch (spriteNum) {
                        case 1 -> right1;
                        case 2 -> right2;
                        case 3 -> right3;
                        case 4 -> right4;
                        case 5 -> right5;
                        case 6 -> right6;
                        case 7 -> right7;
                        default -> right1;
                    };
                }
            }
        }

        // โค้ดเดิมมีการใช้ตัวแปรขยะจากการ Decompile ได้ถูกคลีนทิ้งแล้ว
        if (image != null) {
            g2.drawImage(image, x, y, 64, 64, null);
        }
    }
}