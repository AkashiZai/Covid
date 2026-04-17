package main.entity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import main.GamePanel;
import main.Key;

public class Player extends Entity {
    GamePanel gp;
    Key keyH;

    public Player(GamePanel gp, Key keyH) {
        this.gp = gp;
        this.keyH = keyH;
        this.setDefaultValues();
        this.getPlayerImage();
    }

    public void setDefaultValues() {
        this.x = 100;
        this.y = 100;
        this.speed = 5;
        this.direction = "down";
        this.groundY = this.y;
    }

    public void getPlayerImage() {
        try {
            this.down1 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder34.png"));
            this.down2 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder35.png"));
            this.up1 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder34.png"));
            this.up2 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder35.png"));
            this.left1 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder19.png"));
            this.left2 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder20.png"));
            this.left3 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder21.png"));
            this.left4 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder22.png"));
            this.left5 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder23.png"));
            this.left6 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder24.png"));
            this.left7 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder25.png"));
            this.right1 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder26.png"));
            this.right2 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder27.png"));
            this.right3 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder28.png"));
            this.right4 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder29.png"));
            this.right5 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder30.png"));
            this.right6 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder31.png"));
            this.right7 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder32.png"));
            this.jump1 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder33.png"));
            this.jump2 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder34.png"));
            this.jump3 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder35.png"));
            this.jump4 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder36.png"));
            this.jump5 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder37.png"));
            this.jump6 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder38.png"));
            this.jump7 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder39.png"));
            this.jump8 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder40.png"));
            this.jump9 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder41.png"));
            this.fall1 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder63.png"));
            this.fall2 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder64.png"));
            this.fall3 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder65.png"));
            this.fall4 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder66.png"));
            this.fall5 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder67.png"));
            this.fall6 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder68.png"));
            this.fall7 = ImageIO.read(this.getClass().getResourceAsStream("/main/res/Nurse-16x16-Base-Folder69.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void update() {
        if (this.keyH.upPressed || this.keyH.downPressed || this.keyH.leftPressed || this.keyH.rightPressed) {

            if (this.keyH.upPressed) {
                this.direction = "up";
                this.y -= this.speed;
            }
            if (this.keyH.downPressed) {
                this.direction = "down";
                this.y += this.speed;
            }
            if (this.keyH.leftPressed) {
                this.direction = "left";
                this.x -= this.speed;
            }
            if (this.keyH.rightPressed) {
                this.direction = "right";
                this.x += this.speed;
            }

            // --- ลอจิกจำกัดกรอบให้อยู่ในกล่องต่อสู้ (Battle Box Boundary) ---
            if (GamePanel.playState) {
                int charSize = 64; // ขนาดการวาดของตัวละครตามที่คุณกำหนดไว้
                int boxThickness = 5; // ชดเชยความหนาของเส้นกล่อง

                // ล็อกแกน X (ซ้าย-ขวา) ให้อยู่ในกรอบ
                if (this.x < gp.boxX + boxThickness) {
                    this.x = gp.boxX + boxThickness;
                }
                if (this.x + charSize > gp.boxX + gp.boxWidth - boxThickness) {
                    this.x = gp.boxX + gp.boxWidth - charSize - boxThickness;
                }

                // ล็อกแกน Y (บน-ล่าง) ให้อยู่ในกรอบ
                if (this.y < gp.boxY + boxThickness) {
                    this.y = gp.boxY + boxThickness;
                }
                if (this.y + charSize > gp.boxY + gp.boxHeight - boxThickness) {
                    this.y = gp.boxY + gp.boxHeight - charSize - boxThickness;
                }
            }

            this.groundY = this.y;
            ++this.spriteCounter;

            if (!this.direction.equals("left") && !this.direction.equals("right")) {
                if (this.spriteCounter > 12) {
                    this.spriteNum = this.spriteNum == 1 ? 2 : 1;
                    this.spriteCounter = 0;
                }
            } else if (this.spriteCounter > 5) {
                ++this.spriteNum;
                if (this.spriteNum > 7) {
                    this.spriteNum = 1;
                }
                this.spriteCounter = 0;
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;
        if (this.jumping) {
            switch (this.spriteNum) {
                case 1 -> image = this.jump1;
                case 2 -> image = this.jump2;
                case 3 -> image = this.jump3;
                case 4 -> image = this.jump4;
                case 5 -> image = this.jump5;
                case 6 -> image = this.jump6;
                case 7 -> image = this.jump7;
                case 8 -> image = this.jump8;
                case 9 -> image = this.jump9;
                default -> image = this.jump9;
            }
        } else if (this.falling) {
            switch (this.spriteNum) {
                case 1 -> image = this.fall1;
                case 2 -> image = this.fall2;
                case 3 -> image = this.fall3;
                case 4 -> image = this.fall4;
                case 5 -> image = this.fall5;
                case 6 -> image = this.fall6;
                case 7 -> image = this.fall7;
                default -> image = this.fall1;
            }
        } else {
            switch (this.direction) {
                case "up":
                    image = this.spriteNum == 1 ? this.up1 : this.up2;
                    break;
                case "down":
                    image = this.spriteNum == 1 ? this.down1 : this.down2;
                    break;
                case "left":
                    if (this.spriteNum == 1) {
                        image = this.left1;
                    }

                    if (this.spriteNum == 2) {
                        image = this.left2;
                    }

                    if (this.spriteNum == 3) {
                        image = this.left3;
                    }

                    if (this.spriteNum == 4) {
                        image = this.left4;
                    }

                    if (this.spriteNum == 5) {
                        image = this.left5;
                    }

                    if (this.spriteNum == 6) {
                        image = this.left6;
                    }

                    if (this.spriteNum == 7) {
                        image = this.left7;
                    }

                    if (this.spriteNum > 7) {
                        image = this.left1;
                    }
                    break;
                case "right":
                    if (this.spriteNum == 1) {
                        image = this.right1;
                    }

                    if (this.spriteNum == 2) {
                        image = this.right2;
                    }

                    if (this.spriteNum == 3) {
                        image = this.right3;
                    }

                    if (this.spriteNum == 4) {
                        image = this.right4;
                    }

                    if (this.spriteNum == 5) {
                        image = this.right5;
                    }

                    if (this.spriteNum == 6) {
                        image = this.right6;
                    }

                    if (this.spriteNum == 7) {
                        image = this.right7;
                    }

                    if (this.spriteNum > 7) {
                        image = this.right1;
                    }
            }
        }

        if (image != null) {
            int var10002 = this.x;
            int var10003 = this.y;
            Objects.requireNonNull(this.gp);
            Objects.requireNonNull(this.gp);
            g2.drawImage(image, var10002, var10003, 64, 64, (ImageObserver)null);
        }

    }
}
