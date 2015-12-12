package org.immutables.gson.adapter;

import com.google.gson.annotations.SerializedName;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters(fieldNamingStrategy = true)
public interface AlternateNamesStrategy {
  @SerializedName(value = "url", alternate = {"URL", "href"})
  String url();
}
