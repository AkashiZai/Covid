package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import main.entity.Player;
import main.entity.ScytheBoss;
import main.object.Obstacle;

public class GamePanel extends JPanel implements Runnable {

    // ── Screen & tile settings ────────────────────────────────────────
    public final int tileSize = 64;
    public final int screenWidth = 1024;
    public final int screenHeight = 768;
    private final int FPS = 60;

    // ── Game objects ──────────────────────────────────────────────────
    public final Key keyH = new Key();
    public Player player;
    public ScytheBoss scytheBoss;
    public Obstacle obstacle;
    private BattleGUI battleGUI;
    private Thread gameThread;

    // ── Menu / screen states ──────────────────────────────────────────
    public static boolean playState = false;
    public int menuState = 0;
    public final int titleScreen = 0;
    public final int selectBossScreen = 1;
    public final int gameOverScreen = 2;
    public final int youWinScreen = 3;
    public final int upgradeScreen = 4;

    // ── Battle box (area player moves in) ─────────────────────────────
    public int boxX, boxY;
    public final int boxWidth = 700;
    public final int boxHeight = 300;

    // ── Stats & progression ───────────────────────────────────────────
    int playerMaxHp = 20;
    int playerCurrentHp = 20;
    public int playerPoints = 0;
    public int playerAtkLv = 1;
    public int playerHpLv = 1;
    public final int maxStatLv = 10;
    public final int upgradeCost = 30;
    private int upgradeSelection = 0; // 0 = HP, 1 = ATK

    // ── Turn phases ───────────────────────────────────────────────────
    public int battlePhase = 0;
    public final int playerTurnPhase = 0;
    public final int bossAttackPhase = 1;

    // Player turn sub-states
    private static final int PT_IDLE = 0;
    private static final int PT_SHOWING_QUESTION = 1;
    private static final int PT_ANSWERED = 2;
    private int playerTurnState = PT_IDLE;

    private String feedbackText = "";
    private int feedbackTimer = 0;
    private boolean pendingBossPhase = false;
    private static final int FEEDBACK_FRAMES = 70;

    // ── Pattern queue (boss attack patterns) ─────────────────────────
    private final Random rng = new Random();
    private final List<Integer> patternQueue = new ArrayList<>();
    private int patternIndex = 0;
    private int cooldownTimer = 0;
    private boolean inCooldown = false;
    private boolean initialCooldownActive = false;
    private int initialCooldownTimer = 0;
    private static final int PATTERN_COOLDOWN = 60;
    private static final int INITIAL_COOLDOWN = 180; // 3 seconds

    // ── Misc ──────────────────────────────────────────────────────────
    private int resultTimer = 0;
    private int battleFrames = 0;
    private int lastGainedPoints = 0;
    private static final int RESULT_DISPLAY_FRAMES = 180;

    // Title screen animation
    private int titleAnimCounter = 0;
    private int currentTitleFrame = 1;
    private BufferedImage titleFrame1, titleFrame2;
    public int bossSelected = 1;

    public Font kanitFont;

    // ── Constructor ───────────────────────────────────────────────────

    public GamePanel() {
        player = new Player(this, keyH);
        scytheBoss = new ScytheBoss(this);
        obstacle = new Obstacle(this);
        battleGUI = new BattleGUI(screenWidth, screenHeight);
        battleGUI.loadQuestions("/main/res/questions.txt");

        boxX = (screenWidth - boxWidth) / 2;
        boxY = 280;

        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        addKeyListener(keyH);
        setFocusable(true);

        loadImages();
        loadThaiFont();
    }

    private void loadThaiFont() {
        try {
            InputStream is = getClass().getResourceAsStream("/main/res/Kanit-Bold.ttf");
            kanitFont = (is != null)
                    ? Font.createFont(Font.TRUETYPE_FONT, is)
                    : new Font("Tahoma", Font.BOLD, 24);
        } catch (Exception e) {
            kanitFont = new Font("Tahoma", Font.BOLD, 24);
        }
    }

    private void loadImages() {
        titleFrame1 = loadTitleImage("/main/title/title1.png");
        titleFrame2 = loadTitleImage("/main/title/title2.png");
    }

