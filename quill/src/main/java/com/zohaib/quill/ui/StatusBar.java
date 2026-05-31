package com.zohaib.quill.ui;

import com.zohaib.quill.editor.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * StatusBar — The bottom bar of the Quill editor window. Displays real-time
 * information including word count, character count, caret position (line and
 * column), and the current theme.
 *
 * <p>Implements {@link Document.DocumentChangeListener} so it automatically
 * refreshes word/character counts whenever the document content changes.
 *
 * @author Zohaib
 * @version 1.0
 */
public class StatusBar extends JPanel implements Document.DocumentChangeListener {

    // -----------------------------------------------------------------------
    // UI components
    // -----------------------------------------------------------------------

    private final JLabel wordCountLabel;
    private final JLabel charCountLabel;
    private final JLabel positionLabel;
    private final JLabel themeLabel;
    private final JLabel modifiedLabel;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private ThemeManager themeManager;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs a StatusBar with default zero-state values.
     *
     * @param themeManager the application theme manager for colouring
     */
    public StatusBar(ThemeManager themeManager) {
        this.themeManager = themeManager;

        setLayout(new BorderLayout());
        setBackground(themeManager.getStatusBarBackground());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
            themeManager.getBorderColor()));
        setPreferredSize(new Dimension(0, 26));

        // ── Left group: modified indicator ───────────────────────────────────
        modifiedLabel = makeLabel("", true);
        modifiedLabel.setForeground(themeManager.getSecondaryAccent());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        leftPanel.add(modifiedLabel);

        // ── Right group: stats ───────────────────────────────────────────────
        wordCountLabel = makeLabel("Words: 0",  false);
        charCountLabel = makeLabel("Chars: 0",  false);
        positionLabel  = makeLabel("Ln 1, Col 1", false);
        themeLabel     = makeLabel("● " + capitalize(themeManager.getThemeName()), false);
        themeLabel.setForeground(themeManager.getPrimaryAccent());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(0, 0, 0, 10));
        rightPanel.add(themeLabel);
        addSeparator(rightPanel);
        rightPanel.add(positionLabel);
        addSeparator(rightPanel);
        rightPanel.add(wordCountLabel);
        addSeparator(rightPanel);
        rightPanel.add(charCountLabel);

        add(leftPanel,  BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    // -----------------------------------------------------------------------
    // DocumentChangeListener
    // -----------------------------------------------------------------------

    /**
     * Called by the {@link Document} whenever its content or dirty state changes.
     * Updates word count, character count, and the unsaved-changes indicator.
     *
     * @param document the changed document
     */
    @Override
    public void onDocumentChanged(Document document) {
        SwingUtilities.invokeLater(() -> {
            wordCountLabel.setText("Words: " + document.getWordCount());
            charCountLabel.setText("Chars: " + document.getCharCount());
            modifiedLabel.setText(document.isDirty() ? "● Unsaved changes" : "");
        });
    }

    // -----------------------------------------------------------------------
    // Public update API
    // -----------------------------------------------------------------------

    /**
     * Updates the caret position display.
     *
     * @param line   1-based line number
     * @param column 1-based column number
     */
    public void updatePosition(int line, int column) {
        positionLabel.setText("Ln " + line + ", Col " + column);
    }

    /**
     * Updates the word count display.
     *
     * @param count word count
     */
    public void updateWordCount(int count) {
        wordCountLabel.setText("Words: " + count);
    }

    /**
     * Updates the character count display.
     *
     * @param count character count
     */
    public void updateCharCount(int count) {
        charCountLabel.setText("Chars: " + count);
    }

    /**
     * Updates the theme indicator label.
     *
     * @param themeName "dark" or "light"
     */
    public void updateTheme(String themeName) {
        themeLabel.setText("● " + capitalize(themeName));
        themeLabel.setForeground(themeManager.getPrimaryAccent());
    }

    /**
     * Re-applies theme colours to all labels and the bar itself.
     *
     * @param newTheme the updated theme manager
     */
    public void applyTheme(ThemeManager newTheme) {
        this.themeManager = newTheme;
        setBackground(newTheme.getStatusBarBackground());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, newTheme.getBorderColor()));

        for (Component c : getAllLabels()) {
            c.setForeground(newTheme.getSubtleText());
        }
        themeLabel.setForeground(newTheme.getPrimaryAccent());
        modifiedLabel.setForeground(newTheme.getSecondaryAccent());
        updateTheme(newTheme.getThemeName());
        revalidate();
        repaint();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a styled status bar label with consistent font and padding.
     *
     * @param text  initial label text
     * @param bold  whether to use bold weight
     * @return configured label
     */
    private JLabel makeLabel(String text, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 11));
        label.setForeground(themeManager.getSubtleText());
        label.setBorder(new EmptyBorder(0, 8, 0, 8));
        return label;
    }

    /** Adds a thin vertical separator between status bar items. */
    private void addSeparator(JPanel panel) {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 14));
        sep.setForeground(themeManager.getBorderColor());
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(sep);
        panel.add(wrapper);
    }

    /** Returns all label components for batch colour updates. */
    private java.util.List<Component> getAllLabels() {
        return java.util.List.of(wordCountLabel, charCountLabel, positionLabel);
    }

    /** Capitalises the first character of a string. */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
