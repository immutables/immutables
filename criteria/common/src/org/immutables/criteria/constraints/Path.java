package org.immutables.criteria.constraints;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Access to a property possibly via several paths like {@code foo.bar.qux}
 */
public final class Path implements Expression {

  private final ImmutableList<String> paths;

  private Path(List<String> paths) {
    this.paths = ImmutableList.copyOf(paths);
  }

  public List<String> paths() {
    return paths;
  }

  public Path add(String path) {
    Objects.requireNonNull(path, "path");
    Preconditions.checkArgument(!path.isEmpty(), "empty path");
    return new Path(ImmutableList.<String>builder().addAll(paths).add(path).build());
  }

  public Path concat(Path other) {
    Objects.requireNonNull(other, "other");
    return new Path(ImmutableList.<String>builder().addAll(this.paths).addAll(other.paths).build());
  }

  public static Path of(String path) {
    Objects.requireNonNull(path);
    return new Path(ImmutableList.of(path));
  }

  /**
   * Returns current path in java bean format: {@code foo.bar.qux}
   */
  public String toStringPath() {
    return String.join(".", paths());
  }

  @Override
  public String toString() {
    return toStringPath();
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }
}
