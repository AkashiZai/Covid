package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

import main.entity.Player;
import main.entity.ScytheBoss;
import main.entity.SkullBoss;
import main.entity.KekeBoss;
import main.object.Obstacle;

public class GamePanel extends JPanel implements Runnable {

    // ── Screen settings ───────────────────────────────────────────────
    public final int screenWidth  = 1024;
    public final int screenHeight = 768;

    // ── Game objects ──────────────────────────────────────────────────
    public final Key keyH = new Key();
    public Player player;
    public ScytheBoss scytheBoss;
    public SkullBoss skullBoss;
    public KekeBoss kekeBoss;
    public Obstacle obstacle;
    private final BattleGUI battleGUI;
    private Thread gameThread;

    // ── Menu / screen states ──────────────────────────────────────────
    public static boolean playState = false;
    public int menuState = 0;

    public final int titleScreen      = 0;
    public final int selectBossScreen = 1;
    public final int gameOverScreen   = 2;
    public final int youWinScreen     = 3;
    public final int upgradeScreen    = 4;

    // ── Battle box (area player moves in) ─────────────────────────────
    public int boxX, boxY;
    public final int boxWidth  = 700;
    public final int boxHeight = 300;

    // ── ระบบด่าน (Stage System) ───────────────────────────────────────
    public int currentStage         = 1;
    public int highestUnlockedStage = 1;

    // ── Stats & progression ───────────────────────────────────────────
    int playerMaxHp     = 20;
    int playerCurrentHp = 20;
    public int playerPoints = 0;
    public int playerAtkLv  = 1;
    public int playerHpLv   = 1;
    public final int maxStatLv   = 20;
    public final int upgradeCost = 30;
    private int upgradeSelection = 0;

    // ── Turn phases ───────────────────────────────────────────────────
    public int battlePhase = 0;
    public final int playerTurnPhase = 0;
    public final int bossAttackPhase = 1;

    private static final int PT_IDLE             = 0;
    private static final int PT_SHOWING_QUESTION = 1;
    private static final int PT_ANSWERED         = 2;
    private int playerTurnState = PT_IDLE;

    private String  feedbackText     = "";
    private int     feedbackTimer    = 0;
    private boolean pendingBossPhase = false;
    private static final int FEEDBACK_FRAMES = 70;

    // ── Pattern queue ─────────────────────────────────────────────────
    private final Random rng = new Random();
    private final List<Integer> patternQueue = new ArrayList<>();

    private int     patternIndex          = 0;
    private int     cooldownTimer         = 0;
    private boolean inCooldown            = false;
    private boolean initialCooldownActive = false;
    private int     initialCooldownTimer  = 0;

    private static final int PATTERN_COOLDOWN = 60;
    private static final int INITIAL_COOLDOWN = 180;

    // ── Misc ──────────────────────────────────────────────────────────
    private int resultTimer      = 0;
    private int battleFrames     = 0;
    private int lastGainedPoints = 0;
    private static final int RESULT_DISPLAY_FRAMES = 180;

    private int titleAnimCounter  = 0;
    private int currentTitleFrame = 1;
    private BufferedImage titleFrame1, titleFrame2;
    private BufferedImage bgStage1, bgStage2, bgStage3;

    private BufferedImage boss1Img, boss2Img, boss3Img;

    public Font kanitFont;

    // ── Save system ───────────────────────────────────────────────────
    private int currentSaveSlot = 1;
    private String saveStatusText = "";
    private int    saveStatusTimer = 0;
    private static final int SAVE_STATUS_FRAMES = 120;

