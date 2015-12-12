package org.immutables.gson.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class AlternateNameTest {
  private final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersAlternateNames())
      .create();

  @Test
  public void alternateNames() {
    check(gson.fromJson("{\"url\":\"a\"}", AlternateNames.class).url()).is("a");
    check(gson.fromJson("{\"URL\":\"b\"}", AlternateNames.class).url()).is("b");
    check(gson.fromJson("{\"href\":\"c\"}", AlternateNames.class).url()).is("c");
  }
}
