package org.immutables.gson.adapter;

import com.google.gson.annotations.SerializedName;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface AlternateNames {
  @SerializedName(value = "url", alternate = {"URL", "href"})
  String url();
}
