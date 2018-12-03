package org.immutables.fixture.nullable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.immutables.value.Value;

public interface BaseNullableAttribute {
    @Nullable
    String attribute();

    @Value.Immutable
    public interface NullableAttribute extends BaseNullableAttribute {
    }

    @Value.Immutable
    public interface NonnullAttribute extends BaseNullableAttribute {
        @Override
        @Nonnull
        String attribute();
    }
}
