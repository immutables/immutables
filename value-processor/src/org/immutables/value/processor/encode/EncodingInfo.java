package org.immutables.value.processor.encode;

import javax.annotation.Nullable;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
  ImmutableSet<String> crossReferencedMembers() {
    Set<String> referenced = new HashSet<>();
    for (EncodedElement e : element()) {
      for (Term t : e.code()) {
        if (t.isBinding()) {
          String identifier = ((Code.Binding) t).identifier();
          boolean mayBeMethod = Ascii.isLowerCase(identifier.charAt(0));
          if (mayBeMethod) {
            referenced.add(identifier);
          }
        }
      }
    }
    return ImmutableSet.copyOf(referenced);
  }
}
