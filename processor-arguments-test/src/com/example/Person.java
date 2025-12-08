package com.example;

import org.immutables.value.Value;

/**
 * Simple immutable person to test annotation processor options.
 * This will test -Aimmutables.annotations.pick to see which @Generated annotation is used.
 */
@Value.Immutable
public interface Person {
    String name();
    int age();
    String email();
}
