package io.github.alhusseinzaghloul.restfulbooker.utils;

import io.github.alhusseinzaghloul.restfulbooker.client.BaseApiRequest;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Verified, automatic cleanup of created resources (Standards §3.6, O12). */
public final class CleanupRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupRegistry.class);
    private static final ThreadLocal<Deque<BaseApiRequest>> PENDING = ThreadLocal.withInitial(ArrayDeque::new);
    private static final Queue<BaseApiRequest> LEAKED = new ConcurrentLinkedQueue<>();

    private CleanupRegistry() {}

    public static void register(BaseApiRequest deleteRequest) {
        PENDING.get().push(deleteRequest);
    }

    /** Called by SuiteLifecycleListener after every test method. Never throws. */
    public static void runPending() {
        Deque<BaseApiRequest> pending = PENDING.get();
        if (pending.isEmpty()) {
            return;
        }
        Allure.step("Cleanup", () -> {
            while (!pending.isEmpty()) {
                BaseApiRequest deleteRequest = pending.pop();
                if (!deleted(deleteRequest)) {
                    LEAKED.add(deleteRequest);
                }
            }
        });
    }

    /** Called once when the suite ends. Returns the resources that are still leaked. */
    public static List<String> retryLeaked() {
        List<String> stillLeaked = new ArrayList<>();
        BaseApiRequest deleteRequest;
        while ((deleteRequest = LEAKED.poll()) != null) {
            if (!deleted(deleteRequest)) {
                stillLeaked.add(deleteRequest.toString());
            }
        }
        return stillLeaked;
    }

    private static boolean deleted(BaseApiRequest deleteRequest) {
        String failure;
        try {
            int status = deleteRequest.send().statusCode();
            if ((status >= 200 && status < 300) || deleteRequest.isAlreadyGone(status)) {
                return true;
            }
            failure = deleteRequest + " returned HTTP " + status;
        } catch (RuntimeException e) {
            failure = deleteRequest + " threw " + e;
        }
        LOG.warn("Cleanup failed: {}", failure);
        if (Allure.getLifecycle().getCurrentExecutableKey().isPresent()) { // not during the suite-end retry
            Allure.attachment("Cleanup failure", "text/plain", failure);
        }
        return false;
    }
}
