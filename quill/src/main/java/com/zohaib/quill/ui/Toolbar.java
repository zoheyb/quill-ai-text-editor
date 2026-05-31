package com.zohaib.quill.ui;

import com.zohaib.quill.editor.EditorPane;
import com.zohaib.quill.file.FileManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Toolbar — Builds and manages the icon/text toolbar at the top of the editor window.
 * Contains quick-access buttons for the most common file, edit, and AI operations.
 *
 * <p>All action handlers are passed in as {@link ActionListener} references from
 * {@code EditorWindow}, maintaining clean separation between UI definition and
 * business logic.
 *
 * @author Zohaib
 * @version 1.0
 */
public class Toolbar extends JToolBar {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private ThemeManager themeManager;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs the Toolbar and wires all action listeners.
     *
     * @param themeManager the application theme manager
     * @param onNew        action for New file
     * @param onOpen       action for Open file
     * @param onSave       action for Save
     * @param onUndo       action for Undo
     * @param onRedo       action for Redo
     * @param onFind       action for Find & Replace
     * @param onFix        action for AI Fix & Rephrase
     * @param onSummarize  action for AI Summarize
     * @param onExplain    action for AI Explain Code
     * @param onTheme      action for theme toggle
     */
    public Toolbar(ThemeManager themeManager,
                   ActionListener onNew,
                   ActionListener onOpen,
                   ActionListener onSave,
                   ActionListener onUndo,
                   ActionListener onRedo,
                   ActionListener onFind,
                   ActionListener onFix,
                   ActionListener onSummarize,
                   ActionListener onExplain,
                   ActionListener onTheme) {
        super(JToolBar.HORIZONTAL);
        this.themeManager = themeManager;

        setFloatable(false);
        setRollover(true);
        setBackground(themeManager.getToolbarBackground());
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
            themeManager.getBorderColor()));

        // ── File group ───────────────────────────────────────────────────────
        add(makeButton("New",  "📄", "New document  (Ctrl+N)", onNew));
        add(makeButton("Open", "📂", "Open file  (Ctrl+O)",    onOpen));
        add(makeButton("Save", "💾", "Save file  (Ctrl+S)",    onSave));

        addSeparator(new Dimension(1, 28));

        // ── Edit group ───────────────────────────────────────────────────────
        add(makeButton("Undo", "↩", "Undo  (Ctrl+Z)", onUndo));
        add(makeButton("Redo", "↪", "Redo  (Ctrl+Y)", onRedo));
        add(makeButton("Find", "🔍", "Find & Replace  (Ctrl+F)", onFind));

        addSeparator(new Dimension(1, 28));

        // ── AI group ─────────────────────────────────────────────────────────
        add(makeAIButton("✦ Fix",       "Fix & rephrase selected text",   onFix));
        add(makeAIButton("✦ Summarize", "Summarize selected text",         onSummarize));
        add(makeAIButton("✦ Explain",   "Explain selected code",           onExplain));

        addSeparator(new Dimension(1, 28));

        // ── Theme toggle ─────────────────────────────────────────────────────
        add(makeButton(themeManager.isDark() ? "☀ Light" : "🌙 Dark",
            themeManager.isDark() ? "☀" : "🌙",
            "Toggle theme  (Ctrl+T)", onTheme));
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Re-applies the theme to the toolbar background and all buttons.
     *
     * @param newTheme the updated theme manager
     */
    public void applyTheme(ThemeManager newTheme) {
        this.themeManager = newTheme;
        setBackground(newTheme.getToolbarBackground());
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
            newTheme.getBorderColor()));
        for (Component c : getComponents()) {
            if (c instanceof JButton btn) {
                styleButton(btn, newTheme,
                    btn.getName() != null && btn.getName().startsWith("ai"));
            }
        }
        revalidate();
        repaint();
    }

    // -----------------------------------------------------------------------
    // Private factory methods
    // -----------------------------------------------------------------------

    /**
     * Creates a styled toolbar button for file/edit actions.
     */
    private JButton makeButton(String text, String icon, String tooltip,
                               ActionListener action) {
        JButton btn = new JButton();
        btn.setText("<html><center>" + icon + "<br/><span style='font-size:8px'>"
            + text + "</span></center></html>");
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btn.setName("std:" + text);
        styleButton(btn, themeManager, false);
        btn.addActionListener(action);
        return btn;
    }

    /**
     * Creates a styled toolbar button specifically for AI actions (accent colour).
     */
    private JButton makeAIButton(String text, String tooltip, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setName("ai:" + text);
        styleButton(btn, themeManager, true);
        btn.addActionListener(action);
        return btn;
    }

    /** Applies consistent styling to a toolbar button. */
    private void styleButton(JButton btn, ThemeManager theme, boolean isAI) {
        btn.setBackground(theme.getToolbarBackground());
        btn.setForeground(isAI ? theme.getPrimaryAccent() : theme.getForeground());
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getToolbarBackground(), 1, true),
            new EmptyBorder(isAI ? 6 : 4, 10, isAI ? 6 : 4, 10)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(theme.getButtonHoverBackground());
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.getBorderColor(), 1, true),
                    new EmptyBorder(isAI ? 6 : 4, 10, isAI ? 6 : 4, 10)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(theme.getToolbarBackground());
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.getToolbarBackground(), 1, true),
                    new EmptyBorder(isAI ? 6 : 4, 10, isAI ? 6 : 4, 10)
                ));
            }
        });
    }
}
