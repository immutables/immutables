package org.immutables.fixture;

import com.google.common.base.Optional;
import java.lang.annotation.RetentionPolicy;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.immutables.value.Value;
import simple.GetterAnnotation;
import simple.GetterAnnotation.InnerAnnotation;

@Value.Immutable
@Value.Getters
// FIXME @Value.Modifiable
public interface GetterEncloser {
  Optional<Integer> optional();

  @Value.Immutable
  @Value.Getters
  // FIXME @Value.Modifiable
  public interface Getters {
    int ab();

    // to test annotation content copy on getter
    @POST
    @Path("/cd")
    String cd();

    // to test annotation content copy on getter
    @GetterAnnotation(policy = RetentionPolicy.CLASS,
        string = "\n\"",
        type = Object.class,
        value = { @InnerAnnotation, @InnerAnnotation })
    @Path("/ef")
    @Value.Auxiliary
    boolean ef();
  }
}
