package sk.edpn.spec;

import java.util.List;

/**
 * One node of the archetype tree.
 *
 * @param archetypeId archetype the node is defined in; for nodes inside a slot, the nested archetype
 * @param min         lower bound of {@code occurrences}
 * @param type        value type of an ELEMENT, {@code null} otherwise
 * @param root        fixed {@code @root} of an INSTANCE_IDENTIFIER, {@code null} when any OID is allowed
 */
public record SpecNode(String key, String rm, String archetypeId, String atCode, int min,
                       String type, String codeSystem, String root, List<SpecNode> children) {

    public String fullId() {
        return archetypeId + "/" + atCode;
    }

    public boolean isElement() {
        return "ELEMENT".equals(rm);
    }

    public String collectionName() {
        return "ENTRY".equals(rm) ? "items" : "parts";
    }
}
