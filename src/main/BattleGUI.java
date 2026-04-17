package main;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * BattleGUI — Undertale-style quiz panel shown during the player's turn.
 */
public class BattleGUI {

    // ── Data ──────────────────────────────────────────────────────────
    public static class Question {
        public final String   text;
        public final String[] choices;
        public final int      correct;
        public final String   explanation;

        public Question(String text, String[] choices, int correct, String explanation) {
            this.text        = text;
            this.choices     = choices;
            this.correct     = correct;
            this.explanation = explanation;
        }
    }

    // ── Theme colors ──────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(10,  10,  20);
    private static final Color BG_MID       = new Color(20,  20,  45);
    private static final Color BORDER_COLOR = new Color(180, 160, 255);
    private static final Color HEART_RED    = new Color(220,  50,  50);
    private static final Color TEXT_WHITE   = new Color(240, 240, 240);
    private static final Color TEXT_GRAY    = new Color(160, 160, 180);
    private static final Color CHOICE_HL    = new Color(255, 230, 100);
    private static final Color CHOICE_NORM  = new Color(200, 200, 220);
    private static final Color CHOICE_BG_HL = new Color(80,  60,  120, 180);
    private static final Color SEPARATOR    = new Color(100,  80, 160);

    // ── Fonts ─────────────────────────────────────────────────────────
    private static Font FONT_TITLE, FONT_BODY, FONT_CHOICE, FONT_LABEL, FONT_RESULT;

    static {
        try {
            InputStream is = BattleGUI.class.getResourceAsStream("/res/Kanit-Bold.ttf");
            if (is == null) {
                is = BattleGUI.class.getResourceAsStream("/main/res/Kanit-Bold.ttf");
            }

            if (is != null) {
                Font base   = Font.createFont(Font.TRUETYPE_FONT, is);
                FONT_TITLE  = base.deriveFont(Font.PLAIN, 22f);
                FONT_BODY   = base.deriveFont(Font.PLAIN, 18f);
                FONT_CHOICE = base.deriveFont(Font.PLAIN, 18f);
                FONT_LABEL  = base.deriveFont(Font.PLAIN, 15f);
                FONT_RESULT = base.deriveFont(Font.PLAIN, 20f);
            } else {
                setFallbackFonts(new Font("Tahoma", Font.BOLD, 20));
            }
        } catch (Exception e) {
            setFallbackFonts(new Font("Tahoma", Font.BOLD, 20));
        }
    }

    private static void setFallbackFonts(Font f) {
        FONT_TITLE  = f.deriveFont(Font.BOLD,  22f);
        FONT_BODY   = f.deriveFont(Font.PLAIN, 18f);
        FONT_CHOICE = f.deriveFont(Font.BOLD,  18f);
        FONT_LABEL  = f.deriveFont(Font.BOLD,  14f);
        FONT_RESULT = f.deriveFont(Font.BOLD,  20f);
    }

    // ── State ─────────────────────────────────────────────────────────
    private final List<Question>   questions = new ArrayList<>();
    private final java.util.Random rng       = new java.util.Random();

    private Question current;
    private int      selectedIdx = 0;
    private boolean  visible     = false;
    private boolean  answered    = false;
    private boolean  correct     = false;
    private boolean  showResult  = false;

    // Animation
    private int   fadeTimer   = 0;
    private float fadeAlpha   = 0f;
    private int   resultTimer = 0;

    private static final int FADE_IN_FRAMES = 15;
    private static final int RESULT_FRAMES  = 60; // หน่วงเวลาเฉพาะตอนตอบถูก

    // Layout
    private final int screenW, screenH;
    private final int panelX, panelY;
    private static final int PANEL_W = 720, PANEL_H = 430;

    public BattleGUI(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        panelX = (screenW - PANEL_W) / 2;
        panelY = (screenH - PANEL_H) / 2 + 20;
    }

