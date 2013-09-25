/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.generate.internal.javascript;

import java.util.Map;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

public class ScriptableMapWrapper extends ScriptableObject implements Wrapper {

  private final Map<Object, Object> map;

  public ScriptableMapWrapper(Map<Object, Object> map) {
    this.map = map;
  }

  @Override
  public boolean has(int index, Scriptable start) {
    return has(String.valueOf(index), start);
  }

  @Override
  public boolean has(String name, Scriptable start) {
    return map.containsKey(name);
  }

  @Override
  public Object get(int index, Scriptable start) {
    return get(String.valueOf(index), start);
  }

  @Override
  public void delete(int index) {
    delete(String.valueOf(index));
  }

  @Override
  public Object get(String name, Scriptable start) {
    return has(name, start) ? map.get(name) : NOT_FOUND;
  }

  @Override
  public void put(String name, Scriptable start, Object value) {
    map.put(name, value);
  }

  @Override
  public void put(int index, Scriptable start, Object value) {
    put(String.valueOf(index), start, value);
  }

  @Override
  public void delete(String name) {
    map.remove(name);
  }

  @Override
  public Object[] getAllIds() {
    return map.keySet().toArray();
  }

  @Override
  public Object unwrap() {
    return map;
  }

  @Override
  public Object getDefaultValue(Class<?> typeHint) {
    if (typeHint == null || typeHint == String.class) {
      return map.toString();
    }
    return super.getDefaultValue(typeHint);
  }

  @Override
  public String getClassName() {
    return getClass().getSimpleName();
  }

}
