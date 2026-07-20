package net.exohayvan.dissolver_enhanced.internal;

public final class RecipeLoadCoordinator {
    private final Object monitor = new Object();

    public RecipeLoadCoordinator() {
    }

    public void runExclusive(Runnable load) {
        synchronized (monitor) {
            load.run();
        }
    }
}
