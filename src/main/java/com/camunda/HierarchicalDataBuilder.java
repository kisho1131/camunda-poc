import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;

/**
 * Utility class to build a hierarchical data structure (L1 -> L2 -> L3)
 * from a flat list of L5 records, aggregating financial metrics at each level.
 *
 * This implementation uses only nested Maps and Lists, returning a List<Map<String, Object>>
 * that is ready for template consumption. Aggregation is performed using BigDecimal
 * for maximum precision, and the final results are stored as Double (primitive type)
 * for simple FTL consumption.
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
   * Converts an Object (which might be a String, Number, or BigDecimal) to a double, safely.
   * @param value The raw value from the input map.
   * @return The double value, or 0.0 if conversion fails.
   */
  private static double safeToDouble(Object value) {
    if (value instanceof BigDecimal bd) {
      // Correctly handle BigDecimal as seen in the input file
      return bd.doubleValue();
    }
    if (value instanceof Number n) { // Java 17 pattern matching
      return n.doubleValue();
    }
    if (value instanceof String s) { // Java 17 pattern matching
      try {
        return Double.parseDouble(s.replace(",", "").trim());
      } catch (NumberFormatException e) {
        return 0.0;
      }
    }
    return 0.0;
  }

  /**
   * Converts an Object (which might be a String, Number, or BigDecimal) to a BigDecimal, safely.
   * This is used internally for high-precision aggregation.
   * @param value The raw value from the input map.
   * @return The BigDecimal value, or BigDecimal.ZERO if conversion fails.
   */
  private static BigDecimal safeToBigDecimal(Object value) {
    if (value instanceof BigDecimal bd) {
      return bd;
    }
    if (value instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    if (value instanceof String s) {
      try {
        return new BigDecimal(s.replace(",", "").trim());
      } catch (NumberFormatException e) {
        return BigDecimal.ZERO;
      }
    }
    return BigDecimal.ZERO;
  }


  /**
   * Creates a new Map node with initialized fields for aggregation.
   */
  private static Map<String, Object> createNewNode(String description) {
    Map<String, Object> node = new HashMap<>();
    node.put("description", description);
    // Initialize aggregated values to BigDecimal.ZERO for high precision summing
    VALUE_FIELDS.forEach(field -> node.put(field, BigDecimal.ZERO));
    return node;
  }

  /**
   * Aggregates the L5 values into a parent node's existing sums using BigDecimal arithmetic.
   * * @param node The parent node map (L1, L2, or L3) containing BigDecimal sums.
   * @param l5Values A map of L5 financial fields as BigDecimal.
   */
  private static void aggregateNode(Map<String, Object> node, Map<String, BigDecimal> l5Values) {
    for (Map.Entry<String, BigDecimal> entry : l5Values.entrySet()) {
      String field = entry.getKey();
      BigDecimal valueToAdd = entry.getValue();

      // Use computeIfPresent to safely update the sum
      node.computeIfPresent(field, (k, v) -> ((BigDecimal)v).add(valueToAdd));
    }
  }

  /**
   * Recursively processes the temporary 'childrenLookup' maps and converts them
   * into the final 'children' list structure required for output.
   * * It also converts the aggregated BigDecimal values back to Double for FTL consumption.
   *
   * @param nodes The collection of nodes (L1, L2, or L3) to process.
   * @return The resulting list of maps for the current level.
   */
  private static List<Map<String, Object>> postProcessHierarchy(Collection<Map<String, Object>> nodes) {
    List<Map<String, Object>> result = new ArrayList<>();

    for (Map<String, Object> node : nodes) {
      Map<String, Object> finalNode = new HashMap<>();

      // 1. Convert BigDecimal sums to Double and copy description
      for (Map.Entry<String, Object> entry : node.entrySet()) {
        if (entry.getKey().equals("description")) {
          finalNode.put(entry.getKey(), entry.getValue());
        } else if (VALUE_FIELDS.contains(entry.getKey())) {
          // Convert the precise BigDecimal sum to a standard Double for template rendering
          finalNode.put(entry.getKey(), ((BigDecimal)entry.getValue()).doubleValue());
        }
      }

      // 2. Handle nested children structure
      if (node.containsKey("childrenLookup")) {
        // Recursively process the children map values and set the final 'children' list
        Map<String, Map<String, Object>> childrenLookup = (Map<String, Map<String, Object>>) node.get("childrenLookup");
        finalNode.put("children", postProcessHierarchy(childrenLookup.values()));
      } else {
        // This is the L3 node. Its temporary list of L5 records must be renamed to 'children'.
        // We convert L5 financial fields to Double here as well.
        List<Map<String, Object>> rawL5Records = (List<Map<String, Object>>) node.get("records");

        List<Map<String, Object>> processedL5Records = rawL5Records.stream().map(rawL5 -> {
          Map<String, Object> processed = new HashMap<>(rawL5);
          VALUE_FIELDS.forEach(field ->
                processed.put(field, safeToDouble(rawL5.get(field)))
          );
          return processed;
        }).collect(Collectors.toList());

        finalNode.put("children", processedL5Records);
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

      // 1. Extract L5 values and convert them to BigDecimal map for high-precision aggregation
      Map<String, BigDecimal> l5Values = VALUE_FIELDS.stream()
            .collect(Collectors.toMap(
                  field -> field,
                  field -> safeToBigDecimal(item.get(field))
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
}
