/*
 * Copyright 2020 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.geode;

import com.google.common.base.Preconditions;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Tries to detect if current filter is based only on keys (IDs) and extracts them if possible.
 * Useful in cases when these is a more efficient {@code getByKey} / {@code deleteByKey} API provided
 * by backend.
 *
 * <p><strong>Usage example</strong> Geode (currently) doesn't support delete by query syntax ({@code DELETE ... WHERE ...}) and elements have to be
 * removed explicitly by key (using {@link Map#remove(Object)} or {@code Region.removeAll()} API). With this method
 * one can extract keys from expression and use delete by key API if delete query has only IDs.
 *
 * <p>Example:
 * <pre>
 *  {@code
 *     key = 123 (valid)
 *     key in [1, 2, 3] (valid)
 *
 *     key not in [1, 2, 3] (invalid since keys are unknown)
 *     key != 1 (invalid since keys are unknown)
 *     key > 1 (invalid since keys are unknown)
 *  }
 * </pre>
 */
abstract class KeyLookupAnalyzer {

  /**
   * Analyze current expression and check if it has {@code (id = 1) or (id in [...])}
   * format.
   *
   * @param filter expression to analyze
   */
  public abstract Result analyze(Expression filter);

  /**
   * Result of analyzing filter expression
   */
  public interface Result {
    /**
     * Wherever current expression can be converted to a key-lookup API similar to {@code key = 123} or {@code key in [1, 2, 3]}
     */
    boolean isOptimizable();

    /**
     * Return values present in {@code  key = 1} or {@code key in [1, 2, 3]} call
     *
     * Always check {@link #isOptimizable()} before using this method
     *
     * @return list of values (can be empty, with one or more elements)
     * @throws UnsupportedOperationException if keys could not be extracted
     */
    List<?> values();
  }

  /**
   * Always returns non-optimizable result
   */
  static KeyLookupAnalyzer disabled() {
    return new KeyLookupAnalyzer() {
      @Override
      public Result analyze(Expression filter) {
        return NOT_OPTIMIZABLE;
      }
    };
  }

  /**
   * Create analyzer based on existing extractor. Currently only
   * single key is supported and key expression has to be {@link Path}.
   */
  static KeyLookupAnalyzer fromExtractor(KeyExtractor extractor) {
    Objects.requireNonNull(extractor, "extractor");
    KeyExtractor.KeyMetadata metadata = extractor.metadata();
    if (!(metadata.isExpression() && metadata.isKeyDefined()) || metadata.keys().size() > 1) {
      return disabled();
    }

    Optional<Path> path = Visitors.maybePath(metadata.keys().get(0));
    return path.map(KeyLookupAnalyzer::forPath).orElse(disabled());
  }

  /**
   * Create optimizer when id attribute is known as {@link Path}
   */
  static KeyLookupAnalyzer forPath(Path idPath) {
    Objects.requireNonNull(idPath, "idPath");
    return new PathAnalyzer(idPath);
  }

  /**
   * Default optimization based on {@link Path}
   */
  private static class PathAnalyzer extends KeyLookupAnalyzer {
    private final Path idPath;

    private PathAnalyzer(Path idPath) {
      Objects.requireNonNull(idPath, "id");
      Preconditions.checkArgument(!idPath.members().isEmpty(), "Path %s does not have any members", idPath);
      this.idPath = idPath;
    }

    @Override
    public Result analyze(Expression filter) {
      Objects.requireNonNull(filter, "filter");
      if (!(filter instanceof Call)) {
        return NOT_OPTIMIZABLE;
      }

      final Call predicate = (Call) filter;
      // require "equal" or "in" operators
      if (!(predicate.operator() == Operators.EQUAL || predicate.operator() == Operators.IN)) {
        return NOT_OPTIMIZABLE;
      }

      final List<Expression> args = predicate.arguments();
      Preconditions.checkArgument(args.size() == 2, "Expected size 2 but got %s for %s",
              args.size(), predicate);


      if (!(args.get(0) instanceof Path && args.get(1) instanceof Constant)) {
        // second argument should be a constant
        return NOT_OPTIMIZABLE;
      }

      final Path path = Visitors.toPath(predicate.arguments().get(0));

      if (!idPath.equals(path)) {
        return NOT_OPTIMIZABLE;
      }

      // extract values
      List<?> values = Visitors.toConstant(predicate.arguments().get(1)).values();
      return new Result() {
        @Override
        public boolean isOptimizable() {
          return true;
        }

        @Override
        public List<?> values() {
          return values;
        }
      };
    }
  }

  /**
   * Result when optimization is not possible
   */
  private static final Result NOT_OPTIMIZABLE = new Result() {
    @Override
    public boolean isOptimizable() {
      return false;
    }

    @Override
    public List<?> values() {
      throw new UnsupportedOperationException("Expression can not be optimized for key lookup." +
              " Did you check isOptimizable() method ? ");
    }
  };

}
