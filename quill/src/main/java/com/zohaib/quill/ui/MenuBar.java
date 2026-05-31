package com.zohaib.quill.ui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * MenuBar — Constructs the full application menu bar for Quill, including all
 * menus (File, Edit, View, AI, Help) and their sub-items with keyboard shortcuts.
 *
 * <p>All {@link ActionListener} references are injected by {@code EditorWindow},
 * keeping this class purely concerned with menu construction — not behaviour.
 *
 * @author Zohaib
 * @version 1.0
 */
public class MenuBar extends JMenuBar {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private ThemeManager themeManager;

    // Kept for theme toggle label update
    private JMenuItem themeMenuItem;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Builds and populates the entire menu bar.
     */
    public MenuBar(ThemeManager themeManager,
                   ActionListener onNew,     ActionListener onOpen,
                   ActionListener onSave,    ActionListener onSaveAs,
                   ActionListener onExit,
                   ActionListener onUndo,    ActionListener onRedo,
                   ActionListener onCut,     ActionListener onCopy,
                   ActionListener onPaste,   ActionListener onSelectAll,
                   ActionListener onFind,
                   ActionListener onIncrFont, ActionListener onDecrFont,
                   ActionListener onTab2,    ActionListener onTab4,
                   ActionListener onToggleAI, ActionListener onTheme,
                   ActionListener onFix,     ActionListener onSummarize,
                   ActionListener onExplain, ActionListener onSetApiKey,
                   ActionListener onAbout,
                   Consumer<String> onScale) {

        this.themeManager = themeManager;

        // ── File menu ─────────────────────────────────────────────────────────
        JMenu fileMenu = menu("File", KeyEvent.VK_F);
        fileMenu.add(item("New",       KeyEvent.VK_N, ctrl(KeyEvent.VK_N), onNew));
        fileMenu.add(item("Open…",     KeyEvent.VK_O, ctrl(KeyEvent.VK_O), onOpen));
        fileMenu.addSeparator();
        fileMenu.add(item("Save",      KeyEvent.VK_S, ctrl(KeyEvent.VK_S), onSave));
        fileMenu.add(item("Save As…",  KeyEvent.VK_A, ctrlShift(KeyEvent.VK_S), onSaveAs));
        fileMenu.addSeparator();
        fileMenu.add(item("Exit",      KeyEvent.VK_X, ctrl(KeyEvent.VK_Q), onExit));

        // ── Edit menu ─────────────────────────────────────────────────────────
        JMenu editMenu = menu("Edit", KeyEvent.VK_E);
        editMenu.add(item("Undo",         KeyEvent.VK_U, ctrl(KeyEvent.VK_Z), onUndo));
        editMenu.add(item("Redo",         KeyEvent.VK_R, ctrl(KeyEvent.VK_Y), onRedo));
        editMenu.addSeparator();
        editMenu.add(item("Cut",          KeyEvent.VK_T, ctrl(KeyEvent.VK_X), onCut));
        editMenu.add(item("Copy",         KeyEvent.VK_C, ctrl(KeyEvent.VK_C), onCopy));
        editMenu.add(item("Paste",        KeyEvent.VK_P, ctrl(KeyEvent.VK_V), onPaste));
        editMenu.addSeparator();
        editMenu.add(item("Select All",   KeyEvent.VK_A, ctrl(KeyEvent.VK_A), onSelectAll));
        editMenu.addSeparator();
        editMenu.add(item("Find & Replace…", KeyEvent.VK_F, ctrl(KeyEvent.VK_F), onFind));

        // ── View menu ─────────────────────────────────────────────────────────
        JMenu viewMenu = menu("View", KeyEvent.VK_V);
        viewMenu.add(item("Increase Font Size", KeyEvent.VK_I,
            ctrl(KeyEvent.VK_EQUALS), onIncrFont));
        viewMenu.add(item("Decrease Font Size", KeyEvent.VK_D,
            ctrl(KeyEvent.VK_MINUS), onDecrFont));
        viewMenu.addSeparator();

        JMenu tabMenu = new JMenu("Tab Size");
        tabMenu.setMnemonic(KeyEvent.VK_T);
        tabMenu.add(item("2 Spaces", KeyEvent.VK_2, null, onTab2));
        tabMenu.add(item("4 Spaces", KeyEvent.VK_4, null, onTab4));
        viewMenu.add(tabMenu);
        viewMenu.addSeparator();

        JMenu scalingMenu = new JMenu("UI Scaling");
        scalingMenu.setMnemonic(KeyEvent.VK_S);
        scalingMenu.add(item("Auto-detect", KeyEvent.VK_A, null, e -> onScale.accept("auto")));
        scalingMenu.add(item("1.0x (Normal)", KeyEvent.VK_1, null, e -> onScale.accept("1.0")));
        scalingMenu.add(item("1.25x", KeyEvent.VK_C, null, e -> onScale.accept("1.25")));
        scalingMenu.add(item("1.5x", KeyEvent.VK_D, null, e -> onScale.accept("1.5")));
        scalingMenu.add(item("2.0x (High DPI)", KeyEvent.VK_2, null, e -> onScale.accept("2.0")));
        scalingMenu.add(item("2.5x", KeyEvent.VK_E, null, e -> onScale.accept("2.5")));
        viewMenu.add(scalingMenu);
        viewMenu.addSeparator();

        viewMenu.add(item("Toggle AI Panel", KeyEvent.VK_A,
            ctrl(KeyEvent.VK_BACK_SLASH), onToggleAI));

        String themeLabel = themeManager.isDark() ? "Switch to Light Theme" : "Switch to Dark Theme";
        themeMenuItem = item(themeLabel, KeyEvent.VK_H, ctrl(KeyEvent.VK_T), onTheme);
        viewMenu.add(themeMenuItem);

        // ── AI menu ───────────────────────────────────────────────────────────
        JMenu aiMenu = menu("AI", KeyEvent.VK_A);
        aiMenu.add(item("✦ Fix & Rephrase",  KeyEvent.VK_F,
            ctrlAlt(KeyEvent.VK_F), onFix));
        aiMenu.add(item("✦ Summarize",        KeyEvent.VK_S,
            ctrlAlt(KeyEvent.VK_S), onSummarize));
        aiMenu.add(item("✦ Explain Code",     KeyEvent.VK_E,
            ctrlAlt(KeyEvent.VK_E), onExplain));
        aiMenu.addSeparator();
        aiMenu.add(item("Set API Key…",       KeyEvent.VK_K, null, onSetApiKey));

        // ── Help menu ─────────────────────────────────────────────────────────
        JMenu helpMenu = menu("Help", KeyEvent.VK_H);
        helpMenu.add(item("About Quill", KeyEvent.VK_A,
            KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), onAbout));

        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(aiMenu);
        add(helpMenu);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Updates the theme toggle menu item label after a theme switch.
     *
     * @param isDark true if the new theme is dark
     */
    public void updateThemeLabel(boolean isDark) {
        if (themeMenuItem != null) {
            themeMenuItem.setText(isDark ? "Switch to Light Theme" : "Switch to Dark Theme");
        }
    }

    // -----------------------------------------------------------------------
    // Private factory helpers
    // -----------------------------------------------------------------------

    /** Creates a menu with a mnemonic. */
    private JMenu menu(String text, int mnemonic) {
        JMenu m = new JMenu(text);
        m.setMnemonic(mnemonic);
        return m;
    }

    /** Creates a menu item with optional mnemonic and keyboard shortcut. */
    private JMenuItem item(String text, int mnemonic, KeyStroke accelerator,
                           ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.addActionListener(action);
        return item;
    }

    /** Ctrl + key shortcut. */
    private KeyStroke ctrl(int key) {
        return KeyStroke.getKeyStroke(key, java.awt.event.InputEvent.CTRL_DOWN_MASK);
    }

    /** Ctrl+Shift + key shortcut. */
    private KeyStroke ctrlShift(int key) {
        return KeyStroke.getKeyStroke(key,
            java.awt.event.InputEvent.CTRL_DOWN_MASK |
            java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }

    /** Ctrl+Alt + key shortcut. */
    private KeyStroke ctrlAlt(int key) {
        return KeyStroke.getKeyStroke(key,
            java.awt.event.InputEvent.CTRL_DOWN_MASK |
            java.awt.event.InputEvent.ALT_DOWN_MASK);
    }
}
