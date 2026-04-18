package main.object;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import main.GamePanel;

public class Obstacle {

    public static final int PATTERN_TRIPLE_LANES = 0;
    public static final int PATTERN_SPIRAL_COVID = 1;
    public static final int PATTERN_LAVA_FLOOR   = 2;
    public static final int PATTERN_QUAD_CORNERS = 3;

    private int     currentPattern = -1;
    private boolean active         = false;
    private int     timer          = 0;

    private float difficultyMultiplier = 1.0f;

    public void setDifficultyMultiplier(float multiplier) {
        this.difficultyMultiplier = multiplier;
    }

    // FIX: ใช้ CopyOnWriteArrayList เพื่อป้องกัน ConcurrentModificationException เวลาวาดตอนอัปเดตกระสุน
    private final List<Bullet> bullets = new CopyOnWriteArrayList<>();

    // ── Lava ──
    private float lavaHeight  = 0;
    private boolean lavaRising  = false;
    private boolean lavaHolding = false;
    private int     lavaDuration = 0;

    // ── Spiral ──
    private double spiralAngle = 0;

    private final GamePanel gp;
    private final Random    rng = new Random();

    public Obstacle(GamePanel gp) {
        this.gp = gp;
    }

    public void startNextPattern() {
        startPattern(rng.nextInt(4));
    }

    public void startPattern(int id) {
        currentPattern = id;
        active         = true;
        timer          = 0;
        bullets.clear();

        lavaHeight   = 0;
        lavaRising   = false;
        lavaHolding  = false;
        lavaDuration = 0;
        spiralAngle  = 0;
    }

    public void update() {
        if (!active) return;
        timer++;

        switch (currentPattern) {
            case PATTERN_TRIPLE_LANES -> updateTripleLanes();
            case PATTERN_SPIRAL_COVID -> updateSpiralCovid();
            case PATTERN_LAVA_FLOOR   -> updateLavaFloor();
            case PATTERN_QUAD_CORNERS -> updateQuadCorners();
        }

        // ลบกระสุนที่ตายแล้วทิ้ง
        bullets.removeIf(b -> !b.alive);
        for (Bullet b : bullets) b.move(gp);
    }

    public void draw(Graphics2D g2) {
        if (!active) return;
        if (currentPattern == PATTERN_LAVA_FLOOR && lavaHeight > 0) drawLava(g2);
        for (Bullet b : bullets) b.draw(g2);
    }

    public boolean checkHit(int px, int py, int pSize) {
        if (!active) return false;

        if (currentPattern == PATTERN_LAVA_FLOOR) {
            int lavaTop = gp.boxY + gp.boxHeight - (int) lavaHeight;
            if (py + pSize > lavaTop) return true;
        }

        Rectangle playerRect = new Rectangle(px + 10, py + 10, pSize - 20, pSize - 20);
        for (Bullet b : bullets) {
            if (b.alive && b.getBounds().intersects(playerRect)) return true;
        }
        return false;
    }

    public void stop() {
        active = false;
        bullets.clear();
        lavaHeight = 0;
    }

    public boolean isActive() { return active; }

    // ─────────────────────── PATTERN 0 : TRIPLE LANES ────────────────
    private static final int LANE_FIRE_INTERVAL = 50;
    private static final int LANE_DURATION      = 240;

    private void updateTripleLanes() {
        int fireInterval = Math.max(10, (int)(LANE_FIRE_INTERVAL / difficultyMultiplier));
        if (timer % fireInterval == 0) {
            int segment = gp.boxWidth / 4;
            for (int i = 1; i <= 3; i++) {
                int bx = gp.boxX + segment * i - 8;
                int by = gp.boxY + 10;
                float speed = 3f * difficultyMultiplier;

                float vx = (i == 1) ? -1.5f : (i == 3) ? 1.5f : 0f;

                bullets.add(new Bullet(bx, by, vx, speed, 14, Color.CYAN, BulletShape.CIRCLE));
            }
        }
        if (timer >= LANE_DURATION) active = false;
    }

    // ─────────────────────── PATTERN 1 : SPIRAL COVID ────────────────
    private static final int SPIRAL_ARMS     = 4;
    private static final int SPIRAL_DURATION = 400;

    private void updateSpiralCovid() {
        int fireInterval = Math.max(5, (int)(20 / difficultyMultiplier));
        if (timer % fireInterval == 0) {
            int cx = gp.boxX + gp.boxWidth  / 2;
            int cy = gp.boxY + gp.boxHeight / 2;
            float bulletSpeed = 2f * difficultyMultiplier;

            for (int arm = 0; arm < SPIRAL_ARMS; arm++) {
                double angle = spiralAngle + (Math.PI * 2 / SPIRAL_ARMS) * arm;
                float vx = (float)(Math.cos(angle) * bulletSpeed);
                float vy = (float)(Math.sin(angle) * bulletSpeed);
                bullets.add(new Bullet(cx, cy, vx, vy, 12, new Color(255, 120, 0), BulletShape.CIRCLE));
            }
            spiralAngle += 0.35;
        }
        if (timer >= SPIRAL_DURATION) active = false;
    }

    // ─────────────────────── PATTERN 2 : LAVA FLOOR ──────────────────
    private static final float LAVA_RISE_SPEED = 1.5f;
    private static final float LAVA_MAX        = 90f;
    private static final int   LAVA_HOLD_TIME  = 60;

