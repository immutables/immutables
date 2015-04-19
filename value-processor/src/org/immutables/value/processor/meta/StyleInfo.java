/*
    Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import java.lang.annotation.Annotation;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.meta.ValueMirrors.Enclosing;

@SuppressWarnings("all")
@Value.Immutable(intern = true, copy = false, builder = false)
public abstract class StyleInfo implements ValueMirrors.Style {

  @Override
  public Class<? extends Annotation> annotationType() {
    return ValueMirrors.Style.class;
  }

  /**
   * Patterns to recognize accessors.
   * @return naming template
   */
  @Override
  @Value.Parameter
  public abstract String[] get();

  /**
   * Builder initialization method. i.e. "setter" in builder.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String init();

  /**
   * Modify-by-copy "with" method.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String with();

  /**
   * Add value to collection attribute from iterable
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String add();

  /**
   * Add all values to collection attribute from iterable
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String addAll();

  /**
   * Puts entry to a map attribute
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String put();

  /**
   * Puts all entries to a map attribute
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String putAll();

  /**
   * Copy constructor method name.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String copyOf();

  /**
   * Constructor method name.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String of();

  /**
   * Singleton accessor method name
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String instance();

  /**
   * Builder creator method. This naming allow special keyword "new" value.
   * This will customize builder to be created using constructor rather than
   * factory method.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String builder();

  /**
   * Builder creator method, it differes from {@link #builder()} in that this naming is used for
   * builders that are external to immutable objects, such as top level builders for values or
   * factories. This naming allow special keyword "new" value, which is the default.
   * "new" will customize builder to be created using constructor rather than
   * factory method.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String newBuilder();

  /**
   * Method to initialize builder with values from instance.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String from();

  /**
   * Instance creation method on builder.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String build();

  /**
   * Builder class name.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String typeBuilder();

  /**
   * Naming templates to detect base/raw type name from provided abstract value type name.
   * @return naming templates
   */
  @Value.Parameter
  @Override
  public abstract String[] typeAbstract();

  /**
   * Modifiable type name template.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String typeImmutable();

  /**
   * Umbrella nesting class name generated using {@link Enclosing}.
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String typeImmutableEnclosing();

  /**
   * Immutable class name when generated under umbrella class using {@link Enclosing} annotation.
   * @see #typeImmutable()
   * @see #typeImmutableEnclosing()
   * @return naming template
   */
  @Value.Parameter
  @Override
  public abstract String typeImmutableNested();

  /**
   * Specify default options for the generated immutable objects.
   * If at least one attribute is specifid in inline {@literal @}{@link Immutable} annotation,
   * then this default will not be taken into account, objects will be generated using attributes
   * from inline annotation.
   * @return default configuration
   */
  @Override
  @Value.Parameter
  public abstract ValueImmutableInfo defaults();

  /**
   * When {@code true} &mdash; forces to generate strict builder code. Strict builders are forward
   * only. For collections and maps, there's no set/reset methods are generated in
   * favor of using additive only initializers. For regular attributes, initializers could be
   * called only once, subsequent reinitialization with throw exception. Usage of strict builders
   * may help to prevent initialization mistakes.
   * @return if strict builder enabled
   */
  @Value.Parameter
  @Override
  public abstract boolean strictBuilder();

  /**
   * Specify the mode in which accibility visibility is derived from abstract value type.
   * It is a good idea to not specify such attributea inline with immutable values, but rather
   * create style annotation (@see Style).
   * @return implementation visibility
   */
  @Value.Parameter
  @Override
  public abstract ImplementationVisibility visibility();

  /**
   * When {@code true} &mdash; forces to generate code which use only JDK 7+ standard library
   * classes. It is {@code false} by default, however usage of JDK-only classes will be turned on
   * automatically if <em>Google Guava</em> library is not found in classpath. The generated code
   * will have subtle differences, but nevertheless will be functionally equivalent.
   * <p>
   * <em>Note that some additional annotation processors may not work without
   * Guava being accessible to the generated classes</em>
   * @return if forced JDK-only class usage
   */
  @Value.Parameter
  @Override
  public abstract boolean jdkOnly();

  @Value.Lazy
  public Styles getStyles() {
    return new Styles(this);
  }
}
