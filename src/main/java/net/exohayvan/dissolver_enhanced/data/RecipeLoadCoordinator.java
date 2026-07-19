package net.exohayvan.dissolver_enhanced.data;

public final class RecipeLoadCoordinator {
    public static final RecipeLoadCoordinator GLOBAL = new RecipeLoadCoordinator();

    private final Object monitor = new Object();

    public void runExclusive(Runnable load) {
        synchronized (monitor) {
            load.run();
        }
    }
}
