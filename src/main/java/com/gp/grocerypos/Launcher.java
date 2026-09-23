package com.gp.grocerypos;

/**
 * Application launcher entry point.
 * <p>
 * This class exists solely to work around the JavaFX classpath restriction.
 * When a class that extends {@code javafx.application.Application} is used
 * directly as the main class on the classpath (without a module path), the
 * JVM throws "JavaFX runtime components are missing".
 * <p>
 * The fix is to have a separate launcher class that does NOT extend
 * {@code Application}. This class is invisible to the JavaFX launcher check,
 * so it can safely call {@code GroceryPOSApp.main()} from the classpath.
 * <p>
 * This is the recommended pattern for JavaFX apps distributed as fat JARs
 * or run from IDEs that don't configure the module path automatically.
 */
public class Launcher {

    public static void main(String[] args) {
        GroceryPOSApp.main(args);
    }
}
