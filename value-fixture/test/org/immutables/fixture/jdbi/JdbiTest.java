/*
    Copyright 2014 Ievgen Lukash

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
package org.immutables.fixture.jdbi;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import static org.immutables.check.Checkers.*;

public class JdbiTest {
  static final DBI DBI = new DBI(JdbcConnectionPool.create("jdbc:h2:mem:test", "user", "pass"));

  private final SqlProxy proxy = DBI.onDemand(SqlProxy.class);

  @Test
  public void bindingAndMapping() {
    proxy.createTable();

    proxy.insert(ImmutableRecord.builder().id(1).build());
    proxy.insert(ImmutableRecord.builder().id(2).name("b").build());

    check(proxy.find(1)).is(ImmutableRecord.builder().id(1).build());
    check(proxy.find(2)).is(ImmutableRecord.builder().id(2).name("b").build());
  }
}
