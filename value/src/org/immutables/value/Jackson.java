/*
    Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotations to integrate with jackson JSON processor.
 * Integration strategy choosen requires no registration of serializers or jackson modules.
 * Instead, all work is delegated to {@link Json.Marshaled} marshalers, while integration is handled
 * by pair of generated delegating methods that takes and returns {@code TokenBuffer} which
 * annotated with {@code JsonCreator} and {@code JsonValue} respectively.
 * <p>
 * <em>Note that as of since version 1.1 {@link Jackson.Mapped} annotation is implied when Jackson's
 * {@code @JsonDeserialize} or {@code @JsonSerialize} annotation.
 * </em>
 * @see Mapped
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
   * marshaling is delegated to JSON marshalers from <em>Immutables</em> toolkit, any nested
   * marshaling and unmarshaling is done according to rules of {@link Json Json.*} annotations,
   * including any custom marshaling routines, so Jackson's mappings and customizations will not
   * work for during marshaling nested attributes of the immutable object.
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
   * <p>
   * ImmutableValue could in turn contain
   */
  @Beta
  @Retention(RetentionPolicy.SOURCE)
  @Target({ ElementType.TYPE })
  public @interface Mapped {}
}