    private void updateLavaFloor() {
        float riseSpeed = LAVA_RISE_SPEED * difficultyMultiplier;
        float maxHeight = LAVA_MAX * Math.min(difficultyMultiplier, 1.5f);
        int   holdTime  = Math.max(30, (int)(LAVA_HOLD_TIME / difficultyMultiplier));

        if (!lavaRising && !lavaHolding && lavaHeight == 0) {
            lavaRising = true;
        }

        if (lavaRising) {
            lavaHeight += riseSpeed;
            if (lavaHeight >= maxHeight) {
                lavaHeight   = maxHeight;
                lavaRising   = false;
                lavaHolding  = true;
                lavaDuration = 0;
            }
        } else if (lavaHolding) {
            lavaDuration++;
            if (lavaDuration >= holdTime) lavaHolding = false;
        } else if (lavaHeight > 0) {
            lavaHeight -= riseSpeed;
            if (lavaHeight <= 0) {
                lavaHeight = 0;
                active     = false;
            }
        }
    }

    private void drawLava(Graphics2D g2) {
        int lavaTop  = gp.boxY + gp.boxHeight - (int) lavaHeight;
        int lavaLeft = gp.boxX + 5;
        int lavaW    = gp.boxWidth - 10;
        int lavaH    = (int) lavaHeight;

        GradientPaint grad = new GradientPaint(
                lavaLeft, lavaTop,         new Color(255, 80, 0, 220),
                lavaLeft, lavaTop + lavaH, new Color(180, 0, 0, 180)
        );
        g2.setPaint(grad);
        g2.fillRect(lavaLeft, lavaTop, lavaW, lavaH);

        g2.setColor(new Color(255, 200, 0, 140));
        g2.setStroke(new BasicStroke(2));
        for (int bx = lavaLeft + 20; bx < lavaLeft + lavaW - 20; bx += 50) {
            int bub = (int)(Math.sin(timer * 0.1 + bx) * 6);
            g2.drawOval(bx, lavaTop + 5 + bub, 18, 10);
        }
        g2.setStroke(new BasicStroke(1));
        g2.setPaint(null);
    }

    // ─────────────────────── PATTERN 3 : QUAD CORNERS ────────────────
    private static final int QUAD_WAVE_INTERVAL = 90;
    private static final int QUAD_DURATION      = 300;

    private void updateQuadCorners() {
        int waveInterval = Math.max(30, (int)(QUAD_WAVE_INTERVAL / difficultyMultiplier));
        if (timer % waveInterval == 0) {
            int cx = gp.boxX + gp.boxWidth  / 2;
            int cy = gp.boxY + gp.boxHeight / 2;
            float bulletSpeed = 3f * difficultyMultiplier;

            int[][] corners = {
                    { gp.boxX + 10,               gp.boxY + 10                },
                    { gp.boxX + gp.boxWidth - 10,  gp.boxY + 10                },
                    { gp.boxX + 10,               gp.boxY + gp.boxHeight - 10  },
                    { gp.boxX + gp.boxWidth - 10,  gp.boxY + gp.boxHeight - 10 }
            };

            for (int[] c : corners) {
                double angleToCenter = Math.atan2(cy - c[1], cx - c[0]);

                for (int d = -1; d <= 1; d++) {
                    double spreadAngle = angleToCenter + (d * 0.3);
                    float  vx   = (float)(Math.cos(spreadAngle) * bulletSpeed);
                    float  vy   = (float)(Math.sin(spreadAngle) * bulletSpeed);
                    bullets.add(new Bullet(c[0], c[1], vx, vy, 14, new Color(180, 0, 255), BulletShape.DIAMOND));
                }
            }
        }
        if (timer >= QUAD_DURATION) active = false;
    }

    // ─────────────────────── INNER CLASS : Bullet ────────────────────
    private enum BulletShape { CIRCLE, DIAMOND }

    private class Bullet {
        float x, y, vx, vy;
        int size;
        Color color;
        BulletShape shape;
        boolean alive = true;

        Bullet(float x, float y, float vx, float vy, int size, Color color, BulletShape shape) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.size = size;
            this.color = color;
            this.shape = shape;
        }

        void move(GamePanel gp) {
            x += vx;
            y += vy;
            if (x < gp.boxX - size || x > gp.boxX + gp.boxWidth  + size ||
                    y < gp.boxY - size || y > gp.boxY + gp.boxHeight + size) {
                alive = false;
            }
        }

        Rectangle getBounds() {
            return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
        }

        void draw(Graphics2D g2) {
            if (!alive) return;
            int drawX = (int) x - size / 2;
            int drawY = (int) y - size / 2;

            g2.setColor(color);
            switch (shape) {
                case CIRCLE -> g2.fill(new Ellipse2D.Float(drawX, drawY, size, size));
                case DIAMOND -> {
                    int h  = size / 2;
                    int[] px = { drawX + h, drawX + size, drawX + h, drawX };
                    int[] py = { drawY,      drawY + h,    drawY + size, drawY + h };
                    g2.fillPolygon(px, py, 4);
                }
            }
            g2.setColor(color.brighter());
            g2.setStroke(new BasicStroke(1.5f));
            if (shape == BulletShape.CIRCLE) {
                g2.draw(new Ellipse2D.Float(drawX, drawY, size, size));
            }
            g2.setStroke(new BasicStroke(1));
        }
    }
}