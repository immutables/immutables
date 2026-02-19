/*
 * Copyright 2022 Immutables Authors and Contributors
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
package org.immutables.criteria.sql.commands;

import org.immutables.criteria.sql.compiler.SqlConstantExpression;
import org.immutables.criteria.sql.conversion.TypeConverters;
import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface SqlCommand {
  Publisher<?> execute();

  /**
   * Convert from properties encoded in {@code }SqlConstantExpression} to the underlying database
   * for use by {@code FluentStatment}
   *
   * @param properties the properties to convert
   * @return A new map with keys and types mapped to the underlying database columns and types
   */
  default Map<String, Object> toParameters(final Map<String, SqlConstantExpression> properties) {
    final Map<String, Object> parameters = new HashMap<>();
    for (final SqlConstantExpression property : properties.values()) {
      // Conversion of lists to target type lists - used for IN()
      if (property.value() instanceof List) {
        final List<?> v = (List) property.value();
        final List<?> converted = v.stream()
            .map(f -> TypeConverters.convert(f.getClass(), property.target().mapping().type(), f))
            .collect(Collectors.toList());
        parameters.put(property.sql(), converted);
      } else {
        parameters.put(property.sql(),
            TypeConverters.convert(property.value().getClass(),
                property.target().mapping().type(),
                property.value()));
      }
    }
    return parameters;
  }
}