    // ── Public API ────────────────────────────────────────────────────
    public void loadQuestions(String resourcePath) {
        questions.clear();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream(resourcePath), "UTF-8"))) {

            String   qText   = null;
            String[] choices = new String[3];
            int      ci      = 0, ansIdx = 0;
            String   expText = "";

            for (String line; (line = br.readLine()) != null; ) {
                line = line.trim();

                if (line.equals("_") || line.isEmpty()) {
                    if (qText != null && ci == 3) {
                        questions.add(new Question(qText, choices.clone(), ansIdx, expText));
                    }
                    qText = null;
                    choices = new String[3];
                    ci = 0;
                    ansIdx = 0;
                    expText = "";
                    continue;
                }

                if (line.startsWith("Q:")) {
                    qText = line.substring(2).trim();
                } else if (line.startsWith("A:") && ci == 0) {
                    choices[ci++] = line.substring(2).trim();
                } else if (line.startsWith("B:") && ci == 1) {
                    choices[ci++] = line.substring(2).trim();
                } else if (line.startsWith("C:") && ci == 2) {
                    choices[ci++] = line.substring(2).trim();
                } else if (line.startsWith("ANS:")) {
                    String ans = line.substring(4).trim().toUpperCase();
                    ansIdx = ans.equals("A") ? 0 : ans.equals("B") ? 1 : 2;
                } else if (line.startsWith("EXP:")) {
                    expText = line.substring(4).trim();
                }
            }
            if (qText != null && ci == 3) {
                questions.add(new Question(qText, choices.clone(), ansIdx, expText));
            }

            System.out.println("[BattleGUI] Loaded " + questions.size() + " questions.");
        } catch (Exception e) {
            System.err.println("[BattleGUI] Failed to load questions: " + e.getMessage());
        }
    }

    public void nextQuestion() {
        if (questions.isEmpty()) {
            answered = correct = true;
            return;
        }
        current     = questions.get(rng.nextInt(questions.size()));
        selectedIdx = 0;

        visible    = true;
        answered   = false;
        correct    = false;
        showResult = false;

        fadeTimer   = 0;
        resultTimer = 0;
        fadeAlpha   = 0f;
    }

    public void handleUp() {
        if (visible && !answered) {
            selectedIdx = (selectedIdx + 2) % 3;
        }
    }

    public void handleDown() {
        if (visible && !answered) {
            selectedIdx = (selectedIdx + 1) % 3;
        }
    }

    public void confirmSelection() {
        if (!visible || current == null) return;

        if (showResult) {
            visible = false;
        } else if (!answered) {
            answered    = true;
            correct     = (selectedIdx == current.correct);
            showResult  = true;
            resultTimer = 0;
        }
    }

    public boolean isVisible()    { return visible; }
    public boolean isAnswered()   { return answered; }
    public boolean getResult()    { return correct; }
    public boolean hasQuestions() { return !questions.isEmpty(); }

    // ── Game loop ─────────────────────────────────────────────────────
    public void update() {
        if (!visible) return;

        if (fadeAlpha < 1f) {
            fadeAlpha = Math.min(1f, (float) ++fadeTimer / FADE_IN_FRAMES);
        }

        if (showResult && correct) {
            if (++resultTimer >= RESULT_FRAMES) {
                visible = false;
            }
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────
    public void draw(Graphics2D g2) {
        if (!visible || current == null) return;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dim overlay
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha * 0.55f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenW, screenH);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));

        // Panel background
        g2.setPaint(new GradientPaint(panelX, panelY, BG_DARK, panelX, panelY + PANEL_H, BG_MID));
        g2.fillRoundRect(panelX, panelY, PANEL_W, PANEL_H, 20, 20);

        // Neon border
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(BORDER_COLOR);
        g2.drawRoundRect(panelX, panelY, PANEL_W, PANEL_H, 20, 20);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(100, 80, 160, 80));
        g2.drawRoundRect(panelX + 6, panelY + 6, PANEL_W - 12, PANEL_H - 12, 16, 16);

        // Title label
        drawCentered(g2, FONT_LABEL, new Color(160, 130, 255), "BATTLE QUIZ", panelY + 26);
        g2.setColor(SEPARATOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(panelX + 20, panelY + 35, panelX + PANEL_W - 20, panelY + 35);

        // Question text
        g2.setFont(FONT_BODY);
        g2.setColor(TEXT_WHITE);
        drawWrappedText(g2, current.text, panelX + 30, panelY + 70, PANEL_W - 60, 28);

        // Separator above choices
        int sepY = panelY + 130;
        g2.setColor(SEPARATOR);
        g2.drawLine(panelX + 20, sepY, panelX + PANEL_W - 20, sepY);

        // Answer choices
        String[] labels = {"A", "B", "C"};
        for (int i = 0; i < 3; i++) {
            int cy = sepY + 30 + i * 56;
            drawChoice(g2, i, cy, labels[i]);
        }

        // Result or hint text at bottom
        if (showResult) {
            String txt = correct ? "CORRECT!" : "WRONG!";
            Color  col = correct ? new Color(100, 255, 100) : new Color(255, 80, 80);

            if (!correct && current.explanation != null && !current.explanation.isEmpty()) {
                drawCentered(g2, FONT_RESULT, col, txt, panelY + 310);

                g2.setFont(FONT_LABEL);
                g2.setColor(new Color(255, 210, 100));
                drawWrappedText(g2, "ความรู้เพิ่มเติม: " + current.explanation, panelX + 30, panelY + 340, PANEL_W - 60, 22);

                drawCentered(g2, FONT_LABEL, new Color(120, 100, 180), "[ ENTER เพื่อรับการโจมตี ]", panelY + PANEL_H - 18);
            } else {
                drawCentered(g2, FONT_RESULT, col, txt, panelY + 330);
                if (!correct) {
                    drawCentered(g2, FONT_LABEL, new Color(120, 100, 180), "[ ENTER เพื่อรับการโจมตี ]", panelY + PANEL_H - 18);
                }
            }
        } else {
            drawCentered(g2, FONT_LABEL, new Color(120, 100, 180),
                    "[ W / S เลื่อน ]   [ ENTER ยืนยัน ]", panelY + PANEL_H - 18);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawChoice(Graphics2D g2, int i, int cy, String label) {
        boolean selected = (i == selectedIdx);

        if (showResult) {
            if (i == current.correct) {
                fillChoice(g2, cy, new Color(40, 120, 40, 160), new Color(100, 255, 100));
            } else if (i == selectedIdx && !correct) {
                fillChoice(g2, cy, new Color(120, 30, 30, 160), new Color(255, 80, 80));
            }
        } else if (selected) {
            fillChoice(g2, cy, CHOICE_BG_HL, CHOICE_HL);
        }

        if (!showResult && selected) {
            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            g2.setColor(HEART_RED);
            g2.drawString("♥", panelX + 28, cy + 6);
        }

        g2.setFont(FONT_CHOICE);
        g2.setColor((selected && !showResult) ? CHOICE_HL : TEXT_GRAY);
        g2.drawString(label + ".", panelX + 52, cy + 6);

        g2.setColor((selected && !showResult) ? TEXT_WHITE : CHOICE_NORM);
        g2.drawString(current.choices[i], panelX + 80, cy + 6);
    }

    private void fillChoice(Graphics2D g2, int cy, Color fill, Color border) {
        g2.setColor(fill);
        g2.fillRoundRect(panelX + 18, cy - 24, PANEL_W - 36, 44, 10, 10);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panelX + 18, cy - 24, PANEL_W - 36, 44, 10, 10);
    }

    private void drawCentered(Graphics2D g2, Font font, Color color, String text, int y) {
        g2.setFont(font);
        g2.setColor(color);
        int w = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, panelX + (PANEL_W - w) / 2, y);
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm     = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int curY           = y;

        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
                g2.drawString(line.toString(), x, curY);
                curY += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) {
            g2.drawString(line.toString(), x, curY);
        }
    }
}