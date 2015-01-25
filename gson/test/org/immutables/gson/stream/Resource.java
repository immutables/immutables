package org.immutables.gson.stream;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class Resource {
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<String> get(Set<Integer> integers) {
    return Arrays.asList("a", "b", "c", integers.toString());
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.TEXT_PLAIN)
  public List<String> getGsonPure(Set<Integer> integers) {
    return Arrays.asList("x", "y", integers.toString());
  }
}
