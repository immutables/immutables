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
package org.immutables.criteria.sql;

import org.immutables.criteria.sql.dialects.SQL92Dialect;
import org.immutables.criteria.sql.dialects.SqlDialect;
import org.immutables.criteria.sql.reflection.SqlTypeMetadata;
import org.immutables.value.Value;

import javax.sql.DataSource;

@Value.Immutable
public interface SqlSetup {
  static SqlSetup of(final DataSource datasource, final SqlTypeMetadata metadata) {

    return of(datasource, new SQL92Dialect(), metadata);
  }

  static SqlSetup of(final DataSource datasource, final SqlDialect dialect, final SqlTypeMetadata metadata) {
    return ImmutableSqlSetup.builder()
        .datasource(datasource)
        .dialect(dialect)
        .metadata(metadata)
        .build();
  }

  SqlTypeMetadata metadata();

  DataSource datasource();

  SqlDialect dialect();
}