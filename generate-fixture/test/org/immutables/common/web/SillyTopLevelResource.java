package org.immutables.common.web;

import com.google.common.base.Preconditions;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.immutables.generate.silly.ImmutableSillyDumb;
import org.immutables.generate.silly.SillyAbstract;
import org.immutables.generate.silly.SillyDumb;
import org.immutables.generate.silly.SillySub1;
import org.immutables.generate.silly.SillySub3;

@Path("/res")
public class SillyTopLevelResource {

  @Inject
  SillyAbstract abstractSilly;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public SillyDumb invoke(SillySub3 input) {
    Preconditions.checkArgument(input != null);
    Preconditions.checkState(abstractSilly instanceof SillySub1);
    return ImmutableSillyDumb.builder()
        .c3(1)
        .build();
  }
}
