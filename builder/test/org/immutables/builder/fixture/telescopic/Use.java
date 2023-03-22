/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.builder.fixture.telescopic;

public class Use {
  public static void main(String... args) {
    ImmutableMoji.ABuildStage b = ImmutableMoji.builder();
    ImmutableMoji.BBuildStage a = b.a(1);
    ImmutableMoji.BuildFinal f = a.b("");
    ImmutableMoji moji = f.build();

    ImmutableStg.ABuildStage b1 = ImmutableStg.builder();
    ImmutableStg.BBuildStage a1 = b1.a(1);
    ImmutableStg.CBuildStage c1 = a1.b(1.2);
    ImmutableStg.BuildFinal f1 = c1.c("c");
    ImmutableStg stg = f1.build();

    ImmutableStagedFactory.SuperstringTheoryBuildStage b2 = ImmutableStagedFactory.superstringBuilder();
    ImmutableStagedFactory.SuperstringRealityBuildStage t2 = b2.theory(1);
    ImmutableStagedFactory.SuperstringBuildFinal r2 = t2.reality("x");
    ImmutableStagedFactory.SuperstringBuildFinal e2 = r2.evidence(null);
    String superstring = e2.build();
  }
}
