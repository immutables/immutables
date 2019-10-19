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

package org.immutables.criteria.backend;

import org.immutables.criteria.expression.Path;
import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;

class JavaBeanNamingTest {

  private final JavaBeanNaming naming = new JavaBeanNaming();

  @Test
  void bean() throws NoSuchMethodException {
    check(naming.name(path("getA"))).is("a");
    check(naming.name(path("getFoo"))).is("foo");
    check(naming.name(path("notABean"))).is("notABean");
    check(naming.name(path("getURL"))).is("URL");
    check(naming.name(path("isBool"))).is("bool");
    check(naming.name(path("isBOOLEAN"))).is("BOOLEAN");
    check(naming.name(path("isNotBoolean"))).is("isNotBoolean");
  }

  @Test
  void booleans() throws NoSuchMethodException {
    check(!JavaBeanNaming.IS_GETTER.test(JavaBean.class.getDeclaredMethod("isNotBoolean")));
    check(JavaBeanNaming.IS_GETTER.test(JavaBean.class.getDeclaredMethod("isBOOLEAN")));
    check(JavaBeanNaming.IS_GETTER.test(JavaBean.class.getDeclaredMethod("isBool")));
    check(JavaBeanNaming.IS_GETTER.test(JavaBean.class.getDeclaredMethod("getURL")));
    check(!JavaBeanNaming.IS_GETTER.test(JavaBean.class.getDeclaredMethod("notABean")));
    check(JavaBeanNaming.IS_GETTER.test(JavaBean.class.getDeclaredMethod("getA")));
  }

  private static Path path(String name) throws NoSuchMethodException {
    return Path.ofMember(JavaBean.class.getDeclaredMethod(name));
  }

  static class JavaBean {
    public String getNotABean(String foo) {
      return "";
    }

    public String notABean() {
      return "";
    }

    public String getA() {
      return "";
    }

    public String getFoo() {
      return "";
    }

    public String getURL() {
      return "";
    }

    public boolean isBool() {
      return true;
    }

    public boolean isBOOLEAN() {
      return true;
    }

    /**
     * This is not a getter because return type is not boolean
     */
    public int isNotBoolean() {
      return 0;
    }
  }

}