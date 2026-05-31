package com.zohaib.quill.editor;

import com.zohaib.quill.ai.AIClient;
import com.zohaib.quill.ai.AIClient.AIException;
import com.zohaib.quill.ai.AIPanel;
import com.zohaib.quill.config.ConfigManager;
import com.zohaib.quill.file.FileManager;
import com.zohaib.quill.ui.*;
import com.zohaib.quill.undo.UndoManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

/**
 * EditorWindow — The main application window (JFrame) for Quill. Wires together
 * all components: the menu bar, toolbar, editor pane, AI panel, status bar,
 * find/replace dialog, and all action handlers.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Instantiating and laying out all UI components</li>
 *   <li>Registering observers (DocumentChangeListener, CaretListener)</li>
 *   <li>Dispatching user actions to the correct managers (FileManager, AIClient)</li>
 *   <li>Managing theme switching across all components</li>
 *   <li>Running AI operations via SwingWorker background threads</li>
 * </ul>
 *
 * @author Zohaib
 * @version 1.0
 */
public class EditorWindow extends JFrame implements Document.DocumentChangeListener {

    // -----------------------------------------------------------------------
    // Configuration and state
    // -----------------------------------------------------------------------

    private final ConfigManager configManager;
    private ThemeManager        themeManager;
    private int                 fontSize;

    // -----------------------------------------------------------------------
    // Core components
    // -----------------------------------------------------------------------

    private final Document    document;
    private EditorPane  editorPane;
    private final UndoManager undoManager;
    private final FileManager fileManager;
    private final AIClient    aiClient;

    // -----------------------------------------------------------------------
    // UI components
    // -----------------------------------------------------------------------

    private com.zohaib.quill.ui.MenuBar menuBar;
    private Toolbar           toolbar;
    private StatusBar         statusBar;
    private AIPanel           aiPanel;
    private FindReplaceDialog findReplaceDialog;

    /** Main horizontal split pane separating editor from AI panel. */
    private JSplitPane splitPane;

    /** Whether the AI panel is currently visible. */
    private boolean aiPanelVisible = true;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs the EditorWindow, initialises all components, and displays the frame.
     *
     * @param configManager the loaded application configuration
     */
    public EditorWindow(ConfigManager configManager) {
        super("Quill — Untitled");
        this.configManager = configManager;
        this.themeManager  = new ThemeManager(configManager.getTheme());
        this.fontSize      = configManager.getFontSize();

        // ── Core model ────────────────────────────────────────────────────────
        document    = new Document();
        undoManager = new UndoManager();
        aiClient    = new AIClient(configManager);
        fileManager = new FileManager(this, document);

        document.addChangeListener(this);

        // ── Build UI ─────────────────────────────────────────────────────────
        buildUI();
        applyGlobalUIDefaults();

        // ── Frame settings ────────────────────────────────────────────────────
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        setMinimumSize(new Dimension(800, 550));
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setVisible(true);

        // Focus the editor
        SwingUtilities.invokeLater(() -> editorPane.getTextArea().requestFocusInWindow());
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    /** Assembles the full UI layout. */
    private void buildUI() {
        // ── Editor pane ───────────────────────────────────────────────────────
        editorPane = new EditorPane(themeManager, undoManager, fontSize,
            configManager.getTabSize());

        // Listen to text area changes to update document model
        editorPane.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { syncDocumentContent(); }
            @Override public void removeUpdate(DocumentEvent e)  { syncDocumentContent(); }
            @Override public void changedUpdate(DocumentEvent e) { syncDocumentContent(); }
        });

        // Update status bar caret position on caret movement
        editorPane.getTextArea().addCaretListener(e -> {
            if (statusBar != null) {
                statusBar.updatePosition(
                    editorPane.getCurrentLine(), editorPane.getCurrentColumn());
            }
        });

        // ── AI panel ──────────────────────────────────────────────────────────
        aiPanel = new AIPanel(themeManager);

