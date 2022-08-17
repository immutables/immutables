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

import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoteRepositoryTests extends AbstractTestBase {
    protected NoteRepository repository;

    @BeforeEach
    public void onStart() throws Exception {
        final DataSource datasource = getDataSource();
        migrate(datasource);
        repository = new NoteRepository(NoteSetup.backend(datasource));
    }

    @Test
    public void testFindAllCount() {
        assertEquals(5, repository.findAll().count());
    }

    @Test
    public void testFindWithDistinctCount() {
        assertEquals(5, repository.findAll().select(NoteCriteria.note.id).distinct().count());
    }

    @Test
    public void testFindWithLimitCount() {
        // The limit applies to the result of the COUNT()
        assertEquals(5, repository.findAll().limit(3L).count());
    }

    @Test
    public void testFindWithLimitSetToZero() {
        // The limit applies to the result of the COUNT() and returns no results, so throws an exception
        assertThrows(SQLException.class, () -> repository.findAll().limit(0L).count());
    }

    @Test
    public void testFindWithOffsetCount() {
        // The offset applies to the result of the COUNT() which will result in no results being returned
        // then an exception being thrown
        assertThrows(SQLException.class, () -> repository.findAll().offset(3L).count());
    }

    @Test
    public void testFindAllFetch() {
        final List<Note> results = repository.findAll().fetch();
        assertEquals(5, results.size());
        assertTrue(results.stream().anyMatch(e ->
                        e.id().equals(UUID.fromString("7aae20e3-9144-40ba-86f0-c9de8f143269")) &&
                                e.created().toEpochMilli() == 1640995200000L &&
                                e.message().equals("Message #1")),
                "Missing item");
    }

    @Test
    public void testFindAllFetchWithLimit() {
        final List<Note> results = repository.findAll().limit(3).fetch();
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(e ->
                        e.id().equals(UUID.fromString("7aae20e3-9144-40ba-86f0-c9de8f143269")) &&
                                e.created().toEpochMilli() == 1640995200000L &&
                                e.message().equals("Message #1")),
                "Missing item");
    }

    @Test
    public void testFindAllFetchWithLimitSetToZero() {
        final List<Note> results = repository.findAll().limit(0).fetch();
        assertEquals(0, results.size());
    }

    @Test
    public void testFindAllFetchWithOffset() {
        final List<Note> results = repository.findAll().offset(3).fetch();
        assertEquals(2, results.size());
    }

    @Test
    public void testFindAllFetchWithLargeOffset() {
        final List<Note> results = repository.findAll().offset(300L).fetch();
        assertEquals(0, results.size());
    }

    @Test
    public void testIdCriteriaBasedFetch() {
        final Note results = repository.find(NoteCriteria.note
                .id.is(UUID.fromString("7aae20e3-9144-40ba-86f0-c9de8f143269"))).one();
        assertNotNull(results);
    }


    @Test
    public void testCreatedOnCriteriaBasedFetch() {
        final Note results = repository.find(NoteCriteria.note
                .created.atLeast(Instant.ofEpochMilli(1640995200000L))).one();
        assertNotNull(results);
    }


    @Test
    public void testCriteriaBasedMultiFetch() {
        final List<Note> results = repository.find(NoteCriteria.note
                        .id.in(UUID.fromString("7aae20e3-9144-40ba-86f0-c9de8f143269"),
                                UUID.fromString("75b90525-38be-41b9-b43d-df427a66893c")))
                .fetch();
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindOneFetchWithSingleOrdering() {
        final Note results = repository.findAll()
                .orderBy(NoteCriteria.note.created.asc())
                .limit(1)
                .one();
        assertEquals(UUID.fromString("c49371f3-8cda-4ddf-ad74-877ee6f67abe"), results.id());
        assertEquals(0L, results.created().toEpochMilli());
    }

    @Test
    public void testFindOneFetchWithMultipleOrdering() {
        final Note results = repository.findAll()
                .orderBy(NoteCriteria.note.id.asc(), NoteCriteria.note.created.asc())
                .limit(1)
                .one();
        assertEquals(UUID.fromString("578f932f-c972-4d3b-92b4-bf8fda9599f9"), results.id());
        assertEquals(1577836800000L, results.created().toEpochMilli());
    }

    @Test
    public void testInsertSingleValue() {
        final Note event = insertAndVerify(newEntity());
        assertEquals(1, repository.find(NoteCriteria.note.id.is(event.id())).count());
    }


    @Test
    public void testInsertMultipleValues() {
        final List<Note> data = insertAndVerify(newEntities(5));
        data.forEach(e -> assertEquals(1, repository.find(NoteCriteria.note.id.is(e.id())).count()));
    }

    @Test
    public void testDeleteWhereNotEmpty() {
        repository.delete(NoteCriteria.note.message.notEmpty());
        final long after = repository.findAll().count();
        assertEquals(0, after);
    }

    @Test
    public void testDeleteSingleUsingCriteria() {
        final long before = repository.findAll().count();

        // Insert new data and verify
        final Note data = insertAndVerify(newEntity());

        // Delete and verify state is correct
        repository.delete(NoteCriteria.note.id.is(data.id()));
        final long after = repository.findAll().count();
        assertEquals(before, after);
    }

    @Test
    public void testDeleteMultipleUsingCriteria() {
        final long before = repository.findAll().count();

        // Insert new data and verify
        final List<Note> data = insertAndVerify(newEntities(3));

        // Delete and verify state is correct
        repository.delete(NoteCriteria.note.id.in(data.stream().map(e -> e.id()).collect(Collectors.toList())));
        final long after = repository.findAll().count();
        assertEquals(before, after);
    }

    @Test
    public void testUpdateSingleEntityUsingCriteria() {
        final String message = "I'm a jolly little update";
        final NoteCriteria criteria = NoteCriteria.note;
        final Note target = repository.findAll().fetch().get(0);
        final WriteResult result = repository.update(criteria.id.is(target.id()))
                .set(criteria.message, message)
                .execute();
        assertNotNull(result);
        assertTrue(result.updatedCount().isPresent());
        assertEquals(1, result.updatedCount().getAsLong());

        final Note updated = repository.find(NoteCriteria.note.id.is(target.id())).one();
        assertNotNull(updated);
        assertEquals(message, updated.message());
    }

    @Test
    public void testUpdateMultipleEntitiesUsingCriteria() {
        final String message = "I'm a jolly little update";
        final NoteCriteria criteria = NoteCriteria.note;
        final Note target = repository.findAll().fetch().get(0);
        final WriteResult result = repository.update(criteria.id.isNot(UUID.randomUUID()))
                .set(criteria.message, message)
                .execute();
        assertNotNull(result);
        assertTrue(result.updatedCount().isPresent());
        assertEquals(5, result.updatedCount().getAsLong());

        repository.findAll().fetch().forEach(e -> {
            assertEquals(message, e.message());
        });
    }

    @Test
    public void testUpdateSingleEntityUsingObject() {
        final String message = "I'm a jolly little update";
        final Note target = repository.findAll().fetch().get(0);
        final ImmutableNote update = ImmutableNote.builder()
                .from(target)
                .message(message)
                .build();
        final WriteResult result = repository.update(update);
        assertNotNull(result);
        assertTrue(result.updatedCount().isPresent());
        assertEquals(1, result.updatedCount().getAsLong());

        final Note updated = repository.find(NoteCriteria.note.id.is(target.id())).one();
        assertTrue(repository.find(NoteCriteria.note.id.is(target.id())).exists(), "Unable to find note");
        assertNotNull(updated);
        assertEquals(message, updated.message());
    }

    @Test
    public void testUpdateMultipleEntitiesUsingObject() {
        final String message = "I'm a jolly little update";
        final List<Note> targets = repository.findAll().limit(3).fetch();
        final List<Note> updates = targets.stream()
                .map(e -> ImmutableNote.builder()
                        .from(e)
                        .message(message)
                        .build()).collect(Collectors.toList());
        final WriteResult result = repository.updateAll(updates);
        assertNotNull(result);
        assertTrue(result.updatedCount().isPresent());
        assertEquals(3, result.updatedCount().getAsLong());

        targets.forEach(t -> {
            final Note updated = repository.find(NoteCriteria.note.id.is(t.id())).one();
            assertTrue(repository.find(NoteCriteria.note.id.is(t.id())).exists(), "Unable to find note");
            assertNotNull(updated);
            assertEquals(message, updated.message());
        });
    }

    protected final List<Note> insertAndVerify(final List<Note> entities) {
        final long before = repository.findAll().count();

        final WriteResult result = repository.insertAll(entities);
        assertNotNull(result);
        assertTrue(result.insertedCount().isPresent());
        assertTrue(result.deletedCount().isPresent());
        assertTrue(result.updatedCount().isPresent());
        assertEquals(entities.size(), result.insertedCount().getAsLong());
        assertEquals(0, result.deletedCount().getAsLong());
        assertEquals(0, result.updatedCount().getAsLong());

        // Ensure post-insert state is correct
        final long after = repository.findAll().count();
        assertEquals(before + entities.size(), after);

        // Ensure that we can find the entities
        entities.forEach(e -> assertEquals(1, repository.find(NoteCriteria.note.id.is(e.id())).count()));

        return entities;
    }

    protected final Note insertAndVerify(final Note event) {
        // Ensure database state is correct
        final long before = repository.findAll().count();

        // Ensure that the insert worked
        final WriteResult result = repository.insert(event);
        assertNotNull(result);
        assertTrue(result.insertedCount().isPresent());
        assertTrue(result.deletedCount().isPresent());
        assertTrue(result.updatedCount().isPresent());
        assertEquals(1, result.insertedCount().getAsLong());
        assertEquals(0, result.deletedCount().getAsLong());
        assertEquals(0, result.updatedCount().getAsLong());

        // Ensure post-insert state is correct
        final long after = repository.findAll().count();
        assertEquals(before + 1, after);

        // Ensure that we can find the entity
        assertEquals(1, repository.find(NoteCriteria.note.id.is(event.id())).count());

        return event;
    }
}
