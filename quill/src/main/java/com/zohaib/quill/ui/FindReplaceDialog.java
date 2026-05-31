package com.zohaib.quill.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * FindReplaceDialog — A non-blocking (modeless) dialog providing find and
 * find-and-replace functionality within the editor's text area.
 *
 * <p>Features:
 * <ul>
 *   <li>Find next / find previous</li>
 *   <li>Replace current occurrence</li>
 *   <li>Replace all occurrences</li>
 *   <li>Case-sensitive toggle</li>
 *   <li>Whole-word match toggle</li>
 *   <li>Visual match count feedback</li>
 * </ul>
 *
 * @author Zohaib
 * @version 1.0
 */
public class FindReplaceDialog extends JDialog {

    // -----------------------------------------------------------------------
    // UI components
    // -----------------------------------------------------------------------

    private final JTextField findField;
    private final JTextField replaceField;
    private final JCheckBox  caseCheckBox;
    private final JCheckBox  wholeWordCheckBox;
    private final JLabel     statusLabel;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** The text area to search within. */
    private final javax.swing.text.JTextComponent target;

    /** Last successfully found index — used by "Find Next". */
    private int lastFoundIndex = -1;

    /** Theme manager for consistent colouring. */
    private ThemeManager themeManager;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs a modeless FindReplaceDialog.
     *
     * @param owner        the parent JFrame for dialog centering
     * @param target       the text component to search within
     * @param themeManager the application theme manager
     */
    public FindReplaceDialog(JFrame owner,
                             javax.swing.text.JTextComponent target,
                             ThemeManager themeManager) {
        super(owner, "Find & Replace", false);
        this.target       = target;
        this.themeManager = themeManager;

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(themeManager.getSurface());
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Form grid ────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(4, 4, 4, 4);
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.anchor  = GridBagConstraints.WEST;

        // Find row
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        form.add(styledLabel("Find:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        findField = styledTextField();
        form.add(findField, gc);

        // Replace row
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        form.add(styledLabel("Replace:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        replaceField = styledTextField();
        form.add(replaceField, gc);

        // Options row
        gc.gridx = 1; gc.gridy = 2;
        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        opts.setOpaque(false);
        caseCheckBox      = styledCheckBox("Case sensitive");
        wholeWordCheckBox = styledCheckBox("Whole word");
        opts.add(caseCheckBox);
        opts.add(Box.createHorizontalStrut(12));
        opts.add(wholeWordCheckBox);
        form.add(opts, gc);

        // Status row
        gc.gridx = 1; gc.gridy = 3;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(themeManager.getSubtleText());
        form.add(statusLabel, gc);

        // ── Button panel ─────────────────────────────────────────────────────
        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(12, 0, 0, 0));

        JButton findPrevBtn    = actionButton("◀ Find Prev",    e -> findPrevious());
        JButton findNextBtn    = actionButton("Find Next ▶",    e -> findNext());
        JButton replaceBtn     = actionButton("Replace",        e -> replaceCurrent());
        JButton replaceAllBtn  = actionButton("Replace All",    e -> replaceAll());

        buttons.add(findPrevBtn);
        buttons.add(findNextBtn);
        buttons.add(replaceBtn);
        buttons.add(replaceAllBtn);

        // ── Close button ─────────────────────────────────────────────────────
        JButton closeBtn = actionButton("Close", e -> setVisible(false));
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        southPanel.setOpaque(false);
        southPanel.add(closeBtn);

        root.add(form,       BorderLayout.CENTER);
        root.add(buttons,    BorderLayout.NORTH);  // moved below, actually…
        // Re-layout: form top, buttons middle, close bottom
        root.removeAll();
        root.setLayout(new BorderLayout(0, 8));
        root.add(form,      BorderLayout.CENTER);
        root.add(buttons,   BorderLayout.SOUTH);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(themeManager.getSurface());
        outer.setBorder(new EmptyBorder(16, 16, 16, 16));
        outer.add(root,     BorderLayout.CENTER);
        outer.add(southPanel, BorderLayout.SOUTH);

        setContentPane(outer);

        // Enter in find field triggers find next
        findField.addActionListener(e -> findNext());

        // Escape closes dialog
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(
            e -> setVisible(false), esc, JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setMinimumSize(new Dimension(420, getPreferredSize().height));
        setLocationRelativeTo(owner);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Makes the dialog visible and pre-populates the find field with the
     * currently selected text in the editor (if any).
     */
    public void showDialog() {
        String selected = target.getSelectedText();
        if (selected != null && !selected.isBlank() && !selected.contains("\n")) {
            findField.setText(selected);
        }
        findField.selectAll();
        setVisible(true);
        findField.requestFocusInWindow();
        lastFoundIndex = -1;
    }

    // -----------------------------------------------------------------------
    // Search / replace logic
    // -----------------------------------------------------------------------

    /** Finds the next occurrence of the search term in the target text. */
    private void findNext() {
        String text   = target.getText();
        String query  = findField.getText();
        if (query.isEmpty()) { setStatus("Please enter a search term."); return; }

        int start = lastFoundIndex >= 0 ? lastFoundIndex + 1 : 0;
        int index = findIn(text, query, start);

        if (index == -1 && lastFoundIndex > 0) {
            // Wrap around
            index = findIn(text, query, 0);
            if (index != -1) setStatus("Wrapped to beginning.");
        }

        if (index != -1) {
            selectRange(index, index + query.length());
            lastFoundIndex = index;
            if (!"Wrapped to beginning.".equals(statusLabel.getText())) {
                setStatus(" ");
            }
        } else {
            lastFoundIndex = -1;
            setStatus("\"" + query + "\" not found.");
        }
    }

    /** Finds the previous occurrence of the search term. */
    private void findPrevious() {
        String text   = target.getText();
        String query  = findField.getText();
        if (query.isEmpty()) { setStatus("Please enter a search term."); return; }

        int start = lastFoundIndex > 0 ? lastFoundIndex - 1 : text.length();
        int index = findInReverse(text, query, start);

        if (index == -1) {
            index = findInReverse(text, query, text.length());
            if (index != -1) setStatus("Wrapped to end.");
        }

        if (index != -1) {
            selectRange(index, index + query.length());
            lastFoundIndex = index;
        } else {
            lastFoundIndex = -1;
            setStatus("\"" + query + "\" not found.");
        }
    }

    /** Replaces the currently highlighted (matched) occurrence with the replacement text. */
    private void replaceCurrent() {
        String selected = target.getSelectedText();
        String query    = findField.getText();
        String replace  = replaceField.getText();

        if (query.isEmpty()) { setStatus("Please enter a search term."); return; }
        if (selected == null) { findNext(); return; }

        boolean matches = caseCheckBox.isSelected()
            ? selected.equals(query)
            : selected.equalsIgnoreCase(query);

        if (matches) {
            target.replaceSelection(replace);
            lastFoundIndex = target.getCaretPosition() - replace.length() - 1;
            setStatus("Replaced one occurrence.");
            findNext();
        } else {
            findNext();
        }
    }

    /** Replaces all occurrences of the search term in the document. */
    private void replaceAll() {
        String query   = findField.getText();
        String replace = replaceField.getText();
        if (query.isEmpty()) { setStatus("Please enter a search term."); return; }

        String text = target.getText();
        String flags = caseCheckBox.isSelected() ? "" : "(?i)";
        String pattern = flags + (wholeWordCheckBox.isSelected()
            ? "\\b" + java.util.regex.Pattern.quote(query) + "\\b"
            : java.util.regex.Pattern.quote(query));

        String newText = text.replaceAll(pattern,
            java.util.regex.Matcher.quoteReplacement(replace));
        int count = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(text);
        while (m.find()) count++;

        if (count > 0) {
            target.setText(newText);
            lastFoundIndex = -1;
            setStatus("Replaced " + count + " occurrence" + (count > 1 ? "s" : "") + ".");
        } else {
            setStatus("\"" + query + "\" not found.");
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Finds the first occurrence of {@code query} in {@code text} starting at
     * {@code from}, respecting case and whole-word settings.
     */
    private int findIn(String text, String query, int from) {
        if (from >= text.length()) return -1;
        String t = caseCheckBox.isSelected() ? text  : text.toLowerCase();
        String q = caseCheckBox.isSelected() ? query : query.toLowerCase();

        int idx = from;
        while (true) {
            int i = t.indexOf(q, idx);
            if (i == -1) return -1;
            if (!wholeWordCheckBox.isSelected() || isWholeWord(text, i, q.length())) {
                return i;
            }
            idx = i + 1;
        }
    }

    /** Finds the last occurrence of {@code query} at or before {@code from}. */
    private int findInReverse(String text, String query, int from) {
        String t = caseCheckBox.isSelected() ? text  : text.toLowerCase();
        String q = caseCheckBox.isSelected() ? query : query.toLowerCase();

        int search = Math.min(from - q.length(), t.length() - q.length());
        for (int i = search; i >= 0; i--) {
            if (t.startsWith(q, i)) {
                if (!wholeWordCheckBox.isSelected() || isWholeWord(text, i, q.length())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Returns true if the substring at [start, start+len) is a whole word. */
    private boolean isWholeWord(String text, int start, int len) {
        boolean beforeOk = start == 0 || !Character.isLetterOrDigit(text.charAt(start - 1));
        int end = start + len;
        boolean afterOk  = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
        return beforeOk && afterOk;
    }

    /** Selects the given character range in the target text component. */
    private void selectRange(int start, int end) {
        target.requestFocusInWindow();
        target.setCaretPosition(start);
        target.moveCaretPosition(end);
        try {
            Rectangle rect = target.modelToView2D(start).getBounds();
            target.scrollRectToVisible(rect);
        } catch (Exception ignored) { }
    }

    /** Updates the status label text. */
    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    // ── Factory helpers ──────────────────────────────────────────────────────

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(themeManager.getForeground());
        return l;
    }

    private JTextField styledTextField() {
        JTextField f = new JTextField(24);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        f.setBackground(themeManager.getOverlay());
        f.setForeground(themeManager.getForeground());
        f.setCaretColor(themeManager.getCaretColor());
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        return f;
    }

    private JCheckBox styledCheckBox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setForeground(themeManager.getForeground());
        cb.setOpaque(false);
        return cb;
    }

    private JButton actionButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(themeManager.getButtonBackground());
        btn.setForeground(themeManager.getForeground());
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1, true),
            new EmptyBorder(5, 12, 5, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(themeManager.getButtonHoverBackground());
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(themeManager.getButtonBackground());
            }
        });
        return btn;
    }
}
