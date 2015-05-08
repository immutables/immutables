package org.immutables.generator;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map.Entry;

public class SourceTypes {
  private SourceTypes() {}

  public static Entry<String, List<String>> extract(CharSequence returnTypeString) {
    StringBuilder typeName = new StringBuilder();
    StringBuilder typeArgument = new StringBuilder();
    List<String> typeArguments = Lists.newArrayList();
    int anglesOpened = 0;
    chars: for (int i = 0; i < returnTypeString.length(); i++) {
      char c = returnTypeString.charAt(i);
      switch (c) {
      case '<':
        if (++anglesOpened > 1) {
          typeArgument.append(c);
        }
        break;
      case '>':
        if (--anglesOpened > 0) {
          typeArgument.append(c);
        } else {
          break chars;
        }
        break;
      case ',':
        if (anglesOpened == 1) {
          typeArguments.add(typeArgument.toString());
          typeArgument = new StringBuilder();
        } else {
          typeArgument.append(c);
        }
        break;
      case ' ':// not sure about this one
        if (anglesOpened > 1) {
          typeArgument.append(c);
        }
        break;
      default:
        if (anglesOpened == 0) {
          typeName.append(c);
        } else {
          typeArgument.append(c);
        }
      }
    }
    String lastArgument = typeArgument.toString();
    if (!lastArgument.isEmpty()) {
      typeArguments.add(lastArgument);
    }
    return Maps.immutableEntry(typeName.toString(), typeArguments);
  }

  public static String stringify(Entry<String, ? extends List<String>> types) {
    if (types.getValue().isEmpty()) {
      return types.getKey();
    }
    return types.getKey() + "<" + Joiner.on(", ").join(types.getValue()) + ">";
  }
}
