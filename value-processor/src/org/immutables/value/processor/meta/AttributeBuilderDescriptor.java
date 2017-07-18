package org.immutables.value.processor.meta;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;

/**
 * API for the template to use.
 */
@Immutable
@Style(stagedBuilder = true, builderVisibility = BuilderVisibility.PACKAGE)
public abstract class AttributeBuilderDescriptor {

  public enum ValueToBuilderTarget {
    /**
     * Would look like {@code ValueObject.Builder builderCopy = valueInstance.toBuilder();}
     */
    VALUE_INSTANCE,
    /**
     * Would look like {@code ValueObject.Builder builderCopy = ValueObject.newBuilderFrom(valueInstance);}
     */
    VALUE_TYPE,
    /**
     * Would look like {@code ValueObject.Builder builderCopy = ValueObject.builder().merge(valueInstance);}
     */
    BUILDER_INSTANCE,
    /**
     * Would look like {@code ValueObject.Builder builderCopy = ValueObject.Builder.from(valueInstance);}
     */
    BUILDER_TYPE
  }

  public abstract ValueToBuilderTarget getValueToBuilderTarget();

  /**
   * A method name which when invoked will return a builder with all the properties set from
   * the value object.  This method will be invoked based on {@link #getValueToBuilderTarget()}
   *
   * @return method to create a value instance. No () included.
   */
  protected abstract String getValueToBuilderMethod();

  /**
   * Uses {@link #getValueToBuilderTarget()} ()} to determine appropriate format for
   * creating a new builder from a value object.
   *
   * The template needs to still query {@link #getValueToBuilderTarget()} ()} to determine
   * whether to use {@code [expression].[n.getQualifiedValueToBuilderMethod]()} vs
   * {@code [n.getQualifiedValueToBuilderMethod]([expression])}
   *
   * If a template could pass an argument, then we could handle the logic in this method.
   *
   * @return method to use for converting a value to a builder.
   */
  public String getQualifiedValueToBuilderMethod() {
    switch (getValueToBuilderTarget()) {

      case VALUE_INSTANCE:
        return getValueToBuilderMethod();
      case VALUE_TYPE:
        return String.format("%s.%s", getQualifiedValueTypeName(), getValueToBuilderMethod());
      case BUILDER_INSTANCE:
        return String
            .format("%s().%s", getQualifiedBuilderConstructorMethod(), getValueToBuilderMethod());
      case BUILDER_TYPE:
        return String.format("%s.%s", getQualifiedBuilderTypeName(), getValueToBuilderMethod());
      default:
        throw new UnsupportedOperationException(
            String.format("Could not handle %s", getValueToBuilderTarget()));
    }
  }

  /**
   * Helper for the template.
   *
   * @return if {@link #getValueToBuilderTarget()} ==  {@link ValueToBuilderTarget#VALUE_INSTANCE}
   */
  public boolean isCopyMethodOnValueInstance() {
    return getValueToBuilderTarget() == ValueToBuilderTarget.VALUE_INSTANCE;
  }

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
