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
package org.immutables.criteria.sql.note;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.*;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class AbstractTestBase {
  protected static DataSource getDataSource() {
    final JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");
    return ds;
  }

  protected static void migrate(final DataSource ds) throws SQLException {
    final Connection connection = ds.getConnection();
    RunScript.execute(connection, new InputStreamReader(AbstractTestBase.class.getResourceAsStream("notes.sql")));
  }

  protected static final List<Note> newEntities(final int count) {
    final List<Note> ret = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ret.add(newEntity());
    }
    return ret;
  }

  protected static final Note newEntity() {
    return ImmutableNote.builder()
        .id(UUID.randomUUID())
        .created(Instant.now())
        .message("Test note")
        .build();
  }

  protected static final Query newQuery(final UUID id) {
    try {
      final Path path = Path.ofMember(Note.class.getMethod("id"));
      final Constant value = Expressions.constant(id);
      final Query query = ImmutableQuery.builder()
          .entityClass(Note.class)
          .filter(Expressions.call(Operators.EQUAL, Arrays.asList(path, value)))
          .build();
      return query;
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  protected static final Criterion<Note> newCriterion(final UUID id) {
    return NoteCriteria.note.id.is(id);
  }
}
