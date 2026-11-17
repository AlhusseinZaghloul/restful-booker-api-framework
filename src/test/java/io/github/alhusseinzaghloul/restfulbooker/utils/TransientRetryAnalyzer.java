package io.github.alhusseinzaghloul.restfulbooker.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/** Retries transient network errors once (Rule 6.8). O19: checks the whole cause chain. */
public class TransientRetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(TransientRetryAnalyzer.class);
    private static final int MAX_RETRIES = 1;
    private int attempts;

    @Override
    public boolean retry(ITestResult result) {
        Throwable error = result.getThrowable();
        boolean retry = isTransient(error) && attempts++ < MAX_RETRIES;
        if (retry) {
            LOG.warn("Retrying {} after transient error: {}", result.getName(), error.toString());
        }
        return retry;
    }

    private static boolean isTransient(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof ConnectException) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
