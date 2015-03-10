package org.immutables.gson.packg;

import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import static org.immutables.check.Checkers.*;
import com.google.gson.Gson;

public class PackgTest {
  final GsonAdaptersPackg adapterFactory = new GsonAdaptersPackg();
  final Gson gson = new Gson();

  @Test
  public void adaptersProvided() {
    check(adapterFactory.create(gson, TypeToken.get(A.class))).notNull();
    check(adapterFactory.create(gson, TypeToken.get(B.class))).notNull();
    check(adapterFactory.create(gson, TypeToken.get(B.C.class))).notNull();
  }
}
