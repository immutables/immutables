/*
   Copyright 2013-2018 Immutables Authors and Contributors

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
package org.immutables.mongo.types;

import java.util.Arrays;
import org.bson.types.ObjectId;

/**
 * Represents ObjectId type from MongoDB. Usually when modelling documents there's no
 * much need to use special {@link Id} type, other types will perfectly work. But to easily bind to
 * {@code ObjectID} when you need it, just use this class.
 * <p>
 * <em>
 * Internal data structure will be changed for version 3.x of Mongo Java Driver to adhere to
 * specification.
 * Use this class as opaque storage, i.e. not for reading and manipulation with individual
 * components.
 * In this version of class there are no separate fields, just a byte array, to further guarantee
 * this.
 * </em>
 */
public final class Id {
  private final byte[] data;

  private Id(byte[] data) {
    this.data = data;
  }

  /**
   * Construct {@link Id} instance by parsing supplied string. {@link #toString()} Could then
   * convert back to string representation.
   * @param string hexadecimal formatted string
   * @return id object that corresponds to
   */
  public static Id fromString(String string) {
    return from(new ObjectId(string));
  }

  public static Id from(byte[] data) {
    return new Id(data.clone());
  }

  private static Id from(ObjectId objectId) {
    return new Id(objectId.toByteArray());
  }

  public byte[] value() {
    return data.clone();
  }

  /**
   * Generate new Id, based on machine/process, current seconds and incrementing counter.
   * Should be treated as opaque holder though.
   * @return the id
   */
  public static Id generate() {
    return from(ObjectId.get());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof Id) {
      Id id = (Id) other;
      return Arrays.equals(data, id.data);
    }
    return false;
  }

  /**
   * Returns {@link #fromString(String) parsable} representation of Id.
   * @return hexadecimal formatted string.
   */
  @Override
  public String toString() {
    return new ObjectId(data).toString();
  }
}
