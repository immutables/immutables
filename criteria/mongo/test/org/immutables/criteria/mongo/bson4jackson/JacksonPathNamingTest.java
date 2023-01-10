/*
 * Copyright 2023 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.mongo.bson4jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.matcher.Projection;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.immutables.check.Checkers.check;

public class JacksonPathNamingTest {

  private final JacksonPathNaming naming = new JacksonPathNaming(new ObjectMapper());
  private final Bean1Criteria bean1 = Bean1Criteria.bean1;
  private final Bean2Criteria bean2 = Bean2Criteria.bean2;
  private final Bean3Criteria bean3 = Bean3Criteria.bean3;

  private String extractPathName(Projection<?> projection) {
    Path path = Visitors.toPath(Matchers.toExpression(projection));
    return naming.name(path);
  }

  @Test
  void bean1() {
    check(extractPathName(bean1.id)).is("_id");
    check(extractPathName(bean1.prop1)).is("prop1");
    check(extractPathName(bean1.prop2)).is("prop2");
    check(extractPathName(bean1.prop3)).is("prop3_changed");
    check(extractPathName(bean1.isProp4)).is("prop4");
  }

  @Test
  void bean2() {
    check(extractPathName(bean2.idProp)).is("_id");
    check(extractPathName(bean2.prop1)).is("prop1");
    check(extractPathName(bean2.prop2)).is("prop2");
    check(extractPathName(bean2.prop3)).is("prop3_changed");
  }

  @Test
  void nestedClasses() {
    check(extractPathName(bean3.bean1)).is("bean1");
    check(extractPathName(bean3.bean2)).is("bean2");

    check(extractPathName(bean3.bean2.idProp)).is("bean2._id");
    check(extractPathName(bean3.bean2.prop3)).is("bean2.prop3_changed");
    check(extractPathName(bean3.bean2.prop2)).is("bean2.prop2");

    check(extractPathName(bean3.bean1.value().id)).is("bean1._id");
    check(extractPathName(bean3.bean1.value().prop1)).is("bean1.prop1");
    check(extractPathName(bean3.bean1.value().prop2)).is("bean1.prop2");
    check(extractPathName(bean3.bean1.value().prop3)).is("bean1.prop3_changed");
  }

  @Value.Immutable
  @Criteria
  @JsonSerialize(as = ImmutableBean1.class)
  @JsonDeserialize(as = ImmutableBean1.class)
  public interface Bean1 {

    @Criteria.Id
    @JsonProperty("_id")
    String getId();

    String getProp1();

    @JsonProperty("prop2")
    String prop2();

    @JsonProperty("prop3_changed")
    String getProp3();

    @JsonProperty("prop4")
    boolean isProp4();
  }

  @Value.Immutable
  @Value.Style(get = {"get*", "is*"},
          implementationNestedInBuilder = true,
          overshadowImplementation = true,
          init = "set*")
  @Criteria
  @JsonDeserialize(builder = Bean2Builder.class)
  public interface Bean2 {
    @Criteria.Id
    @JsonProperty("_id")
    String getIdProp();

    String getProp1();

    boolean isProp2();

    @JsonProperty("prop3_changed")
    int getProp3();
  }

  @Value.Immutable
  @Criteria
  @JsonSerialize(as = ImmutableBean3.class)
  @JsonDeserialize(as = ImmutableBean3.class)
  public interface Bean3 {
    Optional<Bean1> bean1();

    Bean2 bean2();
  }
}