    private BufferedImage loadTitleImage(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null)
                return createTitlePlaceholder();
            BufferedImage image = ImageIO.read(is);
            return (image != null) ? image : createTitlePlaceholder();
        } catch (Exception e) {
            e.printStackTrace();
            return createTitlePlaceholder();
        }
    }

    private BufferedImage createTitlePlaceholder() {
        BufferedImage placeholder = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screenWidth, screenHeight);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("COVIDTALE", 80, 120);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Missing title assets", 80, 170);
        g.dispose();
        return placeholder;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    // ── Game loop (fixed 60 FPS) ──────────────────────────────────────
    @Override
    public void run() {
        double interval = 1_000_000_000.0 / FPS;
        double delta = 0;
        long last = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / interval;
            last = now;
            if (delta >= 1.0) {
                update();
                repaint();
                delta--;
            }
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────

    public void update() {
        if (menuState == gameOverScreen || menuState == youWinScreen) {
            if (++resultTimer >= RESULT_DISPLAY_FRAMES)
                resetToSelectBoss();
            return;
        }

        if (!playState) {
            updateMenus();
        } else {
            battleFrames++;
            if (battlePhase == playerTurnPhase)
                updatePlayerTurn();
            else
                updateBossAttack();

            scytheBoss.update();

            if (scytheBoss.isDead) {
                playState = false;
                menuState = youWinScreen;
                resultTimer = 0;
                lastGainedPoints = 100;
                playerPoints += lastGainedPoints;
                obstacle.stop();
            }
        }
    }

    // ── Menu navigation ───────────────────────────────────────────────

    private void updateMenus() {
        switch (menuState) {
            case 0 -> updateTitleScreen(); // titleScreen
            case 1 -> updateSelectBossScreen(); // selectBossScreen
            case 4 -> updateUpgradeScreen(); // upgradeScreen
        }
    }

    private void updateTitleScreen() {
        if (keyH.anyKeyPressed) {
            menuState = selectBossScreen;
            keyH.anyKeyPressed = keyH.enterPressed = false;
        }
        if (++titleAnimCounter > 30) {
            currentTitleFrame = (currentTitleFrame == 1) ? 2 : 1;
            titleAnimCounter = 0;
        }
    }

    private void updateSelectBossScreen() {
        if (keyH.leftPressed) {
            bossSelected = (bossSelected < 2) ? 3 : bossSelected - 1;
            keyH.leftPressed = false;
        }
        if (keyH.rightPressed) {
            bossSelected = (bossSelected > 2) ? 1 : bossSelected + 1;
            keyH.rightPressed = false;
        }

        if (keyH.enterPressed && bossSelected == 1) {
            startBattle();
            keyH.enterPressed = false;
        }
        if (keyH.cPressed) {
            menuState = upgradeScreen;
            keyH.cPressed = false;
        }
    }

    private void startBattle() {
        playState = true;
        battlePhase = playerTurnPhase;
        playerTurnState = PT_IDLE;
        playerMaxHp = 20 + (playerHpLv - 1) * 5;
        playerCurrentHp = playerMaxHp;
        battleFrames = 0;
        player.x = boxX + boxWidth / 2 - 32;
        player.y = boxY + boxHeight / 2 - 32;
    }

    private void updateUpgradeScreen() {
        if (keyH.upPressed) {
            upgradeSelection = (upgradeSelection + 1) % 2;
            keyH.upPressed = false;
        }
        if (keyH.downPressed) {
            upgradeSelection = (upgradeSelection + 1) % 2;
            keyH.downPressed = false;
        }

        if (keyH.enterPressed) {
            if (playerPoints >= upgradeCost) {
                if (upgradeSelection == 0 && playerHpLv < maxStatLv) {
                    playerHpLv++;
                    playerPoints -= upgradeCost;
                }
                if (upgradeSelection == 1 && playerAtkLv < maxStatLv) {
                    playerAtkLv++;
                    playerPoints -= upgradeCost;
                }
            }
            keyH.enterPressed = false;
        }
        if (keyH.cPressed) {
            menuState = selectBossScreen;
            keyH.cPressed = false;
        }
    }

    // ── Player turn ───────────────────────────────────────────────────

    private void updatePlayerTurn() {
        if (feedbackTimer > 0)
            feedbackTimer--;
        battleGUI.update();

        switch (playerTurnState) {
            case PT_IDLE -> {
                if (pendingBossPhase && feedbackTimer <= 0) {
                    pendingBossPhase = false;
                    battlePhase = bossAttackPhase;
                    buildPatternQueue();
                    return;
                }
                if (keyH.enterPressed && !scytheBoss.isDead) {
                    keyH.enterPressed = false;
                    battleGUI.nextQuestion();
                    playerTurnState = PT_SHOWING_QUESTION;
                }
            }
            case PT_SHOWING_QUESTION -> {
                if (keyH.upPressed) {
                    battleGUI.handleUp();
                    keyH.upPressed = false;
                }
                if (keyH.downPressed) {
                    battleGUI.handleDown();
                    keyH.downPressed = false;
                }
                if (keyH.enterPressed) {
                    battleGUI.confirmSelection();
                    keyH.enterPressed = false;
                }
                if (!battleGUI.isVisible() && battleGUI.isAnswered()) {
                    playerTurnState = PT_ANSWERED;
                    processAnswer(battleGUI.getResult());
                }
            }
            case PT_ANSWERED -> {
                if (feedbackTimer <= 0)
                    playerTurnState = PT_IDLE;
            }
        }
    }

    /** Applies damage on correct answer, or skips turn on wrong answer. */
    private void processAnswer(boolean correct) {
        if (correct) {
            int damage = 20 + (playerAtkLv - 1) * 5;
            scytheBoss.hp = Math.max(0, scytheBoss.hp - damage);
            feedbackText = "* โจมตี " + damage + " ดาเมจ!";
        } else {
            feedbackText = "* ตอบผิด! เสียโอกาสโจมตี!";
        }
        feedbackTimer = FEEDBACK_FRAMES;
        pendingBossPhase = true;
        playerTurnState = PT_ANSWERED;
    }

    // ── Boss attack turn ──────────────────────────────────────────────

    private void updateBossAttack() {
        if (scytheBoss.state == ScytheBoss.STATE_IDLE)
            scytheBoss.triggerAttack();

        // 3-second pause before first pattern fires
        if (initialCooldownActive) {
            if (++initialCooldownTimer >= INITIAL_COOLDOWN) {
                initialCooldownActive = false;
                startNextInQueue();
            }
            player.update();
            return;
        }

        if (!inCooldown) {
            if (!obstacle.isActive()) {
                if (patternIndex < patternQueue.size()) {
                    inCooldown = true;
                    cooldownTimer = 0;
                } else
                    battlePhase = playerTurnPhase;
            }
        } else if (++cooldownTimer >= PATTERN_COOLDOWN) {
            inCooldown = false;
            startNextInQueue();
        }

        obstacle.update();
        player.update();

        if (obstacle.checkHit(player.x, player.y, 64)) {
            playerCurrentHp = Math.max(0, playerCurrentHp - 1);
        }

        if (playerCurrentHp <= 0) {
            playState = false;
            menuState = gameOverScreen;
            resultTimer = 0;
            lastGainedPoints = battleFrames / 60; // 1 point per second survived
            playerPoints += lastGainedPoints;
            obstacle.stop();
        }
    }

    /**
     * Builds a shuffled queue of 1–3 random patterns for the boss's turn.
     * An initial 3-second delay is added before the first pattern fires.
     */
    private void buildPatternQueue() {
        patternQueue.clear();
        patternIndex = 0;
        inCooldown = false;
        cooldownTimer = 0;
        initialCooldownActive = true;
        initialCooldownTimer = 0;

        List<Integer> pool = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(pool, rng);
        int count = 1 + rng.nextInt(3);
        for (int i = 0; i < count; i++)
            patternQueue.add(pool.get(i));
    }

    private void startNextInQueue() {
        if (patternIndex < patternQueue.size())
            obstacle.startPattern(patternQueue.get(patternIndex++));
    }

    /** Resets everything back to the boss-select screen. */
    private void resetToSelectBoss() {
        playState = false;
        menuState = selectBossScreen;
        battlePhase = playerTurnPhase;
        resultTimer = 0;
        obstacle.stop();
        patternQueue.clear();
        patternIndex = cooldownTimer = initialCooldownTimer = 0;
        inCooldown = initialCooldownActive = pendingBossPhase = false;
        feedbackText = "";
        feedbackTimer = 0;
        playerTurnState = PT_IDLE;
        scytheBoss = new ScytheBoss(this);
        player.x = boxX + boxWidth / 2 - 32;
        player.y = boxY + boxHeight / 2 - 32;
    }

    // ── PAINT ─────────────────────────────────────────────────────────

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (menuState == gameOverScreen)
            drawGameOver(g2);
        else if (menuState == youWinScreen)
            drawYouWin(g2);
        else if (!playState)
            drawMenus(g2);
        else
            drawBattle(g2);

        g2.dispose();
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 80f), Color.RED, "GAME OVER", screenHeight / 2 - 60);
        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 36f), Color.YELLOW,
                "ได้รับแต้มความพยายาม: +" + lastGainedPoints + " Points", screenHeight / 2 + 10);

        String dots = ".".repeat((resultTimer / 20) % 4);
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 28f), Color.WHITE, "กำลังกลับไปหน้าเลือกบอส" + dots,
                screenHeight / 2 + 80);
    }

    private void drawYouWin(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 80f), Color.YELLOW, "YOU WIN!", screenHeight / 2 - 60);
        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 36f), new Color(100, 255, 100),
                "ได้รับแต้มชัยชนะ: +" + lastGainedPoints + " Points", screenHeight / 2 + 10);

        String dots = ".".repeat((resultTimer / 20) % 4);
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 28f), Color.WHITE, "กำลังกลับไปหน้าเลือกบอส" + dots,
                screenHeight / 2 + 80);
    }

    private void drawMenus(Graphics2D g2) {
        switch (menuState) {
            case 0 -> { // Title screen
                BufferedImage frame = (currentTitleFrame == 1) ? titleFrame1 : titleFrame2;
                if (frame != null)
                    g2.drawImage(frame, 0, 0, screenWidth, screenHeight, null);
            }
            case 1 -> { // Select boss
                g2.setColor(Color.WHITE);
                g2.setFont(kanitFont.deriveFont(Font.BOLD, 30f));
                g2.drawString("เลือกบอส: " + bossSelected, screenWidth / 2 - 100, screenHeight / 2);

                g2.setColor(new Color(150, 200, 255));
                g2.drawString("กด ENTER เพื่อเริ่มต่อสู้!", screenWidth / 2 - 130, screenHeight / 2 + 50);

                g2.setColor(Color.YELLOW);
                g2.drawString("[กด C] อัปเกรดตัวละคร (แต้ม: " + playerPoints + ")", screenWidth / 2 - 180,
                        screenHeight / 2 + 120);
            }
            case 4 -> drawUpgradeScreen(g2); // Upgrade screen
        }
    }

    private void drawUpgradeScreen(Graphics2D g2) {
        g2.setColor(new Color(20, 20, 30));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 48f), Color.WHITE, "อัปเกรดตัวละคร", 120);
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 32f), Color.YELLOW, "แต้มที่มี: " + playerPoints + " Points",
                180);

        g2.setFont(kanitFont.deriveFont(Font.BOLD, 28f));
        drawUpgradeOption(g2, 0, "HP", playerHpLv, 20 + (playerHpLv - 1) * 5, "พลังชีวิตตอนเริ่มสู้", 250);
        drawUpgradeOption(g2, 1, "ATK", playerAtkLv, 20 + (playerAtkLv - 1) * 5, "พลังโจมตี", 330);

        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 24f), new Color(180, 180, 255),
                "[ W / S เลื่อน ]     [ ENTER อัปเกรด ]     [ C กลับหน้าเลือกบอส ]",
                screenHeight - 80);
    }

    private void drawUpgradeOption(Graphics2D g2, int idx, String stat, int lv, int value, String valueLabel, int y) {
        String maxTag = (lv >= maxStatLv) ? " [ตันแล้ว]" : "  (ใช้ " + upgradeCost + " แต้ม)";
        String text = "อัปเกรด " + stat + " (เลเวล " + lv + "/" + maxStatLv + ")  | " + valueLabel + ": " + value
                + maxTag;

        if (upgradeSelection == idx) {
            Color bg = (idx == 0) ? new Color(80, 80, 150) : new Color(150, 80, 80);
            g2.setColor(bg);
            g2.fillRoundRect(80, y, screenWidth - 160, 60, 20, 20);
            g2.setColor(Color.WHITE);
        } else {
            g2.setColor(Color.GRAY);
        }
        g2.drawString(text, 120, y + 40);
    }

    private void drawBattle(Graphics2D g2) {
        scytheBoss.draw(g2);

        // Battle box border
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(5));
        g2.drawRect(boxX, boxY, boxWidth, boxHeight);
        g2.setStroke(new BasicStroke(1));

        obstacle.draw(g2);
        drawStatusText(g2);
        drawHpBars(g2);
        player.draw(g2);
        battleGUI.draw(g2);
    }

    private void drawStatusText(Graphics2D g2) {
        if (battlePhase == playerTurnPhase) {
            g2.setFont(kanitFont.deriveFont(Font.PLAIN, 20f));
            g2.setColor(Color.WHITE);
            g2.drawString("* Scythe Boss จ้องมองคุณอยู่...", boxX + 40, boxY + 55);

            if (feedbackTimer > 0) {
                boolean good = feedbackText.contains("ดาเมจ");
                g2.setColor(good ? new Color(100, 255, 100) : new Color(255, 100, 100));
                g2.setFont(kanitFont.deriveFont(Font.BOLD, 20f));
                g2.drawString(feedbackText, boxX + 40, boxY + 90);
            } else if (playerTurnState == PT_IDLE && !pendingBossPhase) {
                g2.setColor(new Color(255, 230, 100));
                g2.setFont(kanitFont.deriveFont(Font.BOLD, 20f));
                g2.drawString("[ ENTER ] ตอบคำถามเพื่อโจมตี!", boxX + 40, boxY + 90);
            }
        } else {
            g2.setColor(Color.RED);
            g2.setFont(kanitFont.deriveFont(Font.BOLD, 20f));
            int done = Math.min(patternIndex, patternQueue.size());
            int total = patternQueue.size();

            if (initialCooldownActive) {
                int secs = (INITIAL_COOLDOWN - initialCooldownTimer) / 60 + 1;
                g2.drawString("! บอสกำลังเตรียมตัวโจมตี... (" + secs + " วินาที)", boxX + 20, boxY + 55);
            } else {
                String coolStr = inCooldown ? "  (เตรียมรับมือ...)" : "";
                g2.drawString("! บอสกำลังโจมตี — หลบหลีก!  [" + done + " / " + total + "]" + coolStr, boxX + 20,
                        boxY + 55);
            }
        }
    }

    private void drawHpBars(Graphics2D g2) {
        // Player HP bar
        int sy = boxY + boxHeight + 40;
        g2.setFont(kanitFont.deriveFont(Font.PLAIN, 24f));
        g2.setColor(Color.WHITE);
        g2.drawString("YOU", boxX, sy);
        g2.setColor(Color.RED);
        g2.fillRect(boxX + 180, sy - 20, playerMaxHp * 5, 30);
        g2.setColor(Color.YELLOW);
        g2.fillRect(boxX + 180, sy - 20, playerCurrentHp * 5, 30);
        g2.setColor(Color.WHITE);
        g2.drawString(playerCurrentHp + " / " + playerMaxHp, boxX + 180 + playerMaxHp * 5 + 20, sy);

        // Boss HP bar
        int by = boxY - 30;
        g2.setColor(Color.WHITE);
        g2.drawString("BOSS HP", boxX, by);
        g2.setColor(Color.RED);
        g2.fillRect(boxX + 180, by - 20, scytheBoss.maxHp * 3, 30);
        g2.setColor(Color.YELLOW);
        g2.fillRect(boxX + 180, by - 20, Math.max(0, scytheBoss.hp) * 3, 30);
        g2.setColor(Color.WHITE);
        g2.drawString(scytheBoss.hp + " / " + scytheBoss.maxHp, boxX + 180 + scytheBoss.maxHp * 3 + 20, by);
    }

    private void drawCentered(Graphics2D g2, Font font, Color color, String text, int y) {
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, (screenWidth - g2.getFontMetrics().stringWidth(text)) / 2, y);
    }
}
