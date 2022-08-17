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

import org.immutables.criteria.sql.SQLBackend;
import org.immutables.criteria.sql.SQLSetup;
import org.immutables.criteria.sql.reflection.SQLTypeMetadata;

import javax.sql.DataSource;

public class NoteSetup {
    public static SQLBackend backend(final DataSource datasource) {
        return SQLBackend.of(setup(datasource));
    }

    public static SQLSetup setup(final DataSource datasource) {
        final SQLSetup setup = SQLSetup.of(datasource, SQLTypeMetadata.of(Note.class));
        return setup;
    }
}
