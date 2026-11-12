package sk.edpn.request;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import sk.edpn.spec.Spec;
import sk.edpn.spec.SpecNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the {@code ehr_extract} request from Gherkin rows, one row per element.
 * The builder does not validate occurrences or cardinalities, so negative tests can send invalid requests.
 */
public final class RequestBuilder {

    /** One row of the Gherkin table. {@code adl} is optional and only cross-checked against the spec. */
    public record Field(String pole, String hodnota, String adl) {
    }

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final Pattern SEGMENT = Pattern.compile("(\\w+)(?:\\[(\\d+)])?");
    private static final Pattern ADL = Pattern.compile("(!)?\\s*([A-Z_]+)\\s*\\[(at\\d{4})]");
    private static final Map<String, String> TYPE_ALIASES = Map.of(
            "CV", "CODED_VALUE",
            "II", "INSTANCE_IDENTIFIER",
            "TEXT", "SIMPLE_TEXT");

    private final Spec spec;
    private final ValueFactory values;

    public RequestBuilder(Spec spec, ValueFactory values) {
        this.spec = spec;
        this.values = values;
    }

    /**
     * @param fields rows of the Gherkin table; a row with an empty value is validated but not sent
     * @param rcId   {@code @extension} of the {@code rc_id} shared by all nodes
     * @throws IllegalArgumentException when a field is unknown, ambiguous, repeated or its {@code adl} disagrees with the spec
     */
    public ObjectNode build(List<Field> fields, String rcId) {
        Instance root = new Instance(null);
        for (Field field : fields) {
            add(root, field);
        }

        ObjectNode composition = node("COMPOSITION", spec.archetypeId(), rcId);
        composition.set("content", collection(root, spec.content(), rcId));

        ObjectNode extract = JSON.objectNode();
        extract.putObject("ehr_extract").putArray("compositions").addObject().set(spec.compositionKey(), composition);
        return extract;
    }

    private void add(Instance root, Field field) {
        List<String> keys = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (String segment : field.pole().trim().split("\\.")) {
            Matcher m = SEGMENT.matcher(segment);
            if (!m.matches()) {
                throw new IllegalArgumentException("Neplatné pole '" + field.pole() + "'");
            }
            keys.add(m.group(1));
            indexes.add(m.group(2) == null ? 1 : Integer.parseInt(m.group(2)));
        }

        List<SpecNode> path = spec.findElement(keys);
        SpecNode element = path.get(path.size() - 1);
        checkAdl(field, element);
        if (field.hodnota() == null || field.hodnota().isEmpty()) {
            return;
        }

        // The row may name only the tail of the path; nodes it does not name get index 1.
        int offset = path.size() - keys.size();
        Instance current = root;
        for (int i = 0; i < path.size(); i++) {
            int index = i < offset ? 1 : indexes.get(i - offset);
            current = current.child(path.get(i), index);
        }
        if (current.value != null) {
            throw new IllegalArgumentException("Pole '" + field.pole() + "' je v tabuľke dvakrát");
        }
        current.value = field.hodnota();
    }

    private static void checkAdl(Field field, SpecNode element) {
        if (field.adl() == null || field.adl().isBlank()) {
            return;
        }
        Matcher m = ADL.matcher(field.adl().trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Stĺpec adl '" + field.adl() + "' nemá tvar '[!]TYP [atNNNN]'");
        }
        String type = TYPE_ALIASES.getOrDefault(m.group(2), m.group(2));
        boolean mandatory = m.group(1) != null;
        if (!m.group(3).equals(element.atCode()) || !type.equals(element.type()) || mandatory != (element.min() > 0)) {
            throw new IllegalArgumentException("Pole '" + field.pole() + "': adl '" + field.adl()
                    + "' nesedí so špecifikáciou " + (element.min() > 0 ? "!" : "") + element.type()
                    + " [" + element.atCode() + "]");
        }
    }

    private ArrayNode collection(Instance parent, List<SpecNode> specChildren, String rcId) {
        ArrayNode array = JSON.arrayNode();
        for (SpecNode child : specChildren) {
            for (Instance instance : parent.children.getOrDefault(child.key(), new TreeMap<>()).values()) {
                array.addObject().set(child.key(), toJson(instance, rcId));
            }
        }
        return array;
    }

    private ObjectNode toJson(Instance instance, String rcId) {
        SpecNode spec = instance.spec;
        ObjectNode json = node(spec.rm(), spec.fullId(), rcId);
        if (spec.isElement()) {
            json.set("value", values.of(spec, instance.value));
        } else {
            json.set(spec.collectionName(), collection(instance, spec.children(), rcId));
        }
        return json;
    }

    private ObjectNode node(String type, String archetypeId, String rcId) {
        ObjectNode json = JSON.objectNode();
        json.put("_type", type);
        json.put("archetype_id", archetypeId);
        ObjectNode id = json.putObject("rc_id");
        id.put("_type", "INSTANCE_IDENTIFIER");
        id.put("@root", spec.rcRoot());
        id.put("@extension", rcId);
        return json;
    }

    private static final class Instance {
        final SpecNode spec;
        final Map<String, TreeMap<Integer, Instance>> children = new HashMap<>();
        String value;

        Instance(SpecNode spec) {
            this.spec = spec;
        }

        Instance child(SpecNode node, int index) {
            return children.computeIfAbsent(node.key(), k -> new TreeMap<>())
                    .computeIfAbsent(index, i -> new Instance(node));
        }
    }
}