        // ── Split pane ────────────────────────────────────────────────────────
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            editorPane.getScrollPane(), aiPanel);
        splitPane.setResizeWeight(0.72);
        splitPane.setDividerSize(4);
        splitPane.setBackground(themeManager.getBackground());
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // ── Status bar ────────────────────────────────────────────────────────
        statusBar = new StatusBar(themeManager);
        document.addChangeListener(statusBar);

        // ── Find & Replace dialog (lazy, shown on demand) ─────────────────────
        findReplaceDialog = new FindReplaceDialog(this, editorPane.getTextArea(), themeManager);

        // ── Menu bar ──────────────────────────────────────────────────────────
        menuBar = buildMenuBar();
        setJMenuBar(menuBar);

        // ── Toolbar ───────────────────────────────────────────────────────────
        toolbar = buildToolbar();

        // ── Layout ────────────────────────────────────────────────────────────
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(themeManager.getBackground());
        mainPanel.add(toolbar,    BorderLayout.NORTH);
        mainPanel.add(splitPane,  BorderLayout.CENTER);
        mainPanel.add(statusBar,  BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Builds the full menu bar, wiring every action to the appropriate handler.
     */
    private com.zohaib.quill.ui.MenuBar buildMenuBar() {
        return new com.zohaib.quill.ui.MenuBar(themeManager,
            /* File */
            e -> newDocument(),
            e -> openDocument(),
            e -> saveDocument(),
            e -> saveDocumentAs(),
            e -> exitApplication(),
            /* Edit */
            e -> undoManager.undo(),
            e -> undoManager.redo(),
            e -> editorPane.getTextArea().cut(),
            e -> editorPane.getTextArea().copy(),
            e -> editorPane.getTextArea().paste(),
            e -> editorPane.getTextArea().selectAll(),
            e -> showFindReplace(),
            /* View */
            e -> increaseFontSize(),
            e -> decreaseFontSize(),
            e -> setTabSize(2),
            e -> setTabSize(4),
            e -> toggleAIPanel(),
            e -> toggleTheme(),
            /* AI */
            e -> runFixAndRephrase(),
            e -> runSummarize(),
            e -> runExplainCode(),
            e -> showSetApiKeyDialog(),
            /* Help */
            e -> showAboutDialog(),
            /* Scaling */
            scale -> setUIScaleSetting(scale)
        );
    }

    /** Saves scaling preference and alerts user about restart. */
    private void setUIScaleSetting(String scale) {
        configManager.setUIScale(scale);
        JOptionPane.showMessageDialog(this,
            "UI scale factor updated to: " + ("auto".equals(scale) ? "Auto-detect" : scale + "x") + "\n" +
            "Please restart Quill to apply the new scaling factor.",
            "Restart Required", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Builds the toolbar, wiring every action to the appropriate handler.
     */
    private Toolbar buildToolbar() {
        return new Toolbar(themeManager,
            e -> newDocument(),
            e -> openDocument(),
            e -> saveDocument(),
            e -> undoManager.undo(),
            e -> undoManager.redo(),
            e -> showFindReplace(),
            e -> runFixAndRephrase(),
            e -> runSummarize(),
            e -> runExplainCode(),
            e -> toggleTheme()
        );
    }

    // -----------------------------------------------------------------------
    // DocumentChangeListener
    // -----------------------------------------------------------------------

    /**
     * Called whenever the {@link Document} model changes. Updates the window
     * title to reflect the current file name and dirty state.
     *
     * @param document the changed document
     */
    @Override
    public void onDocumentChanged(Document document) {
        SwingUtilities.invokeLater(() -> {
            String dirty = document.isDirty() ? "● " : "";
            setTitle(dirty + document.getDisplayName() + " — Quill");
        });
    }

    // -----------------------------------------------------------------------
    // File actions
    // -----------------------------------------------------------------------

    /** Creates a new empty document. */
    private void newDocument() {
        if (fileManager.newDocument()) {
            editorPane.getTextArea().setText("");
            undoManager.clear();
            updateTitle();
        }
    }

    /** Opens a file via file chooser dialog. */
    private void openDocument() {
        if (fileManager.open()) {
            editorPane.getTextArea().setText(document.getContent());
            editorPane.getTextArea().setCaretPosition(0);
            undoManager.clear();
            document.markClean(); // setText fires document listener; reset dirty after
            updateTitle();
        }
    }

    /** Saves the document to its current path. */
    private void saveDocument() {
        fileManager.save();
    }

    /** Saves the document via Save As dialog. */
    private void saveDocumentAs() {
        fileManager.saveAs();
        updateTitle();
    }

    /** Confirms unsaved changes, then exits. */
    private void exitApplication() {
        if (document.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "\"" + document.getDisplayName() + "\" has unsaved changes.\nSave before exiting?",
                "Exit Quill", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                boolean saved = fileManager.save();
                if (!saved) return; // User cancelled save-as or write failed
            } else if (choice == JOptionPane.CANCEL_OPTION
                    || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
        }
        configManager.save();
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // Edit actions
    // -----------------------------------------------------------------------

    /** Shows the Find & Replace dialog. */
    private void showFindReplace() {
        findReplaceDialog.showDialog();
    }

    // -----------------------------------------------------------------------
    // View actions
    // -----------------------------------------------------------------------

    /** Increases the editor font size by 1 point, up to a max of 40. */
    private void increaseFontSize() {
        if (fontSize < 40) {
            fontSize++;
            editorPane.setFontSize(fontSize);
            configManager.setFontSize(fontSize);
        }
    }

    /** Decreases the editor font size by 1 point, down to a minimum of 8. */
    private void decreaseFontSize() {
        if (fontSize > 8) {
            fontSize--;
            editorPane.setFontSize(fontSize);
            configManager.setFontSize(fontSize);
        }
    }

    /** Sets the tab expansion size. */
    private void setTabSize(int size) {
        editorPane.setTabSize(size);
        configManager.setTabSize(size);
    }

    /** Shows or hides the AI side panel. */
    private void toggleAIPanel() {
        aiPanelVisible = !aiPanelVisible;
        aiPanel.setVisible(aiPanelVisible);
        if (aiPanelVisible) {
            splitPane.setDividerLocation(0.72);
        }
        revalidate();
        repaint();
    }

    /**
     * Toggles between dark and light themes and re-applies colours to every component.
     */
    private void toggleTheme() {
        themeManager.toggleTheme();
        configManager.setTheme(themeManager.getThemeName());

        applyGlobalUIDefaults();

        editorPane.applyTheme(themeManager);
        statusBar.applyTheme(themeManager);
        aiPanel.applyTheme(themeManager);

        // Rebuild toolbar (easiest way to swap colours cleanly)
        JPanel mainPanel = (JPanel) getContentPane();
        mainPanel.remove(toolbar);
        toolbar = buildToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);
        mainPanel.setBackground(themeManager.getBackground());

        splitPane.setBackground(themeManager.getBackground());
        splitPane.getComponent(2).setBackground(themeManager.getBorderColor()); // divider

        menuBar.updateThemeLabel(themeManager.isDark());

        statusBar.updateTheme(themeManager.getThemeName());

        SwingUtilities.updateComponentTreeUI(this);
        revalidate();
        repaint();
    }

    // -----------------------------------------------------------------------
    // AI actions
    // -----------------------------------------------------------------------

    /**
     * Runs the "Fix &amp; Rephrase" AI action on the selected text.
     * Uses SwingWorker to keep the EDT free.
     */
    private void runFixAndRephrase() {
        String selected = editorPane.getSelectedText();
        if (selected.isBlank()) {
            warnNoSelection("Fix & Rephrase");
            return;
        }

        aiPanel.setVisible(true);
        aiPanelVisible = true;
        aiPanel.showLoading("Fix & Rephrase");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return aiClient.fixAndRephrase(selected);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    editorPane.replaceSelection(result);
                    aiPanel.setResponse("✦ Fix & Rephrase\n\nThe selected text has been replaced with the improved version in the editor.\n\n---\n\n" + result);
                } catch (Exception ex) {
                    aiPanel.setError(getRootCause(ex));
                }
            }
        }.execute();
    }

    /**
     * Runs the "Summarize" AI action on the selected text.
     */
    private void runSummarize() {
        String selected = editorPane.getSelectedText();
        if (selected.isBlank()) {
            warnNoSelection("Summarize");
            return;
        }

        aiPanel.setVisible(true);
        aiPanelVisible = true;
        aiPanel.showLoading("Summarize");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return aiClient.summarize(selected);
            }

            @Override
            protected void done() {
                try {
                    aiPanel.setResponse("✦ Summary\n\n" + get());
                } catch (Exception ex) {
                    aiPanel.setError(getRootCause(ex));
                }
            }
        }.execute();
    }

    /**
     * Runs the "Explain Code" AI action on the selected text.
     */
    private void runExplainCode() {
        String selected = editorPane.getSelectedText();
        if (selected.isBlank()) {
            warnNoSelection("Explain Code");
            return;
        }

        aiPanel.setVisible(true);
        aiPanelVisible = true;
        aiPanel.showLoading("Explain Code");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return aiClient.explainCode(selected);
            }

            @Override
            protected void done() {
                try {
                    aiPanel.setResponse("✦ Code Explanation\n\n" + get());
                } catch (Exception ex) {
                    aiPanel.setError(getRootCause(ex));
                }
            }
        }.execute();
    }

    // -----------------------------------------------------------------------
    // Dialog helpers
    // -----------------------------------------------------------------------

    /** Shows a friendly warning when no text is selected before an AI action. */
    private void warnNoSelection(String action) {
        JOptionPane.showMessageDialog(this,
            "Please select some text in the editor before using \"" + action + "\".",
            "No Text Selected", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Shows a dialog allowing the user to enter/update their Groq API key. */
    private void showSetApiKeyDialog() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(420, 80));

        JLabel label = new JLabel(
            "<html>Enter your Groq API key (get one free at <b>https://console.groq.com</b>):</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPasswordField field = new JPasswordField(configManager.getApiKey());
        field.setFont(new Font("Courier New", Font.PLAIN, 12));

        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Set Groq API Key", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String key = new String(field.getPassword()).trim();
            if (!key.isEmpty()) {
                configManager.setApiKey(key);
                JOptionPane.showMessageDialog(this,
                    "API key saved to:\n" + configManager.getConfigFilePath(),
                    "API Key Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /** Shows the Help → About dialog. */
    private void showAboutDialog() {
        String html = "<html>"
            + "<div style='font-family: Segoe UI, sans-serif; padding: 8px;'>"
            + "<h2 style='color: #89b4fa; margin: 0;'>✦ Quill v1.0</h2>"
            + "<p style='margin: 4px 0 0 0; color: #cdd6f4;'>AI-Powered Text Editor</p>"
            + "<hr/>"
            + "<p><b>Built by:</b> Zohaib</p>"
            + "<p><b>University:</b> SIBAU</p>"
            + "<p><b>AI Model:</b> llama-3.3-70b-versatile (Groq)</p>"
            + "<p><b>Stack:</b> Java 17 · Swing · OkHttp · Gson · Maven</p>"
            + "<br/>"
            + "<p style='color: #6c7086; font-size: 10px;'>Select text → use AI menu or toolbar to get AI assistance.</p>"
            + "</div></html>";

        JLabel content = new JLabel(html);
        JOptionPane.showMessageDialog(this, content, "About Quill",
            JOptionPane.PLAIN_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Synchronises the text area content with the {@link Document} model.
     * Called on every Swing document change event.
     */
    private void syncDocumentContent() {
        // Temporarily remove listeners to avoid re-entrancy during content set
        String text = editorPane.getTextArea().getText();
        // Update document model without firing listeners back to the text area
        if (!text.equals(document.getContent())) {
            document.setContent(text);
        }
    }

    /** Updates the window title from the current document state. */
    private void updateTitle() {
        String dirty = document.isDirty() ? "● " : "";
        setTitle(dirty + document.getDisplayName() + " — Quill");
    }

    /**
     * Applies global Swing UI defaults (background, foreground, selection colours)
     * so that standard JOptionPane, JScrollBar, etc. match the active theme.
     */
    private void applyGlobalUIDefaults() {
        Color bg  = themeManager.getBackground();
        Color fg  = themeManager.getForeground();
        Color sel = themeManager.getSelectionColor();
        Color brd = themeManager.getBorderColor();
        Color sur = themeManager.getSurface();

        UIManager.put("Panel.background",               bg);
        UIManager.put("Panel.foreground",               fg);
        UIManager.put("OptionPane.background",          sur);
        UIManager.put("OptionPane.messageForeground",   fg);
        UIManager.put("Label.foreground",               fg);
        UIManager.put("Button.background",              themeManager.getButtonBackground());
        UIManager.put("Button.foreground",              fg);
        UIManager.put("Button.select",                  themeManager.getButtonHoverBackground());
        UIManager.put("TextField.background",           themeManager.getOverlay());
        UIManager.put("TextField.foreground",           fg);
        UIManager.put("TextField.caretForeground",      themeManager.getCaretColor());
        UIManager.put("TextField.selectionBackground",  sel);
        UIManager.put("TextField.selectionForeground",  fg);
        UIManager.put("TextArea.background",            bg);
        UIManager.put("TextArea.foreground",            fg);
        UIManager.put("TextArea.caretForeground",       themeManager.getCaretColor());
        UIManager.put("TextArea.selectionBackground",   sel);
        UIManager.put("ScrollPane.background",          bg);
        UIManager.put("ScrollBar.background",           themeManager.getSecondaryBackground());
        UIManager.put("ScrollBar.thumb",                themeManager.getOverlay());
        UIManager.put("Menu.background",                bg);
        UIManager.put("Menu.foreground",                fg);
        UIManager.put("Menu.selectionBackground",       sel);
        UIManager.put("Menu.selectionForeground",       fg);
        UIManager.put("MenuBar.background",             themeManager.getToolbarBackground());
        UIManager.put("MenuBar.foreground",             fg);
        UIManager.put("MenuItem.background",            bg);
        UIManager.put("MenuItem.foreground",            fg);
        UIManager.put("MenuItem.selectionBackground",   sel);
        UIManager.put("MenuItem.selectionForeground",   fg);
        UIManager.put("PopupMenu.background",           sur);
        UIManager.put("PopupMenu.foreground",           fg);
        UIManager.put("Separator.foreground",           brd);
        UIManager.put("SplitPane.background",           bg);
        UIManager.put("SplitPaneDivider.background",    brd);
        UIManager.put("CheckBox.background",            bg);
        UIManager.put("CheckBox.foreground",            fg);
        UIManager.put("Dialog.background",              sur);
        UIManager.put("PasswordField.background",       themeManager.getOverlay());
        UIManager.put("PasswordField.foreground",       fg);
        UIManager.put("PasswordField.caretForeground",  themeManager.getCaretColor());
    }

    /**
     * Extracts the root cause message from an exception, unwrapping
     * {@link java.util.concurrent.ExecutionException} wrappers.
     */
    private String getRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }
}
