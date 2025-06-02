package org.immutables.criteria.sql.note;

import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;

public class GeneratedSetupTests extends AbstractNoteRepositoryTests {

  @BeforeEach
  public void onStart() throws Exception {
    final DataSource datasource = getDataSource();
    migrate(datasource);
    repository = new NoteRepository(SqlNoteSetup.backend(datasource));
  }
}
