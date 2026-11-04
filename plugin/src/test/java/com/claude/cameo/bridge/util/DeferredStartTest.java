package com.claude.cameo.bridge.util;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DeferredStartTest {
    @Test public void startsOnlyWhenTheActivityRunsAndOnlyOnce() {
        AtomicInteger starts = new AtomicInteger();
        DeferredStart startup = new DeferredStart(starts::incrementAndGet);
        assertEquals("scheduling must not start anything", 0, starts.get());
        assertFalse(startup.started());
        startup.run();
        startup.run();
        assertEquals(1, starts.get());
        assertTrue(startup.started());
    }

    @Test public void closeBeforeTheActivityCancelsTheStart() {
        AtomicInteger starts = new AtomicInteger();
        DeferredStart startup = new DeferredStart(starts::incrementAndGet);
        startup.close();
        startup.run();
        assertEquals(0, starts.get());
        assertFalse(startup.started());
    }

    @Test public void closeFromAnotherThreadWaitsForAStartInProgress() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger finished = new AtomicInteger();
        AtomicInteger finishedWhenCloseReturned = new AtomicInteger(-1);
        DeferredStart startup = new DeferredStart(() -> {
            entered.countDown();
            try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            finished.incrementAndGet();
        });
        Thread activity = new Thread(startup::run);
        activity.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread closer = new Thread(() -> {
            startup.close();
            finishedWhenCloseReturned.set(finished.get());
        });
        closer.start();
        // the closer must reach the monitor and park on it, never return while the start is running
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (closer.getState() != Thread.State.BLOCKED && closer.getState() != Thread.State.TERMINATED && System.nanoTime() < deadline) Thread.sleep(5);
        assertNotEquals("close must wait for the start in progress", Thread.State.TERMINATED, closer.getState());
        release.countDown();
        closer.join(5000);
        activity.join(5000);
        assertEquals("close returned only after the start finished", 1, finishedWhenCloseReturned.get());
        assertTrue(startup.started());
    }

    @Test public void aStartThatThrowsPropagatesAndIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("port in use");
        DeferredStart startup = new DeferredStart(() -> { attempts.incrementAndGet(); throw failure; });
        assertSame(failure, assertThrows(IllegalStateException.class, startup::run));
        assertTrue(startup.started());
        startup.run();
        assertEquals(1, attempts.get());
        startup.close();
    }

    @Test public void requiresAStart() {
        assertThrows(IllegalArgumentException.class, () -> new DeferredStart(null));
    }
}
