package org.immutables.generate.silly;

import simple.GetterAnnotation;
import simple.GetterAnnotation.InnerAnnotation;
import com.google.common.base.Optional;
import java.lang.annotation.RetentionPolicy;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.immutables.annotation.GenerateAuxiliary;
import org.immutables.annotation.GenerateGetters;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateModifiable;

@GenerateImmutable
@GenerateGetters
@GenerateModifiable
public interface GetterEncloser {
  Optional<Integer> optional();

  @GenerateImmutable
  @GenerateGetters
  @GenerateModifiable
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
    @GenerateAuxiliary
    boolean ef();
  }
}
