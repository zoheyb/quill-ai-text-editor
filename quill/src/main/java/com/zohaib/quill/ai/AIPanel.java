package com.zohaib.quill.ai;

import com.zohaib.quill.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.*;

/**
 * AIPanel — A collapsible right-side panel that displays AI-generated responses.
 * It shows a header with the operation name, the response text in a scrollable
 * read-only area, and a "Copy" button. A loading spinner is displayed while an
 * AI request is in progress.
 *
 * <p>This panel is embedded in the main {@code JSplitPane} of {@code EditorWindow}.
 * Toggling visibility is handled by the parent window via {@link #setVisible(boolean)}.
 *
 * @author Zohaib
 * @version 1.0
 */
public class AIPanel extends JPanel {

    // -----------------------------------------------------------------------
    // UI components
    // -----------------------------------------------------------------------

    /** Header label showing the current AI operation name. */
    private final JLabel   headerLabel;

    /** Scrollable text area showing the AI response. */
    private final JTextArea responseArea;

    /** Scroll pane wrapping the response area. */
    private final JScrollPane scrollPane;

    /** Panel containing the spinner and "Processing…" label. */
    private final JPanel  loadingPanel;

    /** Animated loading spinner component. */
    private final LoadingSpinner spinner;

    /** Button to copy the response to clipboard. */
    private final JButton copyButton;

