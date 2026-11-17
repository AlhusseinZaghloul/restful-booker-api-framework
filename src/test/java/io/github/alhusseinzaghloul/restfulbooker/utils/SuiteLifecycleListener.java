package io.github.alhusseinzaghloul.restfulbooker.utils;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.DeleteBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingIdsRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.health.CheckHealthRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Suite-wide lifecycle (Rules 6.3, 6.4): health check, marker sweep, per-test cleanup, report files (O16). */
public class SuiteLifecycleListener implements ISuiteListener, IInvokedMethodListener {

    private static final Logger LOG = LoggerFactory.getLogger(SuiteLifecycleListener.class);
    private static final Path RESULTS = Path.of("target", "allure-results");
    private static final Path LEAKED_FILE = Path.of("target", "leaked-resources.txt");

    @Override
    public void onStart(ISuite suite) {
        MDC.put("test", "suite-setup");
        try {
            // Reading base.uri fails fast on bad configuration
            LOG.info("Suite '{}' starting on '{}' against {}", suite.getName(),
                    EnvironmentConfig.environment(), EnvironmentConfig.get("base.uri"));
            new CheckHealthRequest().sendAsSetup(201);
            if (Boolean.parseBoolean(EnvironmentConfig.get("sweep.enabled", "true"))) {
                sweepLeftovers();
            }
        } finally {
            MDC.remove("test");
        }
    }

    /** O3 / Rule 3.24: delete bookings left by earlier runs. Login failures abort the suite. */
    private void sweepLeftovers() {
        String marker = BookingDataFactory.MARKER_LASTNAME;
        Response search = new GetBookingIdsRequest().lastname(marker).send();
        if (search.statusCode() != 200) {
            LOG.warn("Sweep skipped: search for lastname={} returned HTTP {}", marker, search.statusCode());
            return;
        }
        List<Integer> ids = search.jsonPath().getList("bookingid", Integer.class);
        int removed = 0;
        int failed = 0;
        for (Integer id : ids) {
            DeleteBookingRequest delete = new DeleteBookingRequest(id);
            try {
                int status = delete.send().statusCode();
                if ((status >= 200 && status < 300) || delete.isAlreadyGone(status)) {
                    removed++;
                } else {
                    failed++;
                    LOG.warn("Sweep could not delete booking {}: HTTP {}", id, status);
                }
            } catch (IllegalStateException e) {
                throw e; // Login failed or missing config: infrastructure failure, abort the suite
            } catch (RuntimeException e) {
                failed++;
                LOG.warn("Sweep could not delete booking {}: {}", id, e.toString());
            }
        }
        if (failed > 0) {
            LOG.warn("Sweep removed {} leftover bookings ({} failed)", removed, failed);
        } else {
            LOG.info("Sweep removed {} leftover bookings ({} failed)", removed, failed);
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        MDC.put("test", result.getTestClass().getRealClass().getSimpleName()
                + "." + method.getTestMethod().getMethodName());
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        // After every test method, and after any configuration method that failed
        if (method.isTestMethod() || !result.isSuccess()) {
            CleanupRegistry.runPending();
        }
        MDC.remove("test");
    }

    @Override
    public void onFinish(ISuite suite) {
        List<String> leaked = CleanupRegistry.retryLeaked();
        leaked.forEach(resource -> LOG.error("Leaked resource: {}", resource));
        writeReportFiles(leaked);
    }

    private void writeReportFiles(List<String> leaked) {
        try {
            Files.createDirectories(RESULTS);
            if (!leaked.isEmpty()) {
                // Appended: the regression and known-defects suites share one results dir in CI (O16)
                Files.write(LEAKED_FILE, leaked, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            long leakedTotal = Files.exists(LEAKED_FILE) ? Files.readAllLines(LEAKED_FILE).size() : 0;
            Files.write(RESULTS.resolve("environment.properties"), List.of(
                    "env=" + EnvironmentConfig.environment(),
                    "base.uri=" + EnvironmentConfig.get("base.uri"),
                    "auth.type=" + EnvironmentConfig.get("auth.type", "none"),
                    "java.version=" + System.getProperty("java.version"),
                    "leaked.resources=" + leakedTotal));
            try (InputStream categories = getClass().getClassLoader().getResourceAsStream("categories.json")) {
                if (categories != null) {
                    Files.copy(categories, RESULTS.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            LOG.error("Could not write report files", e);
        }
    }
}
