package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonPropertyTest {

    @Test
    void testJsonPropertyExistsOnGetterWithRequired() throws NoSuchMethodException {
        JsonProperty nullableGetterJsonProperty = ImmutableJacksonNullable.class.getMethod("nullable").getAnnotation(JsonProperty.class);
        JsonProperty notNullableGetterJsonProperty = ImmutableJacksonNullable.class.getMethod("notNullable").getAnnotation(JsonProperty.class);

        Assertions.assertFalse(nullableGetterJsonProperty.required());
        Assertions.assertTrue(notNullableGetterJsonProperty.required());
    }

    @Test
    void testJsonPropertyExistsOnBuilderSetterWithRequired() throws NoSuchMethodException {
        JsonProperty nullableBuilderSetterJsonProperty = ImmutableJacksonNullable.Builder.class.getMethod("nullable", String.class).getAnnotation(JsonProperty.class);
        JsonProperty notNullableBuilderSetterJsonProperty = ImmutableJacksonNullable.Builder.class.getMethod("notNullable", String.class).getAnnotation(JsonProperty.class);

        Assertions.assertFalse(nullableBuilderSetterJsonProperty.required());
        Assertions.assertTrue(notNullableBuilderSetterJsonProperty.required());
    }
}
