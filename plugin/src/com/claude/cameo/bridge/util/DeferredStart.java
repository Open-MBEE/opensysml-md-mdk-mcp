package com.claude.cameo.bridge.util;

/**
 * Runs the bridge's start once, after Cameo has applied the saved Environment Options.
 *
 * <p>Cameo registers a plugin's options group during {@code init()} but applies the saved
 * values to it only after every plugin has initialized, so a value read in {@code init()} is
 * always the built-in default. The plugin therefore schedules its start as an after-startup
 * activity. A close before that activity runs cancels the start; a close from another thread
 * waits for a start in progress to finish, so the caller can then stop whatever was started.
 * The lock is re-entrant: a close on the starting thread itself (for example from a modal
 * dialog the start opened) does not wait. A start that throws is not retried.
 */
public final class DeferredStart {
    private final Runnable start;
    private boolean started;
    private boolean closed;

    public DeferredStart(Runnable start) {
        if (start == null) throw new IllegalArgumentException("start is required");
        this.start = start;
    }

    /** Runs the start unless it already ran or the plugin closed first. */
    public synchronized void run() {
        if (started || closed) return;
        started = true;
        start.run();
    }

    /** Cancels a start that has not run yet. */
    public synchronized void close() {
        closed = true;
    }

    public synchronized boolean started() {
        return started;
    }
}
