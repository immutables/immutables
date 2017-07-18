package org.immutables.value.processor.meta;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;

/**
 * API for the template to use.
 */
@Immutable
@Style(stagedBuilder = true, builderVisibility = BuilderVisibility.PACKAGE)
public abstract class  NestedBuilderDescriptor {
  /**
   *
   * @param attribute which may be a nested immutable.
   * @return null if not a nested builder, otherwise an object which can be used by the template.
   */
  @Nullable
  public static NestedBuilderDescriptor fromValueAttribute(ValueAttribute attribute) {
    NestedBuilderReflection reflection = ImmutableNestedBuilderReflection.of(attribute);

    if (!reflection.isNestedBuilder()) {
      return null;
    }

    return reflection.getNestedBuilderDescriptor();
  }

  /**
   * Helper method for the template for deciding which copy method to use.
   *
   * @return {@literal true} if {@link #getCopyMethod} is static.
   */
  public abstract boolean isCopyMethodStatic();

  /**
   * Will be one of two possible types.
   *
   * 1) A fully qualified path to a static method which create a new instance of a builder from
   * a value object. The method must be invoked with the value object as the only argument.
   * {@link #isCopyMethodStatic()} is {@literal true}.
   *
   * 2) A method which can be invoked on the value object to get a builder instance.
   * The method must take no arguments.
   * {@link #isCopyMethodStatic()} is {@literal false}.
   *
   * @return method to create a value instance. No () included.
   */
  public abstract String getCopyMethod();

  /**
   * A method to be invoked on the builder instance which returns a new value instance.
   *
   * @return a method which constructs a new value instance.
   */
  public abstract String getBuildMethod();

  /**
   * A fully qualified type for the value object.
   *
   * If {@link Style#deepImmutablesDetection()} is {@code true}, then the qualified value
   * type is the generated immutables concrete class.
   *
   * @return fully qualified name of the value type.
   */
  public abstract String getQualifiedValueTypeName();

  /**
   * A fully qualified type for the builder object.
   *
   * If using a nested static builder, then this must return the final concrete builder
   * class.
   *
   * @return fully qualified name of the builder type.
   */
  public abstract String getQualifiedBuilderTypeName();

  /**
   * Use to generate helper methods for a specific builder/value in a unique namespace.
   *
   * @return fully qualified name of the value type joined by underscores.
   */
  public String getQualifiedValueTypeNameWithUnderscores() {
    return getQualifiedValueTypeName().replaceAll("\\.", "_");
  }

  /**
   * A fully qualified path and method which creates a new instance of a builder.
   *
   * If the builder is constructed from a no-arg constructor, the {@code new} keyword
   * is prepended with a space.
   *
   * @return static path which invoked will create a new empty builder. No () included.
   */
  public abstract String getQualifiedBuilderConstructorMethod();
}
