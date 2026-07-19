package net.exohayvan.dissolver_enhanced.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RecipeLoadCoordinatorTest {
    @Test
    void runsOverlappingLoadsExclusively() throws Exception {
        RecipeLoadCoordinator coordinator = new RecipeLoadCoordinator();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        Thread first = new Thread(() -> coordinator.runExclusive(() -> {
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            firstStarted.countDown();
            try { releaseFirst.await(5, TimeUnit.SECONDS); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            active.decrementAndGet();
            completed.countDown();
        }));
        Thread second = new Thread(() -> coordinator.runExclusive(() -> {
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            active.decrementAndGet();
            completed.countDown();
        }));

        first.start();
        assertThat(firstStarted.await(5, TimeUnit.SECONDS)).isTrue();
        second.start();
        Thread.sleep(100);
        assertThat(peak.get()).isEqualTo(1);
        releaseFirst.countDown();
        assertThat(completed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(peak.get()).isEqualTo(1);
    }
}
