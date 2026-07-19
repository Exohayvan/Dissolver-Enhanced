package net.exohayvan.dissolver_enhanced.mixin;

final class RecipeLoadCoordinator {
    private final Object monitor = new Object();

    void runExclusive(Runnable load) {
        synchronized (monitor) {
            load.run();
        }
    }
}
