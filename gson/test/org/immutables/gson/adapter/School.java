/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.gson.adapter;

import java.util.Arrays;
import java.util.List;
import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Gson.TypeAdapters
@Value.Immutable
@Value.Enclosing
@Value.Style(
    defaults = @Value.Immutable(builder = false),
    overshadowImplementation = true,
    strictBuilder = true,
    visibility = ImplementationVisibility.PACKAGE)
public abstract class School {
  public abstract List<Entry<Classroom, Student<String>>> students();

  public static class Builder extends ImmutableSchool.Builder {}

  @Value.Immutable
  public interface Classroom extends ImmutableSchool.WithClassroom {
    String location();

    class Builder extends ImmutableSchool.Classroom.Builder {}
  }

  @Value.Immutable
  public interface Student<T> extends ImmutableSchool.WithStudent<T> {
    T name();

    class Builder<T> extends ImmutableSchool.Student.Builder<T> {}
  }

  @Value.Immutable
  public static abstract class Entry<K, V> {
    @Value.Parameter
    public abstract K key();

    @Value.Parameter
    public abstract List<V> value();

    public static <K, V> Entry<K, V> of(K key, Iterable<? extends V> value) {
      return ImmutableSchool.Entry.of(key, value);
    }
  }

  public static School create() {
    return new School.Builder()
        .addStudents(Entry.of(
            new School.Classroom.Builder()
                .location("Boring Heights")
                .build(),
            Arrays.asList(
                new School.Student.Builder<String>()
                    .name("Java Doe")
                    .build(),
                new School.Student.Builder<String>()
                    .name("Sunny Day")
                    .build())))
        .build();
  }
}