    // ── Constructor ───────────────────────────────────────────────────
    public GamePanel() {
        player     = new Player(this, keyH);
        scytheBoss = new ScytheBoss(this);
        skullBoss  = new SkullBoss(this);
        kekeBoss   = new KekeBoss(this);
        obstacle   = new Obstacle(this);
        battleGUI  = new BattleGUI(screenWidth, screenHeight);

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

        bgStage1 = loadBgImage("/main/bg/1.png");
        bgStage2 = loadBgImage("/main/bg/2.png");
        bgStage3 = loadBgImage("/main/bg/3.png");

        boss1Img = loadBgImage("/main/scytheboss/ScytheBoss1.png");
        boss2Img = loadBgImage("/main/skullboss/SkullBoss1.png");
        boss3Img = loadBgImage("/main/kekeboss/ZKekeBoss1.png");
    }

    private BufferedImage loadTitleImage(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return createTitlePlaceholder();
            BufferedImage image = ImageIO.read(is);
            return (image != null) ? image : createTitlePlaceholder();
        } catch (Exception e) {
            return createTitlePlaceholder();
        }
    }

    private BufferedImage loadBgImage(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception e) {
            return null;
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
        return placeholder;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        final int FPS   = 60;
        double interval = 1_000_000_000.0 / FPS;
        double delta    = 0;
        long   last     = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / interval;
            last   = now;
            if (delta >= 1.0) {
                update();
                repaint();
                delta--;
            } else {
                // FIX: ป้องกันอาการ CPU ทำงาน 100% (Busy waiting) โดยการยอมให้ Thread พัก
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void update() {
        if (menuState == gameOverScreen || menuState == youWinScreen) {
            if (++resultTimer >= RESULT_DISPLAY_FRAMES) {
                resetToSelectBoss();
            }
            return;
        }

        if (!playState) {
            updateMenus();
        } else {
            battleFrames++;
            if (battlePhase == playerTurnPhase) {
                updatePlayerTurn();
            } else {
                updateBossAttack();
            }

            updateCurrentBoss();

            if (isCurrentBossDead()) {
                playState   = false;
                menuState   = youWinScreen;
                resultTimer = 0;

                lastGainedPoints = 60 + (currentStage - 1) * 30;
                playerPoints    += lastGainedPoints;

                if (currentStage >= highestUnlockedStage && currentStage < 3) {
                    highestUnlockedStage = currentStage + 1;
                }

                obstacle.stop();
            }
        }
    }

    private void updateCurrentBoss() {
        switch (currentStage) {
            case 1 -> scytheBoss.update();
            case 2 -> skullBoss.update();
            case 3 -> kekeBoss.update();
        }
    }

    private boolean isCurrentBossDead() {
        return switch (currentStage) {
            case 1  -> scytheBoss.isDead;
            case 2  -> skullBoss.isDead;
            case 3  -> kekeBoss.isDead;
            default -> false;
        };
    }

    private void triggerCurrentBossAttack() {
        switch (currentStage) {
            case 1 -> scytheBoss.triggerAttack();
            case 2 -> skullBoss.triggerAttack();
            case 3 -> kekeBoss.triggerAttack();
        }
    }

    private int getCurrentBossState() {
        return switch (currentStage) {
            case 1  -> scytheBoss.state;
            case 2  -> skullBoss.state;
            case 3  -> kekeBoss.state;
            default -> ScytheBoss.STATE_IDLE;
        };
    }

    public int getCurrentBossMaxHp() {
        return switch (currentStage) {
            case 1  -> scytheBoss.maxHp;
            case 2  -> skullBoss.maxHp;
            case 3  -> kekeBoss.maxHp;
            default -> 100;
        };
    }

    public int getCurrentBossHp() {
        return switch (currentStage) {
            case 1  -> scytheBoss.hp;
            case 2  -> skullBoss.hp;
            case 3  -> kekeBoss.hp;
            default -> 0;
        };
    }

    private void damageCurrentBoss(int damage) {
        switch (currentStage) {
            case 1 -> scytheBoss.hp = Math.max(0, scytheBoss.hp - damage);
            case 2 -> skullBoss.hp  = Math.max(0, skullBoss.hp - damage);
            case 3 -> kekeBoss.hp   = Math.max(0, kekeBoss.hp - damage);
        }
    }

    private void updateMenus() {
        switch (menuState) {
            case titleScreen      -> updateTitleScreen();
            case selectBossScreen -> updateSelectBossScreen();
            case upgradeScreen    -> updateUpgradeScreen();
        }
    }

    private void updateTitleScreen() {
        if (keyH.anyKeyPressed) {
            menuState          = selectBossScreen;
            keyH.anyKeyPressed = false;
            keyH.enterPressed  = false;
        }
        if (++titleAnimCounter > 30) {
            currentTitleFrame = (currentTitleFrame == 1) ? 2 : 1;
            titleAnimCounter  = 0;
        }
    }

    private void updateSelectBossScreen() {
        if (keyH.leftPressed) {
            currentStage     = (currentStage > 1) ? currentStage - 1 : 3;
            keyH.leftPressed = false;
        }
        if (keyH.rightPressed) {
            currentStage      = (currentStage < 3) ? currentStage + 1 : 1;
            keyH.rightPressed = false;
        }

        if (keyH.upPressed) {
            currentSaveSlot = (currentSaveSlot > 1) ? currentSaveSlot - 1 : 3;
            keyH.upPressed = false;
        }
        if (keyH.downPressed) {
            currentSaveSlot = (currentSaveSlot < 3) ? currentSaveSlot + 1 : 1;
            keyH.downPressed = false;
        }

        if (keyH.enterPressed) {
            if (currentStage <= highestUnlockedStage) {
                startBattle();
            }
            keyH.enterPressed = false;
        }

        if (keyH.cPressed) {
            menuState     = upgradeScreen;
            keyH.cPressed = false;
        }

        if (keyH.oPressed) { saveGame(); keyH.oPressed = false; }
        if (keyH.lPressed) { loadGame(); keyH.lPressed = false; }
        if (keyH.xPressed) { deleteGame(); keyH.xPressed = false; }

        if (saveStatusTimer > 0) saveStatusTimer--;
    }

    private void startBattle() {
        switch (currentStage) {
            case 1 -> scytheBoss = new ScytheBoss(this);
            case 2 -> skullBoss  = new SkullBoss(this);
            case 3 -> kekeBoss   = new KekeBoss(this);
        }

        playState       = true;
        battlePhase     = playerTurnPhase;
        playerTurnState = PT_IDLE;
        playerMaxHp     = 20 + (playerHpLv - 1) * 5;
        playerCurrentHp = playerMaxHp;
        battleFrames    = 0;
        player.x        = boxX + boxWidth / 2 - 32;
        player.y        = boxY + boxHeight / 2 - 32;
    }

    private void updateUpgradeScreen() {
        if (keyH.upPressed) {
            upgradeSelection = (upgradeSelection + 1) % 2;
            keyH.upPressed   = false;
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
            menuState     = selectBossScreen;
            keyH.cPressed = false;
        }
    }

    private void updatePlayerTurn() {
        if (feedbackTimer > 0) feedbackTimer--;
        battleGUI.update();

        switch (playerTurnState) {
            case PT_IDLE -> {
                if (pendingBossPhase && feedbackTimer <= 0) {
                    pendingBossPhase = false;
                    battlePhase      = bossAttackPhase;
                    buildPatternQueue();
                    return;
                }
                if (keyH.enterPressed && !isCurrentBossDead()) {
                    keyH.enterPressed = false;
                    battleGUI.nextQuestion();
                    playerTurnState   = PT_SHOWING_QUESTION;
                }
            }
            case PT_SHOWING_QUESTION -> {
                if (keyH.upPressed)    { battleGUI.handleUp();         keyH.upPressed    = false; }
                if (keyH.downPressed)  { battleGUI.handleDown();       keyH.downPressed  = false; }
                if (keyH.enterPressed) { battleGUI.confirmSelection(); keyH.enterPressed = false; }

                if (!battleGUI.isVisible() && battleGUI.isAnswered()) {
                    playerTurnState = PT_ANSWERED;
                    processAnswer(battleGUI.getResult());
                }
            }
            case PT_ANSWERED -> {
                if (feedbackTimer <= 0) {
                    playerTurnState = PT_IDLE;
                }
            }
        }
    }

    private void processAnswer(boolean correct) {
        if (correct) {
            int damage = 1000 + (playerAtkLv - 1) * 5;
            damageCurrentBoss(damage);
            feedbackText = "* โจมตี " + damage + " ดาเมจ!";
        } else {
            feedbackText = "* ตอบผิด! เสียโอกาสโจมตี!";
        }
        feedbackTimer    = FEEDBACK_FRAMES;
        pendingBossPhase = true;
        playerTurnState  = PT_ANSWERED;
    }

    private void updateBossAttack() {
        if (getCurrentBossState() == ScytheBoss.STATE_IDLE) {
            triggerCurrentBossAttack();
        }

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
                    inCooldown    = true;
                    cooldownTimer = 0;
                } else {
                    battlePhase = playerTurnPhase;
                }
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
            playState   = false;
            menuState   = gameOverScreen;
            resultTimer = 0;

            int basePoints   = battleFrames / 120;
            int stageBonus   = (currentStage - 1) * 3;
            lastGainedPoints = basePoints + stageBonus;
            playerPoints    += lastGainedPoints;

            obstacle.stop();
        }
    }

    private void buildPatternQueue() {
        patternQueue.clear();
        patternIndex          = 0;
        inCooldown            = false;
        cooldownTimer         = 0;
        initialCooldownActive = true;
        initialCooldownTimer  = 0;

        float difficulty = switch (currentStage) {
            case 2  -> 1.5f;
            case 3  -> 2.0f;
            default -> 1.0f;
        };
        obstacle.setDifficultyMultiplier(difficulty);

        List<Integer> pool = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(pool, rng);

        int minPatterns = currentStage;
        int maxExtra    = Math.max(0, 3 - currentStage);
        int count       = Math.min(minPatterns + rng.nextInt(maxExtra + 1), pool.size());

        for (int i = 0; i < count; i++) {
            patternQueue.add(pool.get(i));
        }
    }

    private void startNextInQueue() {
        if (patternIndex < patternQueue.size()) {
            obstacle.startPattern(patternQueue.get(patternIndex++));
        }
    }

    private void resetToSelectBoss() {
        playState   = false;
        menuState   = selectBossScreen;
        battlePhase = playerTurnPhase;
        resultTimer = 0;
        obstacle.stop();
        patternQueue.clear();

        patternIndex         = 0;
        cooldownTimer        = 0;
        initialCooldownTimer = 0;
        inCooldown           = false;
        initialCooldownActive = false;
        pendingBossPhase     = false;
        feedbackText         = "";
        feedbackTimer        = 0;
        playerTurnState      = PT_IDLE;

        scytheBoss = new ScytheBoss(this);
        skullBoss  = new SkullBoss(this);
        kekeBoss   = new KekeBoss(this);

        obstacle.setDifficultyMultiplier(1.0f);
        player.x = boxX + boxWidth / 2 - 32;
        player.y = boxY + boxHeight / 2 - 32;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (menuState == gameOverScreen) {
            drawGameOver(g2);
        } else if (menuState == youWinScreen) {
            drawYouWin(g2);
        } else if (!playState) {
            drawMenus(g2);
        } else {
            drawBattle(g2);
        }

        g2.dispose();
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);
        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 80f), Color.RED, "GAME OVER", screenHeight / 2 - 60);
        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 36f), Color.YELLOW, "ได้รับแต้มความพยายาม: +" + lastGainedPoints + " Points", screenHeight / 2 + 10);
        String dots = ".".repeat((resultTimer / 20) % 4);
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 28f), Color.WHITE, "กำลังกลับไปหน้าเลือกด่าน" + dots, screenHeight / 2 + 80);
    }

    private void drawYouWin(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);
        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 80f), Color.YELLOW, "YOU WIN!", screenHeight / 2 - 70);
        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 36f), new Color(100, 255, 100), "ได้รับแต้มชัยชนะ: +" + lastGainedPoints + " Points", screenHeight / 2 - 10);

        if (currentStage < 3) {
            drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 28f), new Color(255, 220, 80), "ปลดล็อคด่านที่ " + (currentStage + 1) + " แล้ว!", screenHeight / 2 + 45);
        } else {
            drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 28f), new Color(255, 80, 255), "คุณเอาชนะทุกด่านแล้ว! ยอดเยี่ยมมาก!", screenHeight / 2 + 45);
        }

        String dots = ".".repeat((resultTimer / 20) % 4);
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 28f), Color.WHITE, "กำลังกลับไปหน้าเลือกด่าน" + dots, screenHeight / 2 + 110);
    }

    private void drawMenus(Graphics2D g2) {
        switch (menuState) {
            case titleScreen -> {
                BufferedImage frame = (currentTitleFrame == 1) ? titleFrame1 : titleFrame2;
                if (frame != null) g2.drawImage(frame, 0, 0, screenWidth, screenHeight, null);
            }
            case selectBossScreen -> drawSelectStageScreen(g2);
            case upgradeScreen    -> drawUpgradeScreen(g2);
        }
    }

    private void drawSelectStageScreen(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 42f), Color.WHITE, "เลือกด่าน", 50);

        int cardW  = 220, cardH = 200;
        int startX = (screenWidth - (cardW * 3 + 40 * 2)) / 2;
        int cardY  = 90;

        String[] stageNames = { "ด่าน 1", "ด่าน 2", "ด่าน 3" };
        String[] bossNames  = { "Scythe Boss", "Skull Boss", "Slime Infected Covid Boss" };
        String[] diffLabels = { "ปกติ", "ยากขึ้น ×1.5", "ยากขึ้น ×2.0" };
        Color[] cardColors  = {
                new Color(30, 80, 160),
                new Color(130, 40, 140),
                new Color(160, 50, 20)
        };

        for (int i = 0; i < 3; i++) {
            int stageNum = i + 1;
            int cx       = startX + i * (cardW + 40);
            boolean unlocked = stageNum <= highestUnlockedStage;
            boolean selected = stageNum == currentStage;

            Color bgColor = unlocked ? cardColors[i] : new Color(50, 50, 50);
            if (!selected) bgColor = bgColor.darker();

            g2.setColor(bgColor);
            g2.fillRoundRect(cx, cardY, cardW, cardH, 20, 20);

            if (selected) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(cx, cardY, cardW, cardH, 20, 20);
                g2.setStroke(new BasicStroke(1));
            }

            if (!unlocked) {
                g2.setFont(kanitFont.deriveFont(Font.BOLD, 28f));
                g2.setColor(new Color(120, 120, 120));
                drawCenteredInRect(g2, "LOCKED", cx, cardY + 30, cardW, 40);

                g2.setFont(kanitFont.deriveFont(Font.BOLD, 20f));
                drawCenteredInRect(g2, stageNames[i], cx, cardY + 80, cardW, 28);
            } else {
                g2.setFont(kanitFont.deriveFont(Font.BOLD, 26f));
                g2.setColor(Color.WHITE);
                drawCenteredInRect(g2, stageNames[i], cx, cardY + 10, cardW, 36);

                BufferedImage img = (i == 0) ? boss1Img : (i == 1) ? boss2Img : boss3Img;
                if(img != null) {
                    g2.drawImage(img, cx + (cardW - 64) / 2, cardY + 50, 64, 64, null);
                }

                g2.setFont(kanitFont.deriveFont(Font.PLAIN, 18f));
                g2.setColor(new Color(200, 200, 255));
                drawCenteredInRect(g2, bossNames[i], cx, cardY + 120, cardW, 26);

                g2.setFont(kanitFont.deriveFont(Font.PLAIN, 16f));
                g2.setColor(new Color(255, 200, 80));
                drawCenteredInRect(g2, diffLabels[i], cx, cardY + 150, cardW, 24);
            }
        }

        int msgY = cardY + cardH + 45;

        if (currentStage <= highestUnlockedStage) {
            drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 26f), new Color(100, 255, 150),
                    "[ ENTER ] เริ่มด่านที่ " + currentStage, msgY);
        } else {
            drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 26f), new Color(255, 100, 100),
                    "ด่านนี้ยังล็อคอยู่ — ชนะด่านก่อนหน้าก่อน", msgY);
        }

        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 22f), Color.GRAY,
                "A / D  เลื่อนเลือกด่าน", msgY + 35);
        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 22f), Color.YELLOW,
                "[C] อัปเกรดตัวละคร  |  แต้มที่มี: " + playerPoints + " pts", msgY + 70);

        int slotStartY = msgY + 100;
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 20f), Color.LIGHT_GRAY,
                "W / S เลือกช่อง | [ O ] เซฟ | [ L ] โหลด | [ X ] ลบเซฟ", slotStartY);

        for(int i = 1; i <= 3; i++) {
            File f = new File("savegame" + i + ".dat");
            String dateStr = "Empty";
            if (f.exists()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                dateStr = sdf.format(new Date(f.lastModified()));
            }

            Color bg = (currentSaveSlot == i) ? new Color(80, 80, 150) : new Color(40, 40, 40);
            g2.setColor(bg);
            int rw = 360;
            int rh = 34;
            int rx = screenWidth/2 - rw/2;
            int ry = slotStartY + 20 + (i-1)*(rh + 10);

            g2.fillRoundRect(rx, ry, rw, rh, 10, 10);

            if (currentSaveSlot == i) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(rx, ry, rw, rh, 10, 10);
                g2.setStroke(new BasicStroke(1));
            }

            g2.setColor(Color.WHITE);
            g2.setFont(kanitFont.deriveFont(Font.BOLD, 18f));
            g2.drawString("Slot " + i + " :  " + dateStr, rx + 20, ry + 24);
        }

        if (saveStatusTimer > 0) {
            Color statusColor = saveStatusText.equals("Saved") || saveStatusText.equals("Loaded") || saveStatusText.equals("Deleted") ? new Color(80, 255, 120) : new Color(255, 100, 100);
            g2.setColor(statusColor);
            g2.setFont(kanitFont.deriveFont(Font.BOLD, 22f));
            drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 22f), statusColor,
                    saveStatusText, slotStartY + 180);
        }
    }

    public void saveGame() {
        String filename = "savegame" + currentSaveSlot + ".dat";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            bw.write("playerPoints=" + playerPoints); bw.newLine();
            bw.write("playerAtkLv=" + playerAtkLv);   bw.newLine();
            bw.write("playerHpLv="  + playerHpLv);    bw.newLine();
            bw.write("highestUnlockedStage=" + highestUnlockedStage); bw.newLine();
            saveStatusText  = "Saved";
            saveStatusTimer = SAVE_STATUS_FRAMES;
        } catch (IOException e) {
            saveStatusText  = "Error";
            saveStatusTimer = SAVE_STATUS_FRAMES;
        }
    }

    public void loadGame() {
        File f = new File("savegame" + currentSaveSlot + ".dat");
        if (!f.exists()) {
            saveStatusText  = "Empty";
            saveStatusTimer = SAVE_STATUS_FRAMES;
            return;
        }

        // FIX: นำค่าไปใส่ตัวแปรชั่วคราวก่อน เพื่อป้องกันไฟล์โหลดไม่ครบแล้วค่าพังกลางคัน
        int tempPoints = playerPoints;
        int tempAtkLv = playerAtkLv;
        int tempHpLv = playerHpLv;
        int tempHighestStage = highestUnlockedStage;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length < 2) continue;
                switch (parts[0].trim()) {
                    case "playerPoints"          -> tempPoints          = Integer.parseInt(parts[1].trim());
                    case "playerAtkLv"           -> tempAtkLv           = Integer.parseInt(parts[1].trim());
                    case "playerHpLv"            -> tempHpLv            = Integer.parseInt(parts[1].trim());
                    case "highestUnlockedStage"  -> tempHighestStage    = Integer.parseInt(parts[1].trim());
                }
            }

            // โหลดครบไร้ Error ค่อยเขียนทับ
            playerPoints = tempPoints;
            playerAtkLv = tempAtkLv;
            playerHpLv = tempHpLv;
            highestUnlockedStage = tempHighestStage;

            saveStatusText  = "Loaded";
            saveStatusTimer = SAVE_STATUS_FRAMES;
        } catch (Exception e) {
            saveStatusText  = "Error";
            saveStatusTimer = SAVE_STATUS_FRAMES;
        }
    }

    public void deleteGame() {
        File f = new File("savegame" + currentSaveSlot + ".dat");
        if (f.exists()) {
            if (f.delete()) {
                saveStatusText = "Deleted";
            } else {
                saveStatusText = "Error";
            }
        } else {
            saveStatusText = "Empty";
        }
        saveStatusTimer = SAVE_STATUS_FRAMES;
    }

    private void drawCenteredInRect(Graphics2D g2, String text, int rx, int ry, int rw, int rh) {
        FontMetrics fm = g2.getFontMetrics();
        int tx = rx + (rw - fm.stringWidth(text)) / 2;
        int ty = ry + (rh + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);
    }

    private void drawUpgradeScreen(Graphics2D g2) {
        g2.setColor(new Color(20, 20, 30));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        drawCentered(g2, kanitFont.deriveFont(Font.BOLD, 48f), Color.WHITE, "อัปเกรดตัวละคร", 100);
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 32f), Color.YELLOW,
                "แต้มที่มี: " + playerPoints + " Points", 160);
        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 20f), new Color(160, 160, 255),
                "ต้องการ " + upgradeCost + " แต้มต่อ 1 เลเวล  |  สูงสุด " + maxStatLv + " เลเวล", 200);

        g2.setFont(kanitFont.deriveFont(Font.BOLD, 28f));
        drawUpgradeOption(g2, 0, "HP",  playerHpLv,  20 + (playerHpLv - 1) * 5,  "พลังชีวิตตอนเริ่มสู้", 260);
        drawUpgradeOption(g2, 1, "ATK", playerAtkLv, 20 + (playerAtkLv - 1) * 5, "พลังโจมตี", 360);

        drawCentered(g2, kanitFont.deriveFont(Font.PLAIN, 24f), new Color(180, 180, 255),
                "[ W / S เลื่อน ]     [ ENTER อัปเกรด ]     [ C กลับ ]",
                screenHeight - 80);
    }

    private void drawUpgradeOption(Graphics2D g2, int idx, String stat, int lv, int value, String valueLabel, int y) {
        boolean maxed  = (lv >= maxStatLv);
        String  maxTag = maxed ? " [MAX!]" : "  (ใช้ " + upgradeCost + " แต้ม)";
        String  text   = "อัปเกรด " + stat + " (Lv " + lv + "/" + maxStatLv + ")  |  "
                + valueLabel + ": " + value + maxTag;

        if (upgradeSelection == idx) {
            Color bg = (idx == 0) ? new Color(80, 80, 150) : new Color(150, 80, 80);
            g2.setColor(bg);
            g2.fillRoundRect(80, y, screenWidth - 160, 70, 20, 20);
            g2.setColor(maxed ? Color.GRAY : Color.WHITE);
        } else {
            g2.setColor(Color.GRAY);
        }
        g2.drawString(text, 120, y + 46);
    }

    private void drawBattle(Graphics2D g2) {
        BufferedImage currentBg = switch (currentStage) {
            case 1 -> bgStage1;
            case 2 -> bgStage2;
            case 3 -> bgStage3;
            default -> null;
        };

        if (currentBg != null) {
            g2.drawImage(currentBg, 0, 0, screenWidth, screenHeight, null);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }

        switch (currentStage) {
            case 1 -> scytheBoss.draw(g2);
            case 2 -> skullBoss.draw(g2);
            case 3 -> kekeBoss.draw(g2);
        }

        g2.setColor(Color.BLACK);
        g2.fillRect(boxX, boxY, boxWidth, boxHeight);

        g2.setColor(switch (currentStage) {
            case 2  -> new Color(200, 100, 255);
            case 3  -> new Color(255, 120, 30);
            default -> Color.WHITE;
        });
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
        String bossName = switch (currentStage) {
            case 2  -> "Skull Boss";
            case 3  -> "Slime Infected Covid Boss";
            default -> "Scythe Boss";
        };

        if (battlePhase == playerTurnPhase) {
            g2.setFont(kanitFont.deriveFont(Font.PLAIN, 20f));
            g2.setColor(Color.WHITE);
            g2.drawString("* " + bossName + " จ้องมองคุณอยู่...", boxX + 40, boxY + 55);

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
            int done  = Math.min(patternIndex, patternQueue.size());
            int total = patternQueue.size();

            if (initialCooldownActive) {
                int secs = (INITIAL_COOLDOWN - initialCooldownTimer) / 60 + 1;
                g2.drawString("! " + bossName + " กำลังเตรียมโจมตี... (" + secs + " วินาที)",
                        boxX + 20, boxY + 55);
            } else {
                String coolStr = inCooldown ? "  (เตรียมรับมือ...)" : "";
                g2.drawString("! " + bossName + " กำลังโจมตี — หลบหลีก!  ["
                        + done + " / " + total + "]" + coolStr, boxX + 20, boxY + 55);
            }
        }
    }

    private void drawHpBars(Graphics2D g2) {
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

        int by       = boxY - 30;
        int bossMax  = getCurrentBossMaxHp();
        int bossHp   = getCurrentBossHp();
        int barWidth = Math.min(bossMax * 3, 500);
        int barFill  = (bossMax > 0) ? (int) ((float) bossHp / bossMax * barWidth) : 0;

        g2.setColor(Color.WHITE);
        g2.drawString("BOSS HP", boxX, by);
        g2.setColor(Color.RED);
        g2.fillRect(boxX + 180, by - 20, barWidth, 30);
        g2.setColor(Color.YELLOW);
        g2.fillRect(boxX + 180, by - 20, Math.max(0, barFill), 30);
        g2.setColor(Color.WHITE);
        g2.drawString(bossHp + " / " + bossMax, boxX + 180 + barWidth + 20, by);

        g2.setFont(kanitFont.deriveFont(Font.BOLD, 22f));
        Color stageColor = switch (currentStage) {
            case 2  -> new Color(200, 100, 255);
            case 3  -> new Color(255, 120, 30);
            default -> new Color(100, 200, 255);
        };

        g2.setColor(Color.BLACK);
        g2.drawString("[ ด่านที่ " + currentStage + " ]", 22, 32);

        g2.setColor(stageColor);
        g2.drawString("[ ด่านที่ " + currentStage + " ]", 20, 30);
    }

    private void drawCentered(Graphics2D g2, Font font, Color color, String text, int y) {
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, (screenWidth - g2.getFontMetrics().stringWidth(text)) / 2, y);
    }
}