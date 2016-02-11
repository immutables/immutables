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
package org.immutables.value.processor.meta;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.generator.Naming;

interface Depluralizer {
  String depluralize(String name);

  Depluralizer NONE = new Depluralizer() {
    @Override
    public String depluralize(String name) {
      return name;
    }
  };

  class DepluralizerWithExceptions implements Depluralizer {
    private final ImmutableMap<String, String> exceptions;

    DepluralizerWithExceptions(String[] exceptions) {
      Map<String, String> map = Maps.newHashMapWithExpectedSize(exceptions.length);
      Splitter splitter = Splitter.on(':');
      for (String s : exceptions) {
        List<String> parts = splitter.splitToList(s.toLowerCase());
        if (parts.size() == 1) {
          // simple no-depluratization exception
          map.put(parts.get(0), parts.get(0));
        } else if (parts.size() == 2) {
          // singular, then plural, so mapping plural->singular
          map.put(parts.get(1), parts.get(0));
        }
      }
      this.exceptions = ImmutableMap.copyOf(map);
    }

    @Override
    public String depluralize(String name) {
      LinkedList<String> parts =
          Lists.newLinkedList(splitCamelCase(name));

      String plural = parts.removeLast();
      @Nullable String singular = exceptions.get(plural);
      if (singular != null) {
        parts.addLast(singular);
        return joinCamelCase(parts);
      }

      String detected = NAMING_PLURAL.detect(name);
      return !detected.isEmpty()
          ? detected
          : name;
    }

    private static String joinCamelCase(Iterable<String> parts) {
      return CaseFormat.LOWER_UNDERSCORE.to(
          CaseFormat.LOWER_CAMEL, JOINER_UNDERSCORE.join(parts));
    }

    private static Iterable<String> splitCamelCase(String name) {
      return SPLITTER_UNDERSCORE.split(
          CaseFormat.LOWER_CAMEL.to(
              CaseFormat.LOWER_UNDERSCORE, name));
    }

    private static final Naming NAMING_PLURAL = Naming.from("*s");
    private static final Splitter SPLITTER_UNDERSCORE = Splitter.on('_')
        .omitEmptyStrings()
        .trimResults();
    private static final Joiner JOINER_UNDERSCORE = Joiner.on('_');
  }
}
