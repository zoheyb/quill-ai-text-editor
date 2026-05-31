package com.zohaib.quill.ui;

import java.awt.*;

/**
 * ThemeManager — Central authority for the Quill application's visual theme.
 * Provides colour constants for both dark and light modes and applies them
 * uniformly across all Swing components via the provided colour accessor methods.
 *
 * <p>Design tokens follow a consistent semantic naming convention so callers
 * never reference raw hex values. When the theme switches, a new ThemeManager
 * is created and {@code applyTheme()} is called by {@code EditorWindow}.
 *
 * @author Zohaib
 * @version 1.0
 */
public class ThemeManager {

    // -----------------------------------------------------------------------
    // Supported themes
    // -----------------------------------------------------------------------

    public enum Theme { DARK, LIGHT }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private Theme currentTheme;

    // ── Dark palette ─────────────────────────────────────────────────────────
    private static final Color D_BG             = new Color(0x1e, 0x1e, 0x2e);  // Catppuccin base
    private static final Color D_BG_SECONDARY   = new Color(0x18, 0x18, 0x25);  // deeper bg
    private static final Color D_SURFACE        = new Color(0x24, 0x24, 0x38);  // surface 0
    private static final Color D_OVERLAY        = new Color(0x2c, 0x2c, 0x44);  // surface 1
    private static final Color D_FG             = new Color(0xcd, 0xd6, 0xf4);  // text
    private static final Color D_SUBTLE         = new Color(0x6c, 0x70, 0x86);  // subtext 0
    private static final Color D_ACCENT         = new Color(0x89, 0xb4, 0xfa);  // blue accent
    private static final Color D_ACCENT2        = new Color(0xcb, 0xa6, 0xf7);  // mauve accent
    private static final Color D_BORDER         = new Color(0x31, 0x32, 0x44);
    private static final Color D_LINE_NUM_BG    = new Color(0x18, 0x18, 0x2b);
    private static final Color D_LINE_NUM_FG    = new Color(0x4c, 0x50, 0x6a);
    private static final Color D_SELECTION      = new Color(0x39, 0x3f, 0x60);
    private static final Color D_BTN_BG         = new Color(0x31, 0x32, 0x44);
    private static final Color D_BTN_HOVER      = new Color(0x45, 0x47, 0x5a);
    private static final Color D_TOOLBAR_BG     = new Color(0x18, 0x18, 0x25);
    private static final Color D_STATUS_BG      = new Color(0x14, 0x14, 0x21);
    private static final Color D_HEADER_BG      = new Color(0x1a, 0x1a, 0x2e);
    private static final Color D_CARET          = new Color(0xf3, 0x8b, 0xa8);  // pink caret

    // ── Light palette ────────────────────────────────────────────────────────
    private static final Color L_BG             = new Color(0xfa, 0xfa, 0xfa);
    private static final Color L_BG_SECONDARY   = new Color(0xf0, 0xf0, 0xf5);
    private static final Color L_SURFACE        = new Color(0xee, 0xee, 0xf5);
    private static final Color L_OVERLAY        = new Color(0xe4, 0xe4, 0xee);
    private static final Color L_FG             = new Color(0x1e, 0x1e, 0x2e);
    private static final Color L_SUBTLE         = new Color(0x6c, 0x70, 0x86);
    private static final Color L_ACCENT         = new Color(0x17, 0x65, 0xd9);
    private static final Color L_ACCENT2        = new Color(0x82, 0x50, 0xdf);
    private static final Color L_BORDER         = new Color(0xd0, 0xd0, 0xe0);
    private static final Color L_LINE_NUM_BG    = new Color(0xf0, 0xf0, 0xf8);
    private static final Color L_LINE_NUM_FG    = new Color(0xa0, 0xa0, 0xb8);
    private static final Color L_SELECTION      = new Color(0xc8, 0xd8, 0xf8);
    private static final Color L_BTN_BG         = new Color(0xe8, 0xe8, 0xf2);
    private static final Color L_BTN_HOVER      = new Color(0xd8, 0xd8, 0xec);
    private static final Color L_TOOLBAR_BG     = new Color(0xf5, 0xf5, 0xfb);
    private static final Color L_STATUS_BG      = new Color(0xec, 0xec, 0xf6);
    private static final Color L_HEADER_BG      = new Color(0xf0, 0xf0, 0xfa);
    private static final Color L_CARET          = new Color(0xe5, 0x3e, 0x5e);

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a ThemeManager set to the specified theme string.
     *
     * @param themeString "dark" or "light" (case-insensitive); defaults to dark
     */
    public ThemeManager(String themeString) {
        this.currentTheme = "light".equalsIgnoreCase(themeString) ? Theme.LIGHT : Theme.DARK;
    }

