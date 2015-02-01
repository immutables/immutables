/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.fixture;

import com.google.common.base.Optional;
import java.lang.annotation.RetentionPolicy;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.immutables.value.Value;
import simple.GetterAnnotation;
import simple.GetterAnnotation.InnerAnnotation;

@SuppressWarnings("deprecation")
@Value.Immutable
public interface GetterEncloser {
  Optional<Integer> optional();

  @Value.Immutable
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
