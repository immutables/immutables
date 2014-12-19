/*
   Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.common.repository;

import javax.annotation.concurrent.Immutable;

import org.bson.types.ObjectId;

import com.google.common.base.Objects;

/**
 * Represents ObjectId type from MongoDB. Usually when modelling documents there's no
 * real need to use special {@link Id} type and some other simple or special value types will
 * perfectly work. But if for some reason this type is expected.
 */
@Immutable
public final class Id {
  private final int time;
  private final int machine;
  private final int inc;

  private Id(int time, int machine, int inc) {
    this.time = time;
    this.machine = machine;
    this.inc = inc;
  }

  /**
   * Constructs {@link Id} instance with given values for time, machine, and increment.
   * @param time some time value
   * @param machine some machine and process value
   * @param inc some increment value
   * @return the id
   */
  public static Id of(int time, int machine, int inc) {
    return new Id(time, machine, inc);
  }

  /**
   * Construct {@link Id} instance by parsing supplied string.
   * @param string hexadecimal formatted string
   * @return id object that corresponds to
   */
  public static Id fromString(String string) {
    return fromObjectId(new ObjectId(string));
  }

  /**
   * Generate new Id, based on machine/preocess, current seconds and inrementing counter.
   * @return the id
   */
  public static Id generate() {
    return fromObjectId(new ObjectId());
  }

  private static Id fromObjectId(ObjectId objectId) {
    return of(
        objectId._time(),
        objectId._machine(),
        objectId._inc());
  }

  /**
   * Increment part of {@link Id}.
   * @return counter value
   */
  public int inc() {
    return inc;
  }

  /**
   * Machine part of {@link Id}.
   * @return combines machine/process into
   */
  public int machine() {
    return machine;
  }

  /**
   * @return time part of {@link Id}
   */
  public int time() {
    return time;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(time, machine, inc);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof Id) {
      Id id = (Id) other;
      return time == id.time
          && machine == id.machine
          && inc == id.inc;
    }
    return false;
  }

  /**
   * Returns {@link #fromString(String) parsable} representation of Id.
   * @return hexadecimal formatted string.
   */
  @Override
  public String toString() {
    return new ObjectId(time, machine, inc).toString();
  }
}
