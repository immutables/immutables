package org.immutables.fixture.style;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(
    get = {"get*", "is*"},
    validationMethod = Value.Style.ValidationMethod.NONE)
@JsonDeserialize(as = ImmutablePostData.class)
public interface PostData {
  @JsonProperty(value = "login_allowed")
  boolean isLoginAllowed();
}
