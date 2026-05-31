package com.zohaib.quill.editor;

import com.zohaib.quill.ui.ThemeManager;
import com.zohaib.quill.undo.UndoManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;

/**
 * EditorPane — The core editing surface of Quill. Combines a {@link JTextArea}
 * with a custom line-number sidebar rendered by an inner {@link LineNumberPanel}.
 * The entire assembly is wrapped in a {@link JScrollPane} so callers simply add
 * {@link #getScrollPane()} to their layout.
 *
 * <p>Features:
 * <ul>
 *   <li>Line-number gutter that updates in real time</li>
 *   <li>Auto-indent on Enter key</li>
 *   <li>Smart Tab expansion to 2 or 4 spaces</li>
 *   <li>Theming support via {@link ThemeManager}</li>
 *   <li>Undo/Redo managed by the supplied {@link UndoManager}</li>
 * </ul>
 *
 * @author Zohaib
 * @version 1.0
 */
public class EditorPane extends JPanel {

    // -----------------------------------------------------------------------
    // Components
    // -----------------------------------------------------------------------

    /** The actual text-editing area. */
    private final JTextArea   textArea;

    /** Scroll pane wrapping textArea + line number panel. */
    private final JScrollPane scrollPane;

    /** The line number gutter component. */
    private final LineNumberPanel lineNumberPanel;

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private ThemeManager themeManager;
    private UndoManager  undoManager;

    // -----------------------------------------------------------------------
    // Settings
    // -----------------------------------------------------------------------