    /** Panel holding the response area and copy button. */
    private final JPanel  contentPanel;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Reference to ThemeManager for colour updates. */
    private ThemeManager themeManager;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs an AIPanel with default empty state.
     *
     * @param themeManager the application theme manager
     */
    public AIPanel(ThemeManager themeManager) {
        this.themeManager = themeManager;

        setLayout(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(340, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0,
            themeManager.getBorderColor()));

        // ── Header ──────────────────────────────────────────────────────────
        headerLabel = new JLabel("AI Assistant", SwingConstants.LEFT);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerLabel.setBorder(new EmptyBorder(10, 14, 10, 14));
        headerLabel.setOpaque(true);
        headerLabel.setBackground(themeManager.getPanelHeaderBackground());
        headerLabel.setForeground(themeManager.getPrimaryAccent());

        // AI icon decoration
        JLabel iconLabel = new JLabel("✦");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        iconLabel.setForeground(themeManager.getPrimaryAccent());
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 6));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getPanelHeaderBackground());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
            themeManager.getBorderColor()));
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        headerLeft.setBackground(themeManager.getPanelHeaderBackground());
        headerLeft.setBorder(new EmptyBorder(8, 10, 8, 0));
        headerLeft.add(iconLabel);
        headerLeft.add(headerLabel);
        headerPanel.add(headerLeft, BorderLayout.CENTER);

        // ── Loading panel ────────────────────────────────────────────────────
        spinner      = new LoadingSpinner(themeManager.getPrimaryAccent());
        JLabel loadLabel = new JLabel("Processing with AI…");
        loadLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        loadLabel.setForeground(themeManager.getSubtleText());

        loadingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        loadingPanel.setOpaque(false);
        loadingPanel.add(spinner);
        loadingPanel.add(loadLabel);

        JPanel loadingWrapper = new JPanel(new GridBagLayout());
        loadingWrapper.setBackground(themeManager.getBackground());
        loadingWrapper.add(loadingPanel);

        // ── Response text area ───────────────────────────────────────────────
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responseArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        responseArea.setBackground(themeManager.getBackground());
        responseArea.setForeground(themeManager.getForeground());
        responseArea.setCaretColor(themeManager.getForeground());
        responseArea.setBorder(new EmptyBorder(12, 14, 12, 14));
        responseArea.setText("Select text in the editor and use the AI menu or toolbar to get started.");

        scrollPane = new JScrollPane(responseArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // ── Copy button ──────────────────────────────────────────────────────
        copyButton = new JButton("⎘  Copy Response");
        copyButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copyButton.setFocusPainted(false);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.setBackground(themeManager.getButtonBackground());
        copyButton.setForeground(themeManager.getForeground());
        copyButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        copyButton.addActionListener(e -> copyToClipboard());

        // Hover effect
        copyButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                copyButton.setBackground(themeManager.getButtonHoverBackground());
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                copyButton.setBackground(themeManager.getButtonBackground());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttonPanel.setBackground(themeManager.getBackground());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
            themeManager.getBorderColor()));
        buttonPanel.add(copyButton);

        // ── Content panel (response + button) ───────────────────────────────
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(themeManager.getBackground());
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        // ── Card panel (switches between loading and content) ─────────────
        JPanel cardWrapper = new JPanel(new CardLayout());
        cardWrapper.add(contentPanel, "content");
        cardWrapper.add(loadingWrapper, "loading");

        add(headerPanel,  BorderLayout.NORTH);
        add(cardWrapper,  BorderLayout.CENTER);

        // Start with content visible (not loading)
        showContent();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Switches the panel to its loading state, showing the animated spinner.
     * Also updates the header label to reflect the current AI operation.
     *
     * @param operationName name of the AI operation (e.g. "Fix &amp; Rephrase")
     */
    public void showLoading(String operationName) {
        headerLabel.setText(operationName);
        spinner.start();
        Container cardWrapper = (Container) getComponent(1); // cardWrapper
        CardLayout cl = (CardLayout) cardWrapper.getLayout();
        cl.show(cardWrapper, "loading");
        revalidate();
        repaint();
    }

    /**
     * Switches the panel back to its content state, stopping the spinner.
     */
    public void showContent() {
        spinner.stop();
        Component cardWrapper = getComponent(1);
        if (cardWrapper instanceof Container c) {
            CardLayout cl = (CardLayout) c.getLayout();
            cl.show(c, "content");
        }
        revalidate();
        repaint();
    }

    /**
     * Displays the given AI response text in the response area.
     * Automatically stops the spinner and shows the content panel.
     *
     * @param text the AI-generated text to display
     */
    public void setResponse(String text) {
        showContent();
        responseArea.setText(text);
        responseArea.setCaretPosition(0);
    }

    /**
     * Displays an error message in the response area with a warning prefix.
     *
     * @param errorMessage the error to display
     */
    public void setError(String errorMessage) {
        showContent();
        responseArea.setText("⚠ Error\n\n" + errorMessage);
        responseArea.setCaretPosition(0);
    }

    /**
     * Clears the response area to a default hint message.
     */
    public void clear() {
        responseArea.setText("Select text in the editor and use the AI menu or toolbar to get started.");
    }

    /**
     * Updates the AI panel colours when the theme changes.
     *
     * @param newThemeManager the updated theme manager
     */
    public void applyTheme(ThemeManager newThemeManager) {
        this.themeManager = newThemeManager;
        responseArea.setBackground(newThemeManager.getBackground());
        responseArea.setForeground(newThemeManager.getForeground());
        contentPanel.setBackground(newThemeManager.getBackground());
        scrollPane.setBackground(newThemeManager.getBackground());
        copyButton.setBackground(newThemeManager.getButtonBackground());
        copyButton.setForeground(newThemeManager.getForeground());
        spinner.setColor(newThemeManager.getPrimaryAccent());
        revalidate();
        repaint();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Copies the current response text to the system clipboard. */
    private void copyToClipboard() {
        String text = responseArea.getText();
        if (text != null && !text.isBlank()) {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);

            // Brief visual feedback
            String original = copyButton.getText();
            copyButton.setText("✓ Copied!");
            Timer t = new Timer(1500, e -> copyButton.setText(original));
            t.setRepeats(false);
            t.start();
        }
    }

    // -----------------------------------------------------------------------
    // Inner class: LoadingSpinner
    // -----------------------------------------------------------------------

    /**
     * A lightweight animated spinner component drawn entirely with Java2D.
     * Animates by rotating a series of arcs of varying opacity.
     */
    private static class LoadingSpinner extends JComponent {

        private static final int  SIZE   = 24;
        private static final int  SPOKES = 12;
        private static final long FPS    = 60;

        private Color   color;
        private int     angle  = 0;
        private Timer   timer;

        LoadingSpinner(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(SIZE, SIZE));
        }

        void start() {
            if (timer == null || !timer.isRunning()) {
                timer = new Timer((int)(1000 / FPS), e -> {
                    angle = (angle + 360 / SPOKES) % 360;
                    repaint();
                });
                timer.start();
            }
        }

        void stop() {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }

        void setColor(Color c) {
            this.color = c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int r  = Math.min(cx, cy) - 2;

            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < SPOKES; i++) {
                float alpha = (float)(i + 1) / SPOKES;
                g2.setColor(new Color(
                    color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(alpha * 255)));

                double theta = Math.toRadians((angle + i * (360 / SPOKES)) % 360);
                int x1 = (int)(cx + (r - 4) * Math.cos(theta));
                int y1 = (int)(cy + (r - 4) * Math.sin(theta));
                int x2 = (int)(cx + r * Math.cos(theta));
                int y2 = (int)(cy + r * Math.sin(theta));

                g2.drawLine(x1, y1, x2, y2);
            }
            g2.dispose();
        }
    }
}
