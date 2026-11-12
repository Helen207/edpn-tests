package sk.edpn.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;

/** Reads {@code edpn.properties}; a JVM system property with the same name wins. */
public final class Config {

    private static final Properties FILE = load();

    private Config() {
    }

    public static String get(String key) {
        return optional(key).orElseThrow(() -> new IllegalStateException("Chýba konfigurácia " + key));
    }

    public static Optional<String> optional(String key) {
        String value = System.getProperty(key, FILE.getProperty(key));
        return Optional.ofNullable(value).filter(v -> !v.isBlank());
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("edpn.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
