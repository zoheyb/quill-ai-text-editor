package com.zohaib.quill;

import com.zohaib.quill.config.ConfigManager;
import com.zohaib.quill.editor.EditorWindow;

import javax.swing.*;
import java.awt.*;

/**
 * Quill — The main entry point of the Quill AI-Powered Text Editor.
 *
 * <p>This class is responsible only for bootstrapping the application:
 * <ol>
 *   <li>Loading the application configuration via {@link ConfigManager}</li>
 *   <li>Configuring Swing rendering hints for crisp text on HiDPI displays</li>
 *   <li>Launching the main window ({@link EditorWindow}) on the Event Dispatch Thread</li>
 * </ol>
 *
 * <p>All subsequent wiring is handled by {@link EditorWindow}.
 *
 * <p><b>Run:</b> {@code java -jar target/quill.jar} (after {@code mvn clean package})
 *
 * @author Zohaib
 * @version 1.0
 */
public class Quill {

    /**
     * Application entry point.
     *
     * @param args command-line arguments (currently unused; file paths planned for future)
     */
    public static void main(String[] args) {
        // ── Load config before touching Swing ─────────────────────────────────
        ConfigManager configManager = new ConfigManager();

        // ── Apply UI Scaling BEFORE Swing starts ─────────────────────────────
        String scaleSetting = configManager.getUIScale();
        String appliedScale = scaleSetting;
        if ("auto".equalsIgnoreCase(scaleSetting)) {
            appliedScale = detectScaleFactor();
        }

        // Set the UI scale property if it's not already overridden via JVM arg
        if (System.getProperty("sun.java2d.uiScale") == null && appliedScale != null && !appliedScale.equals("1.0")) {
            System.setProperty("sun.java2d.uiScale", appliedScale);
        }

        // ── System properties for better rendering ────────────────────────────
        System.setProperty("awt.useSystemAAFontSettings",  "on");
        System.setProperty("swing.aatext",                 "true");
        System.setProperty("sun.java2d.xrender",           "true");

        // ── Launch on the EDT ─────────────────────────────────────────────────
        SwingUtilities.invokeLater(() -> {
            try {
                // Use the system look-and-feel as a base (we override all colours manually)
                // Cross-platform Metal gives most predictable theming surface
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("[Quill] Could not set look-and-feel: " + e.getMessage());
            }

            // Set global rendering hints for smoother graphics
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            System.setProperty("swing.boldMetal", "false");

            // Boot the main window
            new EditorWindow(configManager);
        });
    }

    /**
     * Helper to detect appropriate UI scale factor based on environment or hardware.
     */
    private static String detectScaleFactor() {
        // Check environment variables first
        String gdkScale = System.getenv("GDK_SCALE");
        if (gdkScale != null && !gdkScale.isEmpty()) {
            return gdkScale;
        }
        String qtScale = System.getenv("QT_SCALE_FACTOR");
        if (qtScale != null && !qtScale.isEmpty()) {
            return qtScale;
        }

        // Auto-detect on Linux if we are running in a GUI session
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) {
            try {
                Process process = new ProcessBuilder("xrandr", "--current").start();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(" connected")) {
                            // Example: eDP-1 connected primary 3072x1728+0+0 (normal...) 310mm x 170mm
                            java.util.regex.Matcher geomMatcher = java.util.regex.Pattern
                                    .compile("(\\d+)x(\\d+)\\+\\d+\\+\\d+")
                                    .matcher(line);
                            if (geomMatcher.find()) {
                                int width = Integer.parseInt(geomMatcher.group(1));
                                int height = Integer.parseInt(geomMatcher.group(2));
                                
                                java.util.regex.Matcher mmMatcher = java.util.regex.Pattern
                                        .compile("(\\d+)mm\\s*x\\s*(\\d+)mm")
                                        .matcher(line);
                                if (mmMatcher.find()) {
                                    int mmWidth = Integer.parseInt(mmMatcher.group(1));
                                    int mmHeight = Integer.parseInt(mmMatcher.group(2));
                                    if (mmWidth > 0 && mmHeight > 0) {
                                        double dpiX = (width * 25.4) / mmWidth;
                                        double dpiY = (height * 25.4) / mmHeight;
                                        double dpi = (dpiX + dpiY) / 2.0;
                                        
                                        if (dpi >= 180) return "2.0";
                                        if (dpi >= 130) return "1.5";
                                        return "1.0";
                                    }
                                }
                                
                                // Fallback: check resolution width alone if mm info missing
                                if (width >= 3840) return "2.0";
                                if (width >= 2560) return "1.5";
                            }
                        }
                    }
                }
                process.destroy();
            } catch (Exception e) {
                // Ignore and fall back
            }
        }
        return "1.0";
    }
}
