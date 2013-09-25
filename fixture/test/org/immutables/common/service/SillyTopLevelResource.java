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
package org.immutables.common.service;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.immutables.generate.silly.ImmutableSillyDumb;
import org.immutables.generate.silly.SillyAbstract;
import org.immutables.generate.silly.SillyDumb;
import org.immutables.generate.silly.SillySub1;
import org.immutables.generate.silly.SillySub3;
import static com.google.common.base.Preconditions.*;

@Path("/res")
public class SillyTopLevelResource {

  @Inject
  SillyAbstract injectedByGuice;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public SillyDumb post(SillySub3 input) {
    checkNotNull(input);
    checkState(injectedByGuice instanceof SillySub1);
    return ImmutableSillyDumb.builder()
        .c3(1)
        .build();
  }

  public static class ParamInjected {
    @Inject
    public UriInfo injectedByJersey;
  }

  @GET
  public String get(@BeanParam ParamInjected input) {
    return input.injectedByJersey + "";
  }
}
