/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.gson.stream;

import org.immutables.gson.adapter.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

  @Path("/objectBooleanInMapTest")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public MapTest getObjectBooleanInMapTest() {

    return ImmutableMapTest.builder()
            .putMapObject("object", true)
            .build();
  }

  @Path("/objectDoubleInMapTest")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public MapTest getObjectDoubleInMapTest() {

    return ImmutableMapTest.builder()
            .putMapObject("object", 5.0)
            .build();
  }

  @Path("/booleanInMapTest")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public MapTest getBooleanInMapTest() {

    return ImmutableMapTest.builder()
            .putMapBoolean("boolean", true)
            .build();
  }

  @Path("/doubleInMapTest")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public MapTest getDoubleInMapTest() {

    return ImmutableMapTest.builder()
            .putMapDouble("double", 5.0)
            .build();
  }
}
