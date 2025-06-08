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
package org.immutables.value.processor.encode;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.encode.Code.Term;

@Immutable
public abstract class EncodingInfo {
  abstract String name();

  @Auxiliary
  abstract Set<String> imports();

  @Auxiliary
  abstract Type.Parameters typeParameters();

  @Auxiliary
  abstract Type.Factory typeFactory();

  @Auxiliary
  abstract List<EncodedElement> element();

  static class Builder extends ImmutableEncodingInfo.Builder {}

  public boolean hasValueOrVirtualFields() {
    if (impl().isVirtual()) {
      return true;
    }
    for (EncodedElement e : element()) {
      if (e.isValueField()) {
        return true;
      }
    }
    return false;
  }

  @Derived
  @Auxiliary
  public @Nullable EncodedElement builderCopy() {
    for (EncodedElement e : element()) {
      if (e.isBuilderCopy()) {
        return e;
      }
    }
    return null;
  }

  @Derived
  @Auxiliary
  public @Nullable EncodedElement isWasInit() {
    for (EncodedElement e : element()) {
      if (e.isWasInit()) {
        return e;
      }
    }
    return null;
  }

  @Derived
  @Auxiliary
  public EncodedElement from() {
    for (EncodedElement e : element()) {
      if (e.isFrom()) {
        return e;
      }
    }
    throw new IllegalStateException("Malformed encoding, missing FROM element");
  }

  @Derived
  @Auxiliary
  public EncodedElement impl() {
    for (EncodedElement e : element()) {
      if (e.isImplField()) {
        return e;
      }
    }
    throw new IllegalStateException("Malformed encoding, missing IMPL element");
  }

  @Derived
  @Auxiliary
  public EncodedElement build() {
    for (EncodedElement e : element()) {
      if (e.isBuild()) {
        return e;
      }
    }
    throw new IllegalStateException("Malformed encoding, missing BUILD element");
  }

  @Derived
  @Auxiliary
  public EncodedElement equals() {
    for (EncodedElement e : element()) {
      if (e.isEquals()) {
        return e;
      }
    }
    throw new IllegalStateException("Malformed encoding, missing EQUALS element");
  }

  @Derived
  @Auxiliary
  public EncodedElement hash() {
    for (EncodedElement e : element()) {
      if (e.isHashCode()) {
        return e;
      }
    }
    throw new IllegalStateException("Malformed encoding, missing HASH_CODE element");
  }

  @Derived
  @Auxiliary
  public EncodedElement string() {
    for (EncodedElement e : element()) {
      if (e.isToString()) {
        return e;
      }
    }
    throw new IllegalStateException("Malformed encoding, missing TO_STRING element");
  }

  @Derived
  @Auxiliary
  ImmutableSet<String> crossReferencedMethods() {
    Set<String> referenced = new HashSet<>();
    for (EncodedElement e : element()) {
      for (Term t : e.code()) {
        if (t.isBinding()) {
          Code.Binding b = (Code.Binding) t;
          if (b.isMethod()) {
            referenced.add(b.identifier());
          }
        }
      }
    }
    return ImmutableSet.copyOf(referenced);
  }
}
