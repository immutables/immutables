package org.immutables.fixture.jackson;

import org.immutables.value.Value.Style.ImplementationVisibility;
import org.immutables.value.Value;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

interface Jsonable {}

@JsonSerialize // Jackson automatic integration, why not?
@Value.Style(
    typeAbstract = "Abstract*",
    typeImmutable = "*",
    visibility = ImplementationVisibility.PUBLIC)
@interface MyStyle {}

@MyStyle //<-- Meta annotated with @JsonSerialize and @Value.Style
// and applies to nested immutable objects
interface BlogEvent extends Jsonable {

  @Value.Immutable
  interface AbstractPostAdded extends BlogEvent {
    String getPostId();
    BodyChanged getContent();
  }

  @Value.Immutable
  interface AbstractBodyChanged extends BlogEvent {
    @Value.Parameter
    String getBody();
  }

  @Value.Immutable
  interface AbstractPostPublished extends BlogEvent {
    @Value.Parameter
    String getPostId();
  }
}
