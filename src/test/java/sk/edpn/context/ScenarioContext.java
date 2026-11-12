package sk.edpn.context;

import java.net.http.HttpResponse;
import java.util.UUID;

/** State of one scenario. PicoContainer creates a new instance for every scenario. */
public class ScenarioContext {

    private final String rcId = UUID.randomUUID().toString();
    private String requestBody;
    private HttpResponse<String> response;

    public String rcId() {
        return rcId;
    }

    public String requestBody() {
        if (requestBody == null) {
            throw new IllegalStateException("Request ešte nebol zostavený");
        }
        return requestBody;
    }

    public void requestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public HttpResponse<String> response() {
        if (response == null) {
            throw new IllegalStateException("Request ešte nebol odoslaný");
        }
        return response;
    }

    public void response(HttpResponse<String> response) {
        this.response = response;
    }

    public boolean hasRequest() {
        return requestBody != null;
    }

    public boolean hasResponse() {
        return response != null;
    }
}
