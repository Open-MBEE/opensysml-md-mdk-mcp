package com.claude.cameo.bridge.util;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DeferredStartTest {
    @Test public void readsSettingsWhenTheActivityRunsNotWhenItIsScheduled() {
        // Cameo applies saved option values between plugin init() and the after-startup activities.
        AtomicReference<String> origins = new AtomicReference<>("https://default.example");
        List<String> used = new ArrayList<>();
        DeferredStart startup = new DeferredStart(() -> used.add(origins.get()));
        assertFalse(startup.started());
        origins.set("https://default.example,http://localhost:5192");
        startup.run();
        assertTrue(startup.started());
        assertEquals(List.of("https://default.example,http://localhost:5192"), used);
    }

    @Test public void startsOnce() {
        AtomicInteger starts = new AtomicInteger();
        DeferredStart startup = new DeferredStart(starts::incrementAndGet);
        startup.run();
        startup.run();
        assertEquals(1, starts.get());
    }

    @Test public void closeBeforeTheActivityCancelsTheStart() {
        AtomicInteger starts = new AtomicInteger();
        DeferredStart startup = new DeferredStart(starts::incrementAndGet);
        startup.close();
        startup.run();
        assertEquals(0, starts.get());
        assertFalse(startup.started());
    }

    @Test public void closeDuringAStartWaitsForItToFinish() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger finished = new AtomicInteger();
        DeferredStart startup = new DeferredStart(() -> {
            entered.countDown();
            try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            finished.incrementAndGet();
        });
        Thread activity = new Thread(startup::run);
        activity.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread closer = new Thread(startup::close);
        closer.start();
        closer.join(200);
        assertTrue("close must wait for the start in progress", closer.isAlive());
        release.countDown();
        closer.join(5000);
        activity.join(5000);
        assertFalse(closer.isAlive());
        assertEquals(1, finished.get());
        assertTrue(startup.started());
    }

    @Test public void requiresAStart() {
        assertThrows(IllegalArgumentException.class, () -> new DeferredStart(null));
    }
}
