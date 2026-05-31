package com.zohaib.quill.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * Document — Represents the logical model of a text document being edited.
 * Tracks the current text content, the file path on disk, and whether the
 * document has unsaved changes (the "dirty" flag).
 *
 * <p>Implements the Observer pattern: components (StatusBar, EditorWindow title)
 * register as {@link DocumentChangeListener} instances and are notified whenever
 * the content or dirty state changes.
 *
 * @author Zohaib
 * @version 1.0
 */
public class Document {

    /**
     * Listener interface for observers interested in document state changes.
     */
    public interface DocumentChangeListener {
        /**
         * Called when the document content or dirty state changes.
         *
         * @param document the document that changed
         */
        void onDocumentChanged(Document document);
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Current text content of the document. */
    private String content;

    /** Absolute path to the file on disk, or {@code null} for untitled docs. */
    private String filePath;

    /** Whether the document has unsaved changes. */
    private boolean dirty;

    /** Registered observers. */
    private final List<DocumentChangeListener> listeners = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates a new, untitled, empty document.
     */
    public Document() {
        this.content  = "";
        this.filePath = null;
        this.dirty    = false;
    }

    /**
     * Creates a document pre-loaded with content from the given file path.
     *
     * @param content  the text content
     * @param filePath absolute path to the backing file
     */
    public Document(String content, String filePath) {
        this.content  = content != null ? content : "";
        this.filePath = filePath;
        this.dirty    = false;
    }

    // -----------------------------------------------------------------------
    // Observer management
    // -----------------------------------------------------------------------

    /**
     * Registers a listener to receive document change notifications.
     *
     * @param listener the listener to add
     */
    public void addChangeListener(DocumentChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeChangeListener(DocumentChangeListener listener) {
        listeners.remove(listener);
    }

    /** Notifies all registered listeners of a state change. */
    private void fireChanged() {
        for (DocumentChangeListener l : listeners) {
            l.onDocumentChanged(this);
        }
    }

    // -----------------------------------------------------------------------
    // Content API
    // -----------------------------------------------------------------------

    /**
     * Returns the current text content.
     *
     * @return document text
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the text content and marks the document as dirty.
     * Notifies all registered listeners.
     *
     * @param content new text content
     */
    public void setContent(String content) {
        this.content = content != null ? content : "";
        this.dirty   = true;
        fireChanged();
    }

    /**
     * Resets the document with new content and file path, clearing dirty state.
     * Used after loading a file from disk.
     *
     * @param content  loaded text
     * @param filePath path of the loaded file
     */
    public void reset(String content, String filePath) {
        this.content  = content != null ? content : "";
        this.filePath = filePath;
        this.dirty    = false;
        fireChanged();
    }

    // -----------------------------------------------------------------------
    // File path
    // -----------------------------------------------------------------------

    /**
     * Returns the file path, or {@code null} if this is an untitled document.
     *
     * @return file path string or null
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the file path (e.g., after a Save As operation).
     *
     * @param filePath new file path
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        fireChanged();
    }

    // -----------------------------------------------------------------------
    // Dirty state
    // -----------------------------------------------------------------------

    /**
     * Returns whether the document has unsaved changes.
     *
     * @return {@code true} if the document is dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Explicitly sets the dirty flag.
     *
     * @param dirty new dirty state
     */
    public void setDirty(boolean dirty) {
        boolean changed = (this.dirty != dirty);
        this.dirty = dirty;
        if (changed) {
            fireChanged();
        }
    }

    /**
     * Marks the document as clean (no unsaved changes).
     * Typically called after a successful save.
     */
    public void markClean() {
        setDirty(false);
    }

    // -----------------------------------------------------------------------
    // Computed properties
    // -----------------------------------------------------------------------

    /**
     * Returns the display name for this document.
     * Uses the file name component of the path, or "Untitled" if no path is set.
     *
     * @return display name string
     */
    public String getDisplayName() {
        if (filePath == null || filePath.isBlank()) {
            return "Untitled";
        }
        String sep = System.getProperty("file.separator");
        int idx = filePath.lastIndexOf(sep);
        return idx >= 0 ? filePath.substring(idx + 1) : filePath;
    }

    /**
     * Counts the number of words in the current content.
     * Words are sequences of non-whitespace characters.
     *
     * @return word count
     */
    public int getWordCount() {
        if (content.isBlank()) return 0;
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    /**
     * Returns the number of characters in the current content.
     *
     * @return character count
     */
    public int getCharCount() {
        return content.length();
    }

    @Override
    public String toString() {
        return "Document{file=" + filePath + ", dirty=" + dirty
            + ", chars=" + content.length() + "}";
    }
}
