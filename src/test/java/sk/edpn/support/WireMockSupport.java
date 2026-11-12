package sk.edpn.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import java.net.URI;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Starts WireMock once per test run on a random port with stubs from {@code src/test/resources/wiremock}.
 * The server is static because it lives for the whole run; per-scenario state stays in ScenarioContext.
 */
public final class WireMockSupport {

    private static WireMockServer server;

    private WireMockSupport() {
    }

    @BeforeAll
    public static void start() {
        if (isExternal()) {
            return;
        }
        server = new WireMockServer(options().dynamicPort().usingFilesUnderClasspath("wiremock"));
        server.start();
    }

    @AfterAll
    public static void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public static WireMockServer server() {
        if (server == null) {
            throw new IllegalStateException("WireMock nebeží");
        }
        return server;
    }

    /** {@code edpn.baseUrl} when set, otherwise the embedded WireMock. */
    public static String baseUrl() {
        return Config.optional("edpn.baseUrl").orElseGet(() -> server().baseUrl());
    }

    /**
     * Requests the target WireMock received. A WireMock at {@code edpn.baseUrl} is read through its admin API.
     *
     * @throws IllegalStateException when the target has no WireMock admin API, e.g. a real service
     */
    public static List<LoggedRequest> receivedRequests(RequestPatternBuilder pattern) {
        if (!isExternal()) {
            return server().findAll(pattern);
        }
        URI uri = URI.create(baseUrl());
        int port = uri.getPort() != -1 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);
        // Catches Exception: WireMock rethrows I/O failures as undeclared checked exceptions.
        try {
            return new WireMock(uri.getScheme(), uri.getHost(), port).find(pattern);
        } catch (Exception e) {
            throw new IllegalStateException(baseUrl() + " nemá WireMock admin API (/__admin), prijatý request nemožno overiť", e);
        }
    }

    private static boolean isExternal() {
        return Config.optional("edpn.baseUrl").isPresent();
    }
}
