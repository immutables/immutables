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
package org.immutables.criteria.sql.reflection;

import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.reflect.ClassScanner;
import org.immutables.criteria.reflect.MemberExtractor;
import org.immutables.criteria.sql.SQL;
import org.immutables.criteria.sql.SQLException;
import org.immutables.criteria.sql.conversion.ColumnFetchers;
import org.immutables.criteria.sql.conversion.ColumnMapping;
import org.immutables.criteria.sql.conversion.ImmutableColumnMapping;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SQLTypeMetadata {
    private static final Predicate<Method> MAYBE_GETTER = method -> Modifier.isPublic(method.getModifiers())
            && !Modifier.isStatic(method.getModifiers())
            && method.getReturnType() != Void.class
            && method.getParameterCount() == 0;
    static final Predicate<Member> MAYBE_PERSISTED = t -> {
        if (t instanceof Method) {
            return MAYBE_GETTER.test((Method) t);
        }
        return false;
    };
    private static final Predicate<Method> BOOLEAN_GETTER = method -> MAYBE_GETTER.test(method)
            && method.getName().startsWith("is")
            && method.getName().length() > "is".length()
            && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class);
    private static final Predicate<Method> GENERIC_GETTER = method -> MAYBE_GETTER.test(method)
            && method.getName().startsWith("get")
            && method.getName().length() > "get".length();
    static final Predicate<Method> IS_GETTER = GENERIC_GETTER.or(BOOLEAN_GETTER);
    private final String table;
    private final Class type;
    private final List<Member> members;
    private final List<SQLPropertyMetadata> metadata;

    private final KeyExtractor extractor;
    private final Map<String, SQLPropertyMetadata> columns = new HashMap<>();
    private final Map<String, SQLPropertyMetadata> properties = new HashMap<>();

    private SQLTypeMetadata(final Class<?> clazz) {
        type = clazz;
        table = SQLContainerNaming.SQL.name(clazz);
        extractor = KeyExtractor.defaultFactory().create(clazz);
        members = computePersistedMembers(clazz);
        metadata = computePropertyMetadata(members);
        metadata.forEach(m -> {
            properties.put(m.name(), m);
            columns.put(m.mapping().name(), m);
        });
    }

    public static SQLTypeMetadata of(final Class<?> type) {
        return new SQLTypeMetadata(type);
    }

    private static List<Member> computePersistedMembers(@Nonnull final Class<?> type) {
        return ClassScanner.of(type)
                .stream()
                .filter(m -> MAYBE_PERSISTED.test(m))
                .collect(Collectors.toList());
    }

    private static List<SQLPropertyMetadata> computePropertyMetadata(final List<Member> members) {
        return members.stream()
                .map(m -> {
                    return ImmutableSQLPropertyMetadata.builder()
                            .name(m instanceof Field ? computePropertyName((Field) m) : computePropertyName((Method) m))
                            .type(m instanceof Field ? ((Field) m).getType() : ((Method) m).getReturnType())
                            .mapping(computeColumnMapping(m))
                            .extractor(v -> MemberExtractor.ofReflection().extract(m, v))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static ColumnMapping computeColumnMapping(final Member m) {
        if (m instanceof Field) {
            final Field field = (Field) m;
            final SQL.Column annotation = field.getAnnotation(SQL.Column.class);
            final Class<?> type = annotation != null && annotation.type() != null
                    ? annotation.type()
                    : field.getType();
            return ImmutableColumnMapping.builder()
                    .type(type)
                    .fetcher(ColumnFetchers.get(type))
                    .name(annotation != null && annotation.name().length() > 0
                            ? annotation.name()
                            : computeColumnName(field))
                    .build();
        } else if (m instanceof Method) {
            final Method method = (Method) m;
            final SQL.Column annotation = method.getAnnotation(SQL.Column.class);
            final Class<?> type = annotation != null && annotation.type() != null
                    ? annotation.type()
                    : method.getReturnType();
            return ImmutableColumnMapping.builder()
                    .type(type)
                    .fetcher(ColumnFetchers.get(type))
                    .name(annotation != null && annotation.name().length() > 0
                            ? annotation.name()
                            : computeColumnName(method))
                    .build();
        }
        throw new SQLException("Unable to determine column mapping for member: " + m);
    }

    private static String computePropertyName(final Field field) {
        return field.getName();
    }

    private static String computePropertyName(final Method method) {
        // TODO: String get/is prefix etc.
        return method.getName();
    }

    private static String computeColumnName(final Field field) {
        return field.getName().toLowerCase(Locale.ROOT);
    }

    private static String computeColumnName(final Method method) {
        // TODO: String get/is prefix etc.
        return method.getName().toLowerCase(Locale.ROOT);
    }

    public Class type() {
        return type;
    }

    public String table() {
        return table;
    }

    public KeyExtractor key() {
        return extractor;
    }

    public Map<String, SQLPropertyMetadata> properties() {
        return properties;
    }

    public Map<String, SQLPropertyMetadata> columns() {
        return columns;
    }
}
