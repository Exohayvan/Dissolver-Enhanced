package net.exohayvan.dissolver_enhanced.mixin;

import static org.assertj.core.api.Assertions.assertThat;

import net.exohayvan.dissolver_enhanced.internal.RecipeJsonResult;
import net.exohayvan.dissolver_enhanced.internal.RecipeLoadCoordinator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RecipeLoadCoordinatorTest {
    @Test
    void runtimeHelpersLiveOutsideMixinOwnedPackage() {
        assertThat(RecipeLoadCoordinator.class.getPackageName()).doesNotStartWith("net.exohayvan.dissolver_enhanced.mixin");
        assertThat(RecipeJsonResult.class.getPackageName()).doesNotStartWith("net.exohayvan.dissolver_enhanced.mixin");
    }

    @Test
    void serializesOverlappingRecipeLoads() throws Exception {
        RecipeLoadCoordinator coordinator = new RecipeLoadCoordinator();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicInteger activeLoads = new AtomicInteger();
        AtomicInteger peakLoads = new AtomicInteger();

        Thread first = new Thread(() -> coordinator.runExclusive(() -> {
            peakLoads.accumulateAndGet(activeLoads.incrementAndGet(), Math::max);
            entered.countDown();
            await(release);
            activeLoads.decrementAndGet();
            completed.countDown();
        }));
        Thread second = new Thread(() -> coordinator.runExclusive(() -> {
            peakLoads.accumulateAndGet(activeLoads.incrementAndGet(), Math::max);
            activeLoads.decrementAndGet();
            completed.countDown();
        }));

        first.start();
        assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
        second.start();
        release.countDown();

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(peakLoads.get()).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for test load");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
