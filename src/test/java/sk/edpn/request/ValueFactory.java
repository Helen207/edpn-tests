package sk.edpn.request;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import sk.edpn.spec.Codelists;
import sk.edpn.spec.SpecNode;

/**
 * Builds the {@code value} object of an ELEMENT from the text in the Gherkin table.
 * <ul>
 *   <li>CODED_VALUE: the code; code system comes from the spec, version and display name from {@link Codelists}.
 *       A code missing from the codelist is sent without a display name, which is how negative tests send it.</li>
 *   <li>INSTANCE_IDENTIFIER: {@code root:extension}, or just the extension when the spec fixes the root.</li>
 *   <li>ATTACHMENT: {@code mediaType:content}.</li>
 *   <li>Everything else: the text as {@code @value} (assumption N11 in ANALYZA.md: always a string).</li>
 * </ul>
 */
public final class ValueFactory {

    private final Codelists codelists;

    public ValueFactory(Codelists codelists) {
        this.codelists = codelists;
    }

    public ObjectNode of(SpecNode element, String text) {
        ObjectNode value = JsonNodeFactory.instance.objectNode();
        value.put("_type", element.type());
        switch (element.type()) {
            case "CODED_VALUE" -> {
                Codelists.Codelist codelist = codelists.get(element.codeSystem());
                value.put("@code", text);
                value.put("@code_system", element.codeSystem());
                value.put("@code_schema_version", codelist.version());
                String displayName = codelist.codes().get(text);
                if (displayName != null) {
                    value.put("@display_name", displayName);
                }
            }
            case "INSTANCE_IDENTIFIER" -> {
                int colon = text.indexOf(':');
                if (colon < 0 && element.root() == null) {
                    throw new IllegalArgumentException(element.key() + ": @root nie je pevný, zadaj hodnotu ako 'root:extension'");
                }
                value.put("@root", colon < 0 ? element.root() : text.substring(0, colon));
                value.put("@extension", text.substring(colon + 1));
            }
            case "ATTACHMENT" -> {
                int colon = text.indexOf(':');
                if (colon < 0) {
                    throw new IllegalArgumentException(element.key() + ": zadaj hodnotu ako 'mediaType:obsah'");
                }
                value.put("@value", text.substring(colon + 1));
                value.put("@media_type", text.substring(0, colon));
            }
            default -> value.put("@value", text);
        }
        return value;
    }
}
