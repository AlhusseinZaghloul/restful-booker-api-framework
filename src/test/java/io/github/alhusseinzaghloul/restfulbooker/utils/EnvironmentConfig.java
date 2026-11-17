package io.github.alhusseinzaghloul.restfulbooker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** -Dkey > KEY env var > config/&lt;env&gt;.properties (Rules 3.2, 3.3, 3.5; O15). */
public final class EnvironmentConfig {

    private static final String ENV =
            System.getProperty("env", System.getenv().getOrDefault("TEST_ENV", "dev"));
    private static final Properties PROPS = load("config/" + ENV + ".properties");

    private EnvironmentConfig() {}

    private static Properties load(String file) {
        Properties props = new Properties();
        try (InputStream in = EnvironmentConfig.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) {
                throw new IllegalStateException("Config file not found on classpath: " + file);
            }
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String environment() {
        return ENV;
    }

    /** Required key: fails fast when missing (Rule 3.5). */
    public static String get(String key) {
        String value = lookup(key);
        if (value == null) {
            throw new IllegalStateException("Missing config key '" + key + "' for env '" + ENV + "'");
        }
        return value;
    }

    /** O15: only for the optional keys auth.type and sweep.enabled. */
    public static String get(String key, String defaultValue) {
        String value = lookup(key);
        return value == null ? defaultValue : value;
    }

    private static String lookup(String key) {
        String value = System.getProperty(key);
        if (value == null) value = System.getenv(key.toUpperCase().replace('.', '_'));
        if (value == null) value = PROPS.getProperty(key);
        return value;
    }
}
