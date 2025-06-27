package org.immutables.fixture;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.immutables.value.Value;

// Should not create `import a.B` if import rewriter
@Value.Immutable
@Value.Style(passAnnotations = {JsonPropertyDescription.class})
public interface Bug1507 {

  @JsonPropertyDescription(value = "This is a string with nested quotes \"a.B\"")
  int value();
}
