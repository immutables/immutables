package org.immutables.fixture.builder;

import org.immutables.value.Value;

/**
 * Contains immutable classes using parent-of-builder pattern. The builder's parent has a bit of commonplace Javadoc
 * that happens to contain a '{' symbol.
 */
public class JavadocParentOfBuilder {

    @Value.Immutable
    static class WithSpace {

        /**
         * Builder class for {@link WithSpace}.
         */
        static class Builder extends ImmutableWithSpace.Builder { }

    }

    @Value.Immutable
    static class WithoutSpace {

        /**
         * Builder class for {@link WithoutSpace}.
         */
        static class Builder extends ImmutableWithoutSpace.Builder{}

    }

}
