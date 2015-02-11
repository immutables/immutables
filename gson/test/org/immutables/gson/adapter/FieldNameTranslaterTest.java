package org.immutables.gson.adapter;

import static org.immutables.check.Checkers.*;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

public class FieldNameTranslaterTest {
  final Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
      .create();

  final FieldNamingTranslator translator = new FieldNamingTranslator(gson);

  @Test
  public void applySerializedName() {
    check(translator.translateName(Object.class, Object.class, "toString", "")).is("to-string");
  }

  @Test
  public void overridenSerializedName() {
    check(translator.translateName(Object.class, Object.class, "toString", "TO_STRING")).is("TO_STRING");
  }

  @Test
  public void noConfig() {
    check(new FieldNamingTranslator(new Gson()).translateName(Object.class, Object.class, "toString", ""))
        .is("toString");
  }
}
