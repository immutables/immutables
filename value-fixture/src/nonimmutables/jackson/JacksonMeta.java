package nonimmutables.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize
public @interface JacksonMeta {

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @JacksonAnnotationsInside
  @JsonAnyGetter
  public @interface Any {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @JacksonAnnotationsInside
  @JsonProperty("X")
  public @interface X {}
}
