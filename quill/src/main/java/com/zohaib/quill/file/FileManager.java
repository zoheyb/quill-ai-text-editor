package com.zohaib.quill.file;

import com.zohaib.quill.editor.Document;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * FileManager — Handles all file system operations for Quill: creating new
 * documents, opening files from disk, saving, and Save As. Interacts with
 * the {@link Document} model and presents file chooser dialogs to the user.
 *
 * <p>Responsibility separation: this class contains all I/O logic and dialog
 * orchestration; it never touches UI components directly beyond JFileChooser.
 *
 * @author Zohaib
 * @version 1.0
 */
public class FileManager {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** The parent window for centering dialogs. */
    private final JFrame parentFrame;

    /** The logical document model being managed. */
    private final Document document;

    /** File chooser instance (reused for consistent starting directory). */
    private final JFileChooser fileChooser;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs a FileManager for the given parent frame and document.
     *
     * @param parentFrame the main application window (for dialog centering)
     * @param document    the document model to read/write
     */
    public FileManager(JFrame parentFrame, Document document) {
        this.parentFrame = parentFrame;
        this.document    = document;

        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Text Files (*.txt, *.md, *.java, *.py, *.js, *.html, *.css, *.xml, *.json)",
            "txt", "md", "java", "py", "js", "ts", "html", "css", "xml", "json",
            "c", "cpp", "h", "cs", "rb", "go", "rs", "kt", "swift", "sh", "yaml", "yml"
        ));
        fileChooser.setAcceptAllFileFilterUsed(true);
    }

    // -----------------------------------------------------------------------
    // File operations
    // -----------------------------------------------------------------------

    /**
     * Prompts the user to save unsaved changes if the document is dirty,
     * then resets the document to a clean, untitled empty state.
     *
     * @return {@code true} if the new-document operation should proceed;
     *         {@code false} if the user cancelled
     */
    public boolean newDocument() {
        if (document.isDirty()) {
            int choice = promptSaveChanges();
            if (choice == JOptionPane.YES_OPTION) {
                boolean saved = save();
                if (!saved) return false;
            } else if (choice == JOptionPane.CANCEL_OPTION
                    || choice == JOptionPane.CLOSED_OPTION) {
                return false;
            }
        }
        document.reset("", null);
        return true;
    }

    /**
     * Opens a file chooser, reads the selected file into the document model,
     * and returns {@code true} on success.
     *
     * @return {@code true} if a file was successfully opened
     */
    public boolean open() {
        if (document.isDirty()) {
            int choice = promptSaveChanges();
            if (choice == JOptionPane.YES_OPTION) {
                boolean saved = save();
                if (!saved) return false;
            } else if (choice == JOptionPane.CANCEL_OPTION
                    || choice == JOptionPane.CLOSED_OPTION) {
                return false;
            }
        }

        int result = fileChooser.showOpenDialog(parentFrame);
        if (result != JFileChooser.APPROVE_OPTION) return false;

        File selected = fileChooser.getSelectedFile();
        return openFile(selected);
    }

    /**
     * Opens a specific file directly (used for "recent files" or command-line args).
     *
     * @param file the file to open
     * @return {@code true} on success
     */
    public boolean openFile(File file) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            document.reset(content, file.getAbsolutePath());
            fileChooser.setCurrentDirectory(file.getParentFile());
            return true;
        } catch (IOException e) {
            showError("Could not open file:\n" + file.getAbsolutePath()
                + "\n\nReason: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves the document to its current file path. If the document is untitled,
     * delegates to {@link #saveAs()}.
     *
     * @return {@code true} on success
     */
    public boolean save() {
        if (document.getFilePath() == null) {
            return saveAs();
        }
        return writeToFile(new File(document.getFilePath()));
    }

    /**
     * Opens a file chooser to select a new save location, writes the document,
     * and updates the document's file path.
     *
     * @return {@code true} on success
     */
    public boolean saveAs() {
        // Pre-select the current file name if available
        if (document.getFilePath() != null) {
            fileChooser.setSelectedFile(new File(document.getFilePath()));
        } else {
            fileChooser.setSelectedFile(new File("Untitled.txt"));
        }

        int result = fileChooser.showSaveDialog(parentFrame);
        if (result != JFileChooser.APPROVE_OPTION) return false;

        File selected = fileChooser.getSelectedFile();

        // Append .txt if no extension
        if (!selected.getName().contains(".")) {
            selected = new File(selected.getAbsolutePath() + ".txt");
        }

        // Confirm overwrite
        if (selected.exists()) {
            int confirm = JOptionPane.showConfirmDialog(parentFrame,
                "\"" + selected.getName() + "\" already exists.\nDo you want to replace it?",
                "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return false;
        }

        boolean success = writeToFile(selected);
        if (success) {
            document.setFilePath(selected.getAbsolutePath());
        }
        return success;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Writes the document content to the given file and marks the document clean.
     *
     * @param file destination file
     * @return {@code true} on success
     */
    private boolean writeToFile(File file) {
        try {
            Files.writeString(file.toPath(), document.getContent(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            document.markClean();
            return true;
        } catch (IOException e) {
            showError("Could not save file:\n" + file.getAbsolutePath()
                + "\n\nReason: " + e.getMessage());
            return false;
        }
    }

    /**
     * Presents a "Save changes before closing?" dialog.
     *
     * @return JOptionPane.YES_OPTION, NO_OPTION, or CANCEL_OPTION
     */
    private int promptSaveChanges() {
        return JOptionPane.showConfirmDialog(
            parentFrame,
            "\"" + document.getDisplayName() + "\" has unsaved changes.\nDo you want to save before continuing?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Shows a user-friendly error dialog.
     *
     * @param message the message to display
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(parentFrame, message,
            "File Error", JOptionPane.ERROR_MESSAGE);
    }
}
