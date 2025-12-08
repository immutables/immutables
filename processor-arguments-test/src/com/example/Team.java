package com.example;

import org.immutables.value.Value;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Immutable team with collections to test -Aimmutables.guava.suppress.
 * When Guava is on classpath but suppressed, this should generate JDK collections
 * instead of Guava ImmutableList/ImmutableSet/ImmutableMap.
 */
@Value.Immutable
public interface Team {
    String name();
    List<String> members();
    Set<String> tags();
    Map<String, String> metadata();
}
