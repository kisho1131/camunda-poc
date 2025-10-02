import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to build a hierarchical data structure (L1 -> L2 -> L3)
 * from a flat list of L5 records, aggregating financial metrics at each level.
 *
 * This implementation uses only nested Maps and Lists, returning a List<Map<String, Object>>
 * that is ready for template consumption.
 */
public class HierarchicalDataBuilder {

  // Define the financial fields that need to be aggregated (summed up)
  private static final List<String> VALUE_FIELDS = List.of(
        "currentBookValue",
        "priorBookValue",
        "mtdBookValue",
        "dodVariance",
        "mtdVariance"
  );

  /**
   * Converts an Object (which might be a String or Number) to a double, safely.
   * @param value The raw value from the input map.
   * @return The double value, or 0.0 if conversion fails.
   */
  private static double safeToDouble(Object value) {
    if (value instanceof Number n) { // Java 17 pattern matching
      return n.doubleValue();
    }
    if (value instanceof String s) { // Java 17 pattern matching
      try {
        return Double.parseDouble(s.replace(",", "").trim());
      } catch (NumberFormatException e) {
        // System.err.println("Warning: Could not parse '" + s + "' to double.");
        return 0.0;
      }
    }
    return 0.0;
  }

  /**
   * Creates a new Map node with initialized fields for aggregation.
   */
  private static Map<String, Object> createNewNode(String description) {
    Map<String, Object> node = new HashMap<>();
    node.put("description", description);
    // Initialize aggregated values to 0.0
    VALUE_FIELDS.forEach(field -> node.put(field, 0.0));
    return node;
  }

  /**
   * Aggregates the L5 values into a parent node's existing sums.
   */
  private static void aggregateNode(Map<String, Object> node, Map<String, Double> l5Values) {
    for (Map.Entry<String, Double> entry : l5Values.entrySet()) {
      String field = entry.getKey();
      Double value = entry.getValue();
      // Use computeIfPresent to safely update the sum
      node.computeIfPresent(field, (k, v) -> (Double)v + value);
    }
  }

  /**
   * Recursively processes the temporary 'childrenLookup' maps and converts them
   * into the final 'children' list structure required for output.
   *
   * @param nodes The collection of nodes (L1, L2, or L3) to process.
   * @return The resulting list of maps for the current level.
   */
  private static List<Map<String, Object>> postProcessHierarchy(Collection<Map<String, Object>> nodes) {
    List<Map<String, Object>> result = new ArrayList<>();

    for (Map<String, Object> node : nodes) {
      Map<String, Object> finalNode = new HashMap<>();

      // Copy all fields except the temporary 'childrenLookup' map
      for (Map.Entry<String, Object> entry : node.entrySet()) {
        if (!entry.getKey().equals("childrenLookup")) {
          finalNode.put(entry.getKey(), entry.getValue());
        }
      }

      // Check if this node had deeper hierarchy (L1 or L2)
      if (node.containsKey("childrenLookup")) {
        // Recursively process the children map values and set the final 'children' list
        Map<String, Map<String, Object>> childrenLookup = (Map<String, Map<String, Object>>) node.get("childrenLookup");
        finalNode.put("children", postProcessHierarchy(childrenLookup.values()));
      } else {
        // This is the L3 node. Its temporary list of L5 records must be renamed to 'children'.
        // The 'records' key was used during building for L3 leaves.
        finalNode.put("children", finalNode.remove("records"));
      }

      result.add(finalNode);
    }
    return result;
  }

  /**
   * Transforms a flat list of L5 records into a hierarchical structure (L1 -> L2 -> L3),
   * calculating the sum of financial metrics at L1, L2, and L3 levels.
   *
   * @param flatData A list of maps, where each map is an L5 record.
   * @return A list of maps representing the L1 hierarchy root, suitable for templating.
   */
  public static List<Map<String, Object>> buildHierarchy(List<Map<String, Object>> flatData) {
    // Use a Map keyed by L1 description for quick lookup and aggregation during the build.
    Map<String, Map<String, Object>> rootMap = new HashMap<>();

    for (Map<String, Object> item : flatData) {
      // Safely retrieve hierarchy keys, assuming they are Strings
      String l1Key = (String) item.get("pmfAcctLvl1Desc");
      String l2Key = (String) item.get("pmfAcctLvl2Desc");
      String l3Key = (String) item.get("pmfAcctLvl3Desc");

      if (l1Key == null || l2Key == null || l3Key == null) {
        System.err.println("Skipping item due to missing L1/L2/L3 keys: " + item);
        continue;
      }

      // 1. Extract L5 values and convert them to Double map for aggregation
      Map<String, Double> l5Values = VALUE_FIELDS.stream()
            .collect(Collectors.toMap(
                  field -> field,
                  field -> safeToDouble(item.get(field))
            ));

      // 2. Build L1 Node and Aggregate
      Map<String, Object> l1Node = rootMap.computeIfAbsent(l1Key, HierarchicalDataBuilder::createNewNode);
      aggregateNode(l1Node, l5Values);

      // 3. Build L2 Node and Aggregate (childrenLookup holds the next level for fast lookup)
      Map<String, Map<String, Object>> l2Map = (Map<String, Map<String, Object>>) l1Node.computeIfAbsent("childrenLookup", k -> new HashMap<>());
      Map<String, Object> l2Node = l2Map.computeIfAbsent(l2Key, HierarchicalDataBuilder::createNewNode);
      aggregateNode(l2Node, l5Values);

      // 4. Build L3 Node and Aggregate
      Map<String, Map<String, Object>> l3Map = (Map<String, Map<String, Object>>) l2Node.computeIfAbsent("childrenLookup", k -> new HashMap<>());
      Map<String, Object> l3Node = l3Map.computeIfAbsent(l3Key, HierarchicalDataBuilder::createNewNode);
      aggregateNode(l3Node, l5Values);

      // 5. Add L5 record (leaf) to L3 node. We use "records" temporarily.
      List<Map<String, Object>> l3Records = (List<Map<String, Object>>) l3Node.computeIfAbsent("records", k -> new ArrayList<>());
      l3Records.add(item);
    }

    // 6. Convert the temporary root map and nested lookup maps into the final List<Map<String, Object>> structure
    return postProcessHierarchy(rootMap.values());
  }

  // --- main method removed for brevity, as the request is focused on the function and FTL template ---
}
