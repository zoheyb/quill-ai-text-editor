package com.zohaib.quill.undo;

import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 * UndoManager — A thin, self-contained wrapper around {@link javax.swing.undo.UndoManager}
 * that registers itself as an {@link UndoableEditListener} on a {@link JTextComponent}
 * and exposes clean {@code undo()} and {@code redo()} methods.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Limits the undo history to 200 edits to keep memory bounded.</li>
 *   <li>Compound edits (e.g., pasting multi-line text) are treated as one
 *       single logical undo step via the compound-edit strategy built into
 *       {@code javax.swing.undo.UndoManager}.</li>
 * </ul>
 *
 * @author Zohaib
 * @version 1.0
 */
public class UndoManager implements UndoableEditListener {

    /** Maximum number of undo steps remembered. */
    private static final int UNDO_LIMIT = 200;

    /** The underlying Swing undo manager. */
    private final javax.swing.undo.UndoManager delegate;

    /** The text component this manager is attached to. */
    private JTextComponent textComponent;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates an UndoManager with no attached component yet.
     * Call {@link #attach(JTextComponent)} before use.
     */
    public UndoManager() {
        delegate = new javax.swing.undo.UndoManager();
        delegate.setLimit(UNDO_LIMIT);
    }

    // -----------------------------------------------------------------------
    // Attachment
    // -----------------------------------------------------------------------

    /**
     * Attaches this manager to a {@link JTextComponent}. Any previous
     * attachment is removed first. The undo history is cleared on attach.
     *
     * @param component the text component to track
     */
    public void attach(JTextComponent component) {
        // Detach from old component
        if (this.textComponent != null) {
            this.textComponent.getDocument().removeUndoableEditListener(this);
        }
        this.textComponent = component;
        if (component != null) {
            component.getDocument().addUndoableEditListener(this);
        }
        clear();
    }

    /**
     * Detaches this manager from its current component without clearing history.
     */
    public void detach() {
        if (textComponent != null) {
            textComponent.getDocument().removeUndoableEditListener(this);
            textComponent = null;
        }
    }

    // -----------------------------------------------------------------------
    // UndoableEditListener
    // -----------------------------------------------------------------------

    /**
     * Receives undoable edit events from the Swing document model and adds
     * them to the delegate's history.
     *
     * @param e the undoable edit event
     */
    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        delegate.addEdit(e.getEdit());
    }

    // -----------------------------------------------------------------------
    // Undo / Redo
    // -----------------------------------------------------------------------

    /**
     * Undoes the most recent edit if one is available.
     *
     * @return {@code true} if an undo was performed; {@code false} otherwise
     */
    public boolean undo() {
        if (delegate.canUndo()) {
            try {
                delegate.undo();
                return true;
            } catch (CannotUndoException e) {
                System.err.println("[UndoManager] Cannot undo: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Redoes the most recently undone edit if one is available.
     *
     * @return {@code true} if a redo was performed; {@code false} otherwise
     */
    public boolean redo() {
        if (delegate.canRedo()) {
            try {
                delegate.redo();
                return true;
            } catch (CannotRedoException e) {
                System.err.println("[UndoManager] Cannot redo: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Returns whether an undo operation is currently possible.
     *
     * @return {@code true} if undo is available
     */
    public boolean canUndo() {
        return delegate.canUndo();
    }

    /**
     * Returns whether a redo operation is currently possible.
     *
     * @return {@code true} if redo is available
     */
    public boolean canRedo() {
        return delegate.canRedo();
    }

    /**
     * Clears the entire undo/redo history.
     */
    public void clear() {
        delegate.discardAllEdits();
    }

    /**
     * Returns a human-readable presentation name for the next undo operation,
     * or an empty string if no undo is available.
     *
     * @return presentation name or empty string
     */
    public String getUndoPresentationName() {
        return delegate.canUndo() ? delegate.getUndoPresentationName() : "";
    }

    /**
     * Returns a human-readable presentation name for the next redo operation,
     * or an empty string if no redo is available.
     *
     * @return presentation name or empty string
     */
    public String getRedoPresentationName() {
        return delegate.canRedo() ? delegate.getRedoPresentationName() : "";
    }
}
