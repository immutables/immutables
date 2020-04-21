/*
 * Copyright 2019 Immutables Authors and Contributors
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.javabean.JavaBean1;
import org.immutables.criteria.personmodel.ImmutablePerson;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

import static org.immutables.check.Checkers.check;

/**
 * Test that ID attributes are correctly identified
 */
class IdAnnotationModuleTest {

  static List<ObjectMapper> mappers() {
    ObjectMapper template = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new GuavaModule())
            .registerModule(new JavaTimeModule());

    ObjectMapper mapper1 = template.copy().registerModule(new IdAnnotationModule());
    ObjectMapper mapper2 = template.copy().registerModule(IdAnnotationModule.fromPredicate(m -> ((AnnotatedElement) m).isAnnotationPresent(Criteria.Id.class)));
    ObjectMapper mapper3 = template.copy().registerModule(IdAnnotationModule.fromAnnotation(Criteria.Id.class));
    return Arrays.asList(mapper1, mapper2, mapper3);
  }

  /**
   * Check annotation lookup for java beans
   */
  @ParameterizedTest
  @MethodSource("mappers")
  void javaBean(ObjectMapper mapper) throws IOException {
    JavaBean1 bean1 = new JavaBean1();
    bean1.setString1("id123");
    bean1.setInt1(42);
    bean1.setBase("base1");

    ObjectNode node = mapper.valueToTree(bean1);
    check(ImmutableList.copyOf(node.fieldNames())).has("_id");
    check(node.get("_id").asText()).is("id123");
    check(ImmutableList.copyOf(node.fieldNames())).not().has("string1");

    JavaBean1 bean2 = mapper.treeToValue(node, JavaBean1.class);
    check(bean2.getString1()).is("id123");
    check(bean2.getInt1()).is(42);
    check(bean2.getBase()).is("base1");
  }

  @ParameterizedTest
  @MethodSource("mappers")
  void immutable(ObjectMapper mapper) throws JsonProcessingException {
    // write
    ImmutablePerson person = new PersonGenerator().next().withId("id123");
    ObjectNode node = mapper.valueToTree(person);
    check(node.get("_id").asText()).is("id123");
    check(ImmutableList.copyOf(node.fieldNames())).not().has("id");

    check(mapper.treeToValue(node, Person.class).id()).is("id123");
    check(mapper.treeToValue(node, ImmutablePerson.class).id()).is("id123");
  }
  
}