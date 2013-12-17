/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.common.logging;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * String formatting routines.
 */
public final class Stringification {

  private static final String EMPTY_STRING = "";
  private static final String NULL_STRING = "null";

  private Stringification() {
  }

  public static String stringify(Object object, String nullString) {
    if (object == null) {
      return nullString;
    }

    if (object instanceof String) {
      return (String) object;
    }

    if (object.getClass().isArray()) {
      String arrayToString = Arrays.deepToString(new Object[] { object });
      return arrayToString.substring(1, arrayToString.length() - 1);
    }

    String text = object.toString();
    return text != null ? text : nullString;
  }

  public static String stringifyParts(Object... messageParts) {
    if (messageParts == null) {
      return NULL_STRING;
    }

    if (messageParts.length == 0) {
      return EMPTY_STRING;
    }

    if (messageParts.length == 1) {
      return stringify(messageParts[0], NULL_STRING);
    }

    String message = null;
    Object[] inserts = messageParts;

    if (messageParts[0] instanceof String) {
      message = (String) messageParts[0];
      inserts = new Object[messageParts.length - 1];
      System.arraycopy(messageParts, 1, inserts, 0, inserts.length);
    }

    StringBuilder builder = new StringBuilder();
    appendMessage(builder, null, message, inserts);
    return builder.toString();
  }

  private static Object[] processInserts(Object[] parameters, boolean keepFormatSpecific) {

    for (int i = 0; i < parameters.length; i++) {
      Object o = parameters[i];

      if (keepFormatSpecific) {
        if (o instanceof Number
            || o instanceof String
            || o instanceof Calendar
            || o instanceof Date) {
          continue;
        }
      }

      parameters[i] = stringify(o, NULL_STRING);
    }
    return parameters;
  }

  public static void appendMessage(
      Appendable appendable,
      @Nullable Locale locale,
      @Nullable String messagePattern,
      Object... messageParts) {

    if (messagePattern != null) {
      try {
        String formatPattern = messagePattern;
        boolean usesMessageFormatPattern = false;

        if (formatPattern.contains("{}")) {
          int placeholderIndex = 0;
          while (formatPattern.indexOf("{}") >= 0) {
            formatPattern = formatPattern.replaceFirst("\\{\\}", "{" + (placeholderIndex++) + "}");
          }
          formatPattern = formatPattern.replace("{}", EMPTY_STRING);
          usesMessageFormatPattern = true;
        }

        usesMessageFormatPattern = usesMessageFormatPattern
            || formatPattern.contains("{0")
            || formatPattern.contains("{1");

        if (usesMessageFormatPattern) {

          boolean patternUsesSubformatSpecifiers =
              formatPattern.contains(",number")
                  || formatPattern.contains(",date")
                  || formatPattern.contains(",time");

          MessageFormat formatter = (locale != null)
              ? new MessageFormat(formatPattern, locale)
              : new MessageFormat(formatPattern);

          String formattedMessage = formatter.format(processInserts(messageParts, patternUsesSubformatSpecifiers));

          appendable.append(formattedMessage);
          return;
        }

        if (formatPattern.indexOf('%') >= 0) {
          @SuppressWarnings("resource")
          Formatter formatter = locale != null
              ? new Formatter(locale)
              : new Formatter();

          formatter.format(formatPattern, processInserts(messageParts, true));
          appendable.append(formatter.toString());
          return;
        }

      } catch (Exception e) {
        appendThrowableMark(appendable, e);
      }
    }

    justAppendSequentially(appendable, messagePattern, messageParts);
  }

  private static Appendable justAppendSequentially(
      Appendable builder,
      @Nullable String message,
      Object[] inserts) {

    try {
      if (message != null) {
        builder.append(message);
      }

      for (Object part : inserts) {
        if (part != null) {
          builder.append(stringify(part, EMPTY_STRING));
        }
      }
    } catch (Throwable th) {
      appendThrowableMark(builder, th);
    }

    return builder;
  }

  private static void appendThrowableMark(Appendable builder, Throwable e) {
    try {
      builder.append('!' + e.getClass().getSimpleName() + ": " + e.getMessage() + "!");
    } catch (IOException ex) {
      intendedSwallow();
    }
  }

  private static void intendedSwallow() {
  }
}
