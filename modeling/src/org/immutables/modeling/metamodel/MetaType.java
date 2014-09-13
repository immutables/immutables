/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.modeling.metamodel;

import com.google.common.collect.FluentIterable;
import java.util.List;
import org.immutables.annotation.GenerateImmutable;

/**
 * The Class MetaType.
 */
@GenerateImmutable
public abstract class MetaType {
  public abstract String name();

  public abstract List<MetaTrait> traits();

  public List<MetaAttribute> attributes() {
    return FluentIterable.from(traits())
        .transformAndConcat(MetaTraitFunctions.attributes())
        .toList();
  }
}
