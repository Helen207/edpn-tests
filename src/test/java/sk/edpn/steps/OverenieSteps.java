package sk.edpn.steps;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.cucumber.java.en.Then;
import sk.edpn.context.ScenarioContext;
import sk.edpn.spec.Spec;
import sk.edpn.support.Config;
import sk.edpn.support.WireMockSupport;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OverenieSteps {

    private static final Configuration LENIENT = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
    private static final String REQUEST_RC_ID = "$.ehr_extract.compositions[0].edpn.rc_id['@extension']";

    private final ScenarioContext context;
    private final Spec spec;

    public OverenieSteps(ScenarioContext context, Spec spec) {
        this.context = context;
        this.spec = spec;
    }

    @Then("odpoveď má HTTP status {int} a result.code {string}")
    public void odpovedMaStatus(int status, String code) {
        assertEquals(status, context.response().statusCode(), "HTTP status, telo: " + context.response().body());
        assertEquals(code, String.valueOf(response().read("$.result.code", Object.class)), "result.code");
    }

    @Then("odpoveď obsahuje idPotvrdeniaDpn s @root {string} a @extension z rc_id requestu")
    public void odpovedObsahujeId(String root) {
        DocumentContext response = response();
        String id = "$.data.zapisPotvrdenieDpn.idPotvrdeniaDpn";
        assertEquals(root, response.read(id + "['@root']"), "idPotvrdeniaDpn.@root");
        assertEquals(context.rcId(), response.read(id + "['@extension']"), "idPotvrdeniaDpn.@extension");
    }

    /** An empty value asserts the node is absent, mirroring how an empty cell omits it from the request. */
    @Then("WireMock prijal request, kde uzol [{word}] má hodnotu {string}")
    public void uzolMaHodnotu(String reference, String expected) {
        List<String> expectedValues = expected.isEmpty() ? List.of() : List.of(expected);
        assertEquals(expectedValues, valuesOf(reference), "uzol " + reference + " v prijatom requeste");
    }

    @Then("WireMock prijal request, kde uzol [{word}] má hodnoty {string}")
    public void uzolMaHodnoty(String reference, String expected) {
        List<String> expectedValues = Arrays.stream(expected.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        assertEquals(expectedValues, valuesOf(reference), "uzol " + reference + " v prijatom requeste");
    }

    private List<String> valuesOf(String reference) {
        String archetypeId = spec.archetypeIdOf(reference);
        List<Map<String, Object>> nodes = receivedRequest().read("$..[?(@.archetype_id == '" + archetypeId + "')]");
        return nodes.stream().map(this::comparableValue).toList();
    }

    /** Value in the same notation the Gherkin table uses for it; nodes without a value yield their archetype_id. */
    @SuppressWarnings("unchecked")
    private String comparableValue(Map<String, Object> node) {
        Map<String, Object> value = (Map<String, Object>) node.get("value");
        if (value == null) {
            return String.valueOf(node.get("archetype_id"));
        }
        if (value.containsKey("@code")) {
            return String.valueOf(value.get("@code"));
        }
        if (value.containsKey("@extension")) {
            boolean defaultRoot = Objects.equals(value.get("@root"), spec.fixedRootOf(String.valueOf(node.get("archetype_id"))));
            return defaultRoot ? String.valueOf(value.get("@extension")) : value.get("@root") + ":" + value.get("@extension");
        }
        if (value.containsKey("@media_type")) {
            return value.get("@media_type") + ":" + value.get("@value");
        }
        return String.valueOf(value.get("@value"));
    }

    /** The request this scenario sent, found by its rc_id so other scenarios' requests cannot interfere. */
    private DocumentContext receivedRequest() {
        String endpoint = Config.get("edpn.endpoint");
        List<DocumentContext> mine = WireMockSupport.receivedRequests(postRequestedFor(urlEqualTo(endpoint))).stream()
                .map(LoggedRequest::getBodyAsString)
                .map(body -> JsonPath.using(LENIENT).parse(body))
                .filter(body -> context.rcId().equals(body.read(REQUEST_RC_ID)))
                .toList();
        assertEquals(1, mine.size(), "počet requestov s rc_id " + context.rcId() + " na " + endpoint);
        return mine.get(0);
    }

    private DocumentContext response() {
        return JsonPath.using(LENIENT).parse(context.response().body());
    }
}
