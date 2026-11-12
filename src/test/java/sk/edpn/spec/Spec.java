package sk.edpn.spec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import sk.edpn.support.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Archetype tree of one composition, loaded from the JSON file named by {@code edpn.spec}.
 * Slots are expanded, so every node knows the archetype it belongs to.
 */
public final class Spec {

    private record RawNode(String key, String rm, String at, String occ, String type,
                           String codeSystem, String root, String slot, List<RawNode> children) {
    }

    private record RawSlot(String archetype, List<RawNode> parts) {
    }

    private record RawSpec(String key, String archetype, String rcRoot,
                           List<RawNode> content, Map<String, RawSlot> slots) {
    }

    private final String compositionKey;
    private final String archetypeId;
    private final String rcRoot;
    private final List<SpecNode> content;
    private final Map<String, RawSlot> slots;
    private final List<List<SpecNode>> leafPaths = new ArrayList<>();

    public Spec() {
        this(Config.get("edpn.spec"));
    }

    // Private so PicoContainer only sees the no-arg constructor.
    private Spec(String resource) {
        RawSpec raw = read(resource);
        this.compositionKey = raw.key();
        this.archetypeId = raw.archetype();
        this.rcRoot = raw.rcRoot();
        this.slots = raw.slots();
        this.content = resolve(raw.content(), raw.archetype());
        collectLeafPaths(content, new ArrayList<>());
    }

    public String compositionKey() {
        return compositionKey;
    }

    public String archetypeId() {
        return archetypeId;
    }

    public String rcRoot() {
        return rcRoot;
    }

    public List<SpecNode> content() {
        return content;
    }

    /**
     * Finds the element whose key path ends with {@code keys}, e.g. {@code [diagnoza, platnostOd]}.
     *
     * @return nodes from the top-level ENTRY down to the ELEMENT
     * @throws IllegalArgumentException when no element or more than one element matches
     */
    public List<SpecNode> findElement(List<String> keys) {
        List<List<SpecNode>> matches = leafPaths.stream().filter(path -> endsWith(path, keys)).toList();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        String wanted = String.join(".", keys);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Pole '" + wanted + "' nie je v špecifikácii " + archetypeId);
        }
        throw new IllegalArgumentException("Pole '" + wanted + "' je nejednoznačné, upresni cestu: "
                + matches.stream().map(Spec::dotted).collect(Collectors.joining(", ")));
    }

    /**
     * Resolves a node reference from a Gherkin step to a full {@code archetype_id}.
     * {@code at0034} refers to the composition archetype; nodes of nested archetypes
     * need the archetype name, e.g. {@code Zdravotnicky_pracovnik_odborneho_utvaru.v2/at0004}.
     */
    public String archetypeIdOf(String reference) {
        int slash = reference.indexOf('/');
        if (slash < 0) {
            return archetypeId + "/" + reference;
        }
        String name = reference.substring(0, slash);
        RawSlot slot = slots.get(name);
        if (slot == null) {
            throw new IllegalArgumentException("Neznámy archetyp '" + name + "', známe sú: " + slots.keySet());
        }
        return slot.archetype() + reference.substring(slash);
    }

    /** Fixed {@code @root} of the INSTANCE_IDENTIFIER element with this {@code archetype_id}, or {@code null}. */
    public String fixedRootOf(String fullId) {
        return leafPaths.stream()
                .map(path -> path.get(path.size() - 1))
                .filter(element -> element.fullId().equals(fullId))
                .findFirst()
                .map(SpecNode::root)
                .orElse(null);
    }

    private List<SpecNode> resolve(List<RawNode> nodes, String archetype) {
        if (nodes == null) {
            return List.of();
        }
        List<SpecNode> resolved = new ArrayList<>();
        for (RawNode raw : nodes) {
            List<SpecNode> children;
            if (raw.slot() != null) {
                RawSlot slot = slots.get(raw.slot());
                if (slot == null) {
                    throw new IllegalStateException("Slot " + raw.key() + " odkazuje na neznámy archetyp " + raw.slot());
                }
                children = resolve(slot.parts(), slot.archetype());
            } else {
                children = resolve(raw.children(), archetype);
            }
            resolved.add(new SpecNode(raw.key(), raw.rm(), archetype, raw.at(), minOccurrence(raw),
                    raw.type(), raw.codeSystem(), raw.root(), children));
        }
        return resolved;
    }

    private static int minOccurrence(RawNode raw) {
        return Integer.parseInt(raw.occ().substring(0, raw.occ().indexOf("..")));
    }

    private void collectLeafPaths(List<SpecNode> nodes, List<SpecNode> prefix) {
        for (SpecNode node : nodes) {
            List<SpecNode> path = new ArrayList<>(prefix);
            path.add(node);
            if (node.isElement()) {
                leafPaths.add(List.copyOf(path));
            } else {
                collectLeafPaths(node.children(), path);
            }
        }
    }

    private static boolean endsWith(List<SpecNode> path, List<String> keys) {
        if (keys.size() > path.size()) {
            return false;
        }
        int offset = path.size() - keys.size();
        for (int i = 0; i < keys.size(); i++) {
            if (!path.get(offset + i).key().equals(keys.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static String dotted(List<SpecNode> path) {
        return path.stream().map(SpecNode::key).collect(Collectors.joining("."));
    }

    private static RawSpec read(String resource) {
        try (InputStream in = Spec.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Špecifikácia " + resource + " nie je na classpath");
            }
            return new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                    .readValue(in, RawSpec.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
