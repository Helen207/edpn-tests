package sk.edpn.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.When;
import sk.edpn.context.ScenarioContext;
import sk.edpn.request.RequestBuilder;
import sk.edpn.request.RequestBuilder.Field;
import sk.edpn.support.Config;
import sk.edpn.support.WireMockSupport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class ZapisSteps {

    private final ScenarioContext context;
    private final RequestBuilder builder;

    public ZapisSteps(ScenarioContext context, RequestBuilder builder) {
        this.context = context;
        this.builder = builder;
    }

    @When("zostavím request ZapisPotvrdenieDPN_v4 s údajmi")
    public void zostavimRequest(DataTable table) {
        List<Field> fields = table.asMaps().stream()
                .map(row -> new Field(row.get("pole"), row.get("hodnota"), row.get("adl")))
                .toList();
        context.requestBody(builder.build(fields, context.rcId()).toPrettyString());
    }

    @When("odošlem request")
    public void odoslemRequest() throws IOException, InterruptedException {
        Duration timeout = Duration.ofSeconds(Long.parseLong(Config.get("edpn.timeoutSeconds")));
        HttpRequest request = HttpRequest.newBuilder(URI.create(WireMockSupport.baseUrl() + Config.get("edpn.endpoint")))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(context.requestBody(), StandardCharsets.UTF_8))
                .build();
        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
        context.response(client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
    }

    @After
    public void prilozKReportu(Scenario scenario) {
        scenario.log("rc_id: " + context.rcId());
        if (context.hasRequest()) {
            scenario.attach(context.requestBody(), "application/json", "request");
        }
        if (context.hasResponse()) {
            scenario.attach(context.response().body(), "application/json", "response HTTP " + context.response().statusCode());
        }
    }
}
