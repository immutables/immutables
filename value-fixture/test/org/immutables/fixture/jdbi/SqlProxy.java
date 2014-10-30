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

import org.immutables.common.jdbi.MapperFactory;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.immutables.common.jdbi.BindValue;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

@RegisterMapperFactory(MapperFactory.class)
public interface SqlProxy {
  @SqlUpdate("create table something (id int primary key, name varchar(100))")
  void createTable();

  @SqlQuery("select * from something where id = :id")
  Record find(@Bind("id") int id);

  @SqlUpdate("insert into something (id, name) values (:id, :name)")
  void insert(@BindValue Record record);
}
