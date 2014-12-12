package org.immutables.value.processor.meta;

import com.google.common.collect.Ordering;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.immutables.value.Value.Immutable.ImplementationVisibility;

/**
 * Type visibility interpretation. Treat protected as package.
 */
public enum TypeVisibility {
  PRIVATE, PACKAGE, PUBLIC;

  public boolean isPublic() {
    return this == PUBLIC;
  }

  public boolean isPrivate() {
    return this == PRIVATE;
  }

  public TypeVisibility forImplementation(ImplementationVisibility visibility) {
    switch (visibility) {
    case PACKAGE:
      return PACKAGE;
    case PRIVATE:
      return PRIVATE;
    case PUBLIC:
      return PUBLIC;
    case SAME:
    default:
      return this;
    }
  }

  public TypeVisibility max(TypeVisibility visibility) {
    return Ordering.natural().max(this, visibility);
  }

  public static TypeVisibility of(Element element) {
    if (element.getModifiers().contains(Modifier.PUBLIC)) {
      return PUBLIC;
    }
    if (element.getModifiers().contains(Modifier.PRIVATE)) {
      return PRIVATE;
    }
    return PACKAGE;
  }

  public boolean isMoreRestrictiveThan(TypeVisibility visibility) {
    return this.compareTo(visibility) < 0;
  }
}
