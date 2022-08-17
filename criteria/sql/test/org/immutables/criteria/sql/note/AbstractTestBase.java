package org.immutables.criteria.sql.note;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.h2.jdbcx.JdbcDataSource;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.*;

import javax.sql.DataSource;
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

    protected static void migrate(final DataSource ds) throws LiquibaseException, SQLException {
        final Connection connection = ds.getConnection();
        final Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        final Liquibase liquibase = new Liquibase("test-migrations.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.dropAll();
        liquibase.update("");
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