    /**
     * Creates a ThemeManager set to the specified {@link Theme} enum value.
     *
     * @param theme the theme to use
     */
    public ThemeManager(Theme theme) {
        this.currentTheme = theme;
    }

    // -----------------------------------------------------------------------
    // Theme accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the current active theme.
     *
     * @return current {@link Theme}
     */
    public Theme getCurrentTheme() { return currentTheme; }

    /**
     * Returns {@code true} when dark theme is active.
     *
     * @return true for dark mode
     */
    public boolean isDark() { return currentTheme == Theme.DARK; }

    /**
     * Switches the theme to the opposite mode.
     *
     * @return this manager (for fluent use)
     */
    public ThemeManager toggleTheme() {
        currentTheme = isDark() ? Theme.LIGHT : Theme.DARK;
        return this;
    }

    /**
     * Returns the theme name as a lowercase string suitable for config persistence.
     *
     * @return "dark" or "light"
     */
    public String getThemeName() { return isDark() ? "dark" : "light"; }

    // -----------------------------------------------------------------------
    // Colour tokens
    // -----------------------------------------------------------------------

    public Color getBackground()             { return isDark() ? D_BG           : L_BG; }
    public Color getSecondaryBackground()    { return isDark() ? D_BG_SECONDARY : L_BG_SECONDARY; }
    public Color getSurface()                { return isDark() ? D_SURFACE      : L_SURFACE; }
    public Color getOverlay()                { return isDark() ? D_OVERLAY      : L_OVERLAY; }
    public Color getForeground()             { return isDark() ? D_FG           : L_FG; }
    public Color getSubtleText()             { return isDark() ? D_SUBTLE       : L_SUBTLE; }
    public Color getPrimaryAccent()          { return isDark() ? D_ACCENT       : L_ACCENT; }
    public Color getSecondaryAccent()        { return isDark() ? D_ACCENT2      : L_ACCENT2; }
    public Color getBorderColor()            { return isDark() ? D_BORDER       : L_BORDER; }
    public Color getLineNumberBackground()   { return isDark() ? D_LINE_NUM_BG  : L_LINE_NUM_BG; }
    public Color getLineNumberForeground()   { return isDark() ? D_LINE_NUM_FG  : L_LINE_NUM_FG; }
    public Color getSelectionColor()         { return isDark() ? D_SELECTION    : L_SELECTION; }
    public Color getButtonBackground()       { return isDark() ? D_BTN_BG       : L_BTN_BG; }
    public Color getButtonHoverBackground()  { return isDark() ? D_BTN_HOVER    : L_BTN_HOVER; }
    public Color getToolbarBackground()      { return isDark() ? D_TOOLBAR_BG   : L_TOOLBAR_BG; }
    public Color getStatusBarBackground()    { return isDark() ? D_STATUS_BG    : L_STATUS_BG; }
    public Color getPanelHeaderBackground()  { return isDark() ? D_HEADER_BG    : L_HEADER_BG; }
    public Color getCaretColor()             { return isDark() ? D_CARET        : L_CARET; }

    // -----------------------------------------------------------------------
    // Font helpers
    // -----------------------------------------------------------------------

    /**
     * Returns a suitably sized editor-compatible monospaced font.
     *
     * @param size font size in points
     * @return the best available monospaced font
     */
    public Font getEditorFont(int size) {
        // Prefer JetBrains Mono / Fira Code / Cascadia Code / Consolas / fallback
        String[] candidates = {
            "JetBrains Mono", "Fira Code", "Cascadia Code",
            "Consolas", "Menlo", "Courier New", Font.MONOSPACED
        };
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = java.util.Set.of(ge.getAvailableFontFamilyNames());
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return new Font(candidate, Font.PLAIN, size);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    /**
     * Returns the UI font used for labels, buttons, and menus.
     *
     * @param size font size in points
     * @return the best available sans-serif UI font
     */
    public Font getUIFont(int size) {
        String[] candidates = {"Segoe UI", "Inter", "Roboto", "Ubuntu", Font.SANS_SERIF};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = java.util.Set.of(ge.getAvailableFontFamilyNames());
        for (String c : candidates) {
            if (available.contains(c)) {
                return new Font(c, Font.PLAIN, size);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, size);
    }
}
