/*
   Copyright 2015 Immutables Authors and Contributors

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

import java.nio.charset.StandardCharsets;
import com.google.common.io.Resources;
import java.net.URL;
import java.util.Enumeration;
import java.io.IOException;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;

public final class CustomImmutableAnnotations {
  private CustomImmutableAnnotations() {}

  private static final String ANNOTATIONS_OPTION_KEY = "META-INF/annotations/org.immutables.value.immutable";

  private static final Splitter ADDITIONAL_ANNOTATION_SPLITTER = Splitter.on("\n")
      .omitEmptyStrings()
      .trimResults();

  private static final ImmutableSet<String> ANNOTATIONS = findExtensionAnnotations();

  public static ImmutableSet<String> annotations() {
    return ANNOTATIONS;
  }

  private static ImmutableSet<String> findExtensionAnnotations() {
    List<String> annotations = Lists.newArrayList();

    ClassLoader classLoader = CustomImmutableAnnotations.class.getClassLoader();
    try {
      Enumeration<URL> resources = classLoader.getResources(ANNOTATIONS_OPTION_KEY);
      while (resources.hasMoreElements()) {
        URL nextElement = resources.nextElement();
        String lines = Resources.toString(nextElement, StandardCharsets.UTF_8);
        annotations.addAll(ADDITIONAL_ANNOTATION_SPLITTER.splitToList(lines));
      }
    } catch (IOException cannotReadFromClasspath) {
      // we ignore this as we did or best effort
      // and there are no plans to halt whole compilation
    }
    return FluentIterable.from(annotations).toSet();
  }
}
