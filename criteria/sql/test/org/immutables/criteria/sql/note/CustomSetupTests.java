package org.immutables.criteria.sql.note;

import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;

public class CustomSetupTests extends AbstractNoteRepositoryTests {
  @BeforeEach
  public void onStart() throws Exception {
    final DataSource datasource = getDataSource();
    migrate(datasource);
    repository = new NoteRepository(CustomNoteSetup.backend(datasource));
  }
}
