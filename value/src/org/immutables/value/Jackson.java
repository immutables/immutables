package org.immutables.value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import com.google.common.annotations.Beta;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotations to integrate with jackson JSON processor.
 * Integration strategy choosen requires no registration of serializers or jackson modules.
 * Instead, all work is delegated to {@link Json.Marshaled} marshalers, while integration is handled
 * by pair of generated delegating methods that takes and returns {@code TokenBuffer} which
 * annotated with {@code JsonCreator} and {@code JsonValue} respectively.
 */
@Beta
@Retention(RetentionPolicy.SOURCE)
public @interface Jackson {
  /**
   * Makes generated immutable implementation serializable using Jackson's {@code ObjectMapper}.
   * This adds pair of methods to generated implementation:
   * 
   * <pre>
   * {@literal @}JsonCreator
   * public static ImmutableValue from(TokenBuffer buffer) throws IOException {
   * ...
   * }
   * ...
   * {@literal @}JsonValue
   * public TokenBuffer toTokenBuffer() throws IOException {
   * ...
   * }
   * </pre>
   * 
   * Annotated abstract value type requires to be annotated with {@link Value.Immutable} and
   * {@link Json.Marshaled} in order for the Jackson delegating methods to be generated. When
   * marshaling is delegated to Json marshalers from <em>Immutables</em> toolkit, any nested
   * marshaling and unmarshaling is done according to rules of {@link Json} annotations, including
   * any custom marshaling routines, so Jackson's mappings and customizations will not work for
   * during marshaling nested attributes of the immutable object.
   * <p>
   * <em>Note: Be sure to use immutable implementation
   * class when defining properties that has to be marshaled by Jackson.
   * It might not work to deserialize type if abstract value type is used as property type</em>
   * 
   * <pre>
   * class PlainObject {
   *   &#064;JsonProperty(&quot;val&quot;)
   *   ImmutableValue value;
   * }
   * </pre>
   */
  @Beta
  @Retention(RetentionPolicy.SOURCE)
  @Target({ ElementType.TYPE })
  public @interface Mapped {}
}
