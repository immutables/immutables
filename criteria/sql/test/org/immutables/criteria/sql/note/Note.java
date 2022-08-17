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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.sql.SQL;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.UUID;

@Criteria
@Criteria.Repository
@Value.Immutable
@SQL.Table("notes")
@JsonDeserialize(as = ImmutableNote.class)
@JsonSerialize(as = ImmutableNote.class)
public interface Note {
    String message();

    @SQL.Column(type = long.class, name = "created_on")
    Instant created();

    @Criteria.Id
    @SQL.Column(type = String.class)
    UUID id();
}

