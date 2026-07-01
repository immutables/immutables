package org.immutables.fixture.builder;

import org.immutables.value.Value;

/**
 * Immutable class using parent-of-builder pattern. The builder's parent has a bit of commonplace Javadoc that happens
 * to contain a '{' symbol.
 */
@Value.Immutable
public abstract class JavadocParentOfBuilder {

    /**
     * Builder class for {@link JavadocParentOfBuilder}.
     */
    static class Builder extends ImmutableJavadocParentOfBuilder.Builder { }

}
