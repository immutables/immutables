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
package nonimmutables.recurs;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

@JsonSerialize
@Immutable
public interface AbstractSomething extends IHaveGetContentAndWithId<MySomethingContent, Something> {

  // this override is not needed with 2.2, hooray! \o/
  // @Override
  // MySomethingContent getContent();

  // without this override it generates the withId method that returns T instead of Something for
  // the Json class ;(
  // @Override
  // Something withId(int id);
}
