/*
 * Copyright 2019 Immutables Authors and Contributors
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

package org.immutables.criteria.matcher;

import java.util.Objects;

/**
 * Private interface which exposes context of a matcher / criteria. Context is similar to "state".
 * Used as private API (not visible in regular API).
 *
 * <p>Each matcher call or lambda expression creates new context. That context holds current
 * {@link org.immutables.criteria.expression.Query} or {@link org.immutables.criteria.expression.Expression}
 * used at runtime for translation to native queries or visiting
 * <a href="https://en.wikipedia.org/wiki/Abstract_syntax_tree">AST</a>.
 *
 * @see org.immutables.criteria.Criterion
 */
public interface ContextHolder {

  /**
   * Expose current context of the matcher. This method is used to extract and combine different
   * expressions (matchers).
   */
  CriteriaContext context();

  /**
   * Template implementation of {@link ContextHolder} can be instantiated by subclasses in a couple of lines.
   */
  abstract class AbstractHolder implements ContextHolder {
    private final CriteriaContext context;

    protected AbstractHolder(CriteriaContext context) {
      this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public CriteriaContext context() {
      return context;
    }
  }

}
