/*
 * Copyright 2019 Immutables Authors and Contributors
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

package org.immutables.criteria.elasticsearch;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Elastic cluster version. Useful because depending on version one might generate different queries.
 */
class Version {

  private static final Pattern MAJOR_VERSION = Pattern.compile("^\\d+");

  final int major;
  final String full;

  private Version(String full) {
    this.full = Objects.requireNonNull(full, "full");;

    Matcher matcher = MAJOR_VERSION.matcher(full);
    Preconditions.checkArgument(matcher.lookingAt(), "Invalid version string: %s", full);
    this.major = Integer.parseInt(matcher.group());
  }

  static Version of(String full) {
    return new Version(full);
  }

}
