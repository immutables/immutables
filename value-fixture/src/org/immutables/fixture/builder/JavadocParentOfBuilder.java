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
         * Builder class for {@link WithSpace} which extends the generated builder.
         */
        // Inline comment
        static class Builder extends
                /* Block comment */
                ImmutableWithSpace.Builder { }

    }

    @Value.Immutable
    static class WithoutSpace {

        /**
         * Builder class for {@link WithoutSpace}.
         */
        static class Builder extends/*comment*/ ImmutableWithoutSpace./*comment*/Builder{}

    }

}
