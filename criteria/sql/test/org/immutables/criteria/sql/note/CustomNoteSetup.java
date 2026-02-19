package org.immutables.criteria.sql.note;

import org.immutables.criteria.sql.SqlBackend;
import org.immutables.criteria.sql.SqlSetup;
import org.immutables.criteria.sql.conversion.RowMappers;
import org.immutables.criteria.sql.reflection.SqlTypeMetadata;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.UUID;

public class CustomNoteSetup {
  public static SqlBackend backend(final DataSource datasource) {
    return SqlBackend.of(setup(datasource));
  }
  public static SqlSetup setup(final DataSource datasource) {
    final SqlTypeMetadata metadata = SqlTypeMetadata.of(Note.class);
    final SqlSetup setup = SqlSetup.of(datasource, metadata);
    RowMappers.register(Note.class, (row) ->
      ImmutableNote.builder()
          .id(UUID.fromString(row.getString("id")))
          .created(Instant.ofEpochMilli(row.getLong("created_on")))
          .message(row.getString("message"))
          .build());
    return setup;
  }
}
