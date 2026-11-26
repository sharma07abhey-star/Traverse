package ticketing;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import ticketing.ui.AppContext;
import ticketing.ui.ConsoleApp;
import ticketing.ui.DemoRunner;

/**
 * Entry point.
 *   java ticketing.Main            interactive menu
 *   java ticketing.Main --demo     scripted walkthrough of all features
 *   java ticketing.Main --memory   force the collection-based store (ignore JDBC)
 */
public class Main {
    public static void main(String[] args) throws Exception {
        List<String> options = Arrays.asList(args);
        AppContext ctx = AppContext.create(options.contains("--memory"));
        try {
            if (options.contains("--demo")) {
                System.out.println("Storage mode: " + ctx.mode);
                new DemoRunner(ctx).run();
            } else {
                new ConsoleApp(ctx).run();
            }
        } catch (NoSuchElementException e) {
            System.out.println("\n(input closed)");
        } finally {
            ctx.shutdown();
            System.out.println("Goodbye!");
        }
    }
}