    private int tabSize;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs an EditorPane with the provided theme and undo manager.
     *
     * @param themeManager the application theme manager
     * @param undoManager  the undo manager to attach to the text document
     * @param fontSize     initial font size in points
     * @param tabSize      number of spaces per Tab key press (2 or 4)
     */
    public EditorPane(ThemeManager themeManager, UndoManager undoManager,
                      int fontSize, int tabSize) {
        super(new BorderLayout());
        this.themeManager = themeManager;
        this.undoManager  = undoManager;
        this.tabSize      = tabSize;

        // ── Text area ────────────────────────────────────────────────────────
        textArea = new JTextArea();
        textArea.setFont(themeManager.getEditorFont(fontSize));
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        textArea.setBackground(themeManager.getBackground());
        textArea.setForeground(themeManager.getForeground());
        textArea.setCaretColor(themeManager.getCaretColor());
        textArea.setSelectionColor(themeManager.getSelectionColor());
        textArea.setSelectedTextColor(themeManager.getForeground());
        textArea.setMargin(new Insets(4, 8, 4, 8));
        textArea.setTabSize(tabSize);

        // ── Scroll pane ──────────────────────────────────────────────────────
        scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(themeManager.getBackground());

        // ── Line numbers ─────────────────────────────────────────────────────
        lineNumberPanel = new LineNumberPanel();
        scrollPane.setRowHeaderView(lineNumberPanel);

        // ── Attach UndoManager ───────────────────────────────────────────────
        undoManager.attach(textArea);

        // ── Listeners ────────────────────────────────────────────────────────
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { lineNumberPanel.repaint(); }
            @Override public void removeUpdate(DocumentEvent e)  { lineNumberPanel.repaint(); }
            @Override public void changedUpdate(DocumentEvent e) { lineNumberPanel.repaint(); }
        });

        textArea.addCaretListener(e -> lineNumberPanel.repaint());

        // ── Key bindings ─────────────────────────────────────────────────────
        installKeyBindings();

        add(scrollPane, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // Key bindings
    // -----------------------------------------------------------------------

    /**
     * Installs custom key bindings for Tab expansion and auto-indent on Enter.
     */
    private void installKeyBindings() {
        // Tab → insert spaces
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB && !e.isShiftDown()) {
                    e.consume();
                    insertTab();
                } else if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
                    e.consume();
                    unindentSelection();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    autoIndent();
                }
            }
        });
    }

    /** Inserts spaces equal to the tab size at the caret position. */
    private void insertTab() {
        String spaces = " ".repeat(tabSize);
        textArea.insert(spaces, textArea.getCaretPosition());
    }

    /** Removes one level of leading indent from each selected line. */
    private void unindentSelection() {
        try {
            int start = textArea.getSelectionStart();
            int end   = textArea.getSelectionEnd();
            int lineStart = textArea.getLineStartOffset(
                textArea.getLineOfOffset(start));
            int lineEnd   = textArea.getLineEndOffset(
                textArea.getLineOfOffset(end));

            String[] lines = textArea.getText()
                .substring(lineStart, lineEnd)
                .split("\n", -1);

            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith(" ".repeat(tabSize))) {
                    sb.append(line.substring(tabSize));
                } else if (line.startsWith("\t")) {
                    sb.append(line.substring(1));
                } else {
                    sb.append(line);
                }
                sb.append('\n');
            }
            // Remove trailing newline if not originally there
            if (!textArea.getText().substring(lineStart, lineEnd).endsWith("\n")) {
                sb.deleteCharAt(sb.length() - 1);
            }
            textArea.replaceRange(sb.toString(), lineStart, lineEnd);
        } catch (BadLocationException ex) {
            // Silently ignore boundary errors
        }
    }

    /**
     * On Enter key: inserts a newline followed by the same leading whitespace
     * as the current line (auto-indent). Also detects trailing '{' for
     * brace-based languages and increases indent by one level.
     */
    private void autoIndent() {
        try {
            int caretPos  = textArea.getCaretPosition();
            int lineNum   = textArea.getLineOfOffset(caretPos);
            int lineStart = textArea.getLineStartOffset(lineNum);
            String lineText = textArea.getText(lineStart, caretPos - lineStart);

            // Extract leading whitespace
            StringBuilder indent = new StringBuilder();
            for (char c : lineText.toCharArray()) {
                if (c == ' ' || c == '\t') indent.append(c);
                else break;
            }

            // Extra indent level after opening brace/bracket
            String trimmed = lineText.stripTrailing();
            boolean addLevel = !trimmed.isEmpty() &&
                (trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("["));
            if (addLevel) {
                indent.append(" ".repeat(tabSize));
            }

            textArea.insert("\n" + indent, caretPos);
        } catch (BadLocationException ex) {
            textArea.insert("\n", textArea.getCaretPosition());
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the underlying {@link JScrollPane} containing the text area
     * and line number panel. Add this to your layout.
     *
     * @return the scroll pane
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Returns the underlying {@link JTextArea}.
     *
     * @return the text area
     */
    public JTextArea getTextArea() {
        return textArea;
    }

    /**
     * Returns the currently selected text, or an empty string if nothing is selected.
     *
     * @return selected text
     */
    public String getSelectedText() {
        String sel = textArea.getSelectedText();
        return sel != null ? sel : "";
    }

    /**
     * Replaces the current selection with the given replacement text.
     * If no text is selected, inserts at the caret position.
     *
     * @param replacement text to insert in place of the selection
     */
    public void replaceSelection(String replacement) {
        textArea.replaceSelection(replacement);
    }

    /**
     * Returns the 1-based line number at the caret position.
     *
     * @return current line number
     */
    public int getCurrentLine() {
        try {
            return textArea.getLineOfOffset(textArea.getCaretPosition()) + 1;
        } catch (BadLocationException e) {
            return 1;
        }
    }

    /**
     * Returns the 1-based column number at the caret position.
     *
     * @return current column number
     */
    public int getCurrentColumn() {
        try {
            int caret = textArea.getCaretPosition();
            int lineStart = textArea.getLineStartOffset(
                textArea.getLineOfOffset(caret));
            return (caret - lineStart) + 1;
        } catch (BadLocationException e) {
            return 1;
        }
    }

    /**
     * Applies a new font size to the text area.
     *
     * @param size new font size in points
     */
    public void setFontSize(int size) {
        Font current = textArea.getFont();
        textArea.setFont(current.deriveFont((float) size));
        lineNumberPanel.repaint();
    }

    /**
     * Updates the tab size used for Tab-key expansion.
     *
     * @param size number of spaces per tab
     */
    public void setTabSize(int size) {
        this.tabSize = size;
        textArea.setTabSize(size);
    }

    /**
     * Re-applies the colour palette from a new ThemeManager instance.
     *
     * @param newTheme the new theme manager
     */
    public void applyTheme(ThemeManager newTheme) {
        this.themeManager = newTheme;
        textArea.setBackground(newTheme.getBackground());
        textArea.setForeground(newTheme.getForeground());
        textArea.setCaretColor(newTheme.getCaretColor());
        textArea.setSelectionColor(newTheme.getSelectionColor());
        textArea.setSelectedTextColor(newTheme.getForeground());
        scrollPane.setBackground(newTheme.getBackground());
        lineNumberPanel.repaint();
    }

    // -----------------------------------------------------------------------
    // Inner class: LineNumberPanel
    // -----------------------------------------------------------------------

    /**
     * A custom component rendered in the scroll pane's row header that draws
     * the line numbers aligned with the text area rows.
     */
    private class LineNumberPanel extends JComponent {

        private static final int PADDING = 8;

        LineNumberPanel() {
            setBorder(new EmptyBorder(4, PADDING, 4, PADDING));
        }

        @Override
        public Dimension getPreferredSize() {
            int lineCount = textArea.getLineCount();
            String maxStr = String.valueOf(Math.max(lineCount, 99));
            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            int width = fm.stringWidth(maxStr) + PADDING * 2 + 4;
            return new Dimension(width, textArea.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width  = getWidth();
            int height = getHeight();

            // Background fill
            g2.setColor(themeManager.getLineNumberBackground());
            g2.fillRect(0, 0, width, height);

            // Right border separator
            g2.setColor(themeManager.getBorderColor());
            g2.drawLine(width - 1, 0, width - 1, height);

            // Draw line numbers
            Font font = textArea.getFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);

            int lineHeight = fm.getHeight();
            int ascent     = fm.getAscent();

            int currentLine;
            try {
                currentLine = textArea.getLineOfOffset(textArea.getCaretPosition()) + 1;
            } catch (BadLocationException e) {
                currentLine = -1;
            }

            Rectangle clip = g2.getClipBounds();
            int startLine  = (clip != null ? clip.y / lineHeight : 0);
            int endLine    = (clip != null ? (clip.y + clip.height) / lineHeight + 1 : textArea.getLineCount());

            // Account for the text area's top inset
            int topInset = textArea.getInsets().top;

            for (int i = startLine; i <= Math.min(endLine, textArea.getLineCount() - 1); i++) {
                int lineNum = i + 1;
                int y       = topInset + i * lineHeight + ascent;

                // Highlight current line number
                if (lineNum == currentLine) {
                    g2.setColor(themeManager.getPrimaryAccent());
                } else {
                    g2.setColor(themeManager.getLineNumberForeground());
                }

                String numStr = String.valueOf(lineNum);
                int x = width - fm.stringWidth(numStr) - PADDING;
                g2.drawString(numStr, x, y);
            }

            g2.dispose();
        }
    }
}
