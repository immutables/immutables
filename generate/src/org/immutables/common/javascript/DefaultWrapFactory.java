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
package org.immutables.common.javascript;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

public class DefaultWrapFactory extends WrapFactory {

  @Override
  public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
    if (obj == null
        || obj instanceof String
        || obj instanceof Number
        || obj instanceof Boolean
        || obj instanceof Scriptable
        || obj == Undefined.instance) {
      return obj;
    }
    if (obj instanceof Character) {
      return obj.toString();
    }
    if (obj instanceof Enum<?>) {
      return ((Enum<?>) obj).name();
    }
    if (obj instanceof Object[]) {
      Object[] array = (Object[]) obj;
      Object[] contentCopy = new Object[array.length];
      System.arraycopy(array, 0, contentCopy, 0, array.length);
      wrapContent(cx, scope, contentCopy);
      return createNativeArray(scope, contentCopy);
    }
    if (obj instanceof Collection<?>) {
      Object[] contentCopy = ((Collection<?>) obj).toArray();
      wrapContent(cx, scope, contentCopy);
      return createNativeArray(scope, contentCopy);
    }
    if (obj instanceof Map<?, ?>) {
      Map<?, ?> map = (Map<?, ?>) obj;

      NativeObject nativeObject = createNativeObject(scope);
      for (Object e : map.entrySet()) {
        Entry<?, ?> entry = (Entry<?, ?>) e;
        nativeObject.put(
            String.valueOf(entry.getKey().toString()),
            nativeObject,
            wrap(cx, scope, entry.getValue(), null));
      }
      return nativeObject;
    }
    return super.wrap(cx, scope, obj, staticType);
  }

  private NativeObject createNativeObject(Scriptable scope) {
    NativeObject nativeObject = new NativeObject();
    nativeObject.setPrototype(ScriptableObject.getObjectPrototype(scope));
    return nativeObject;
  }

  private Object createNativeArray(Scriptable scope, Object[] contentCopy) {
    NativeArray nativeArray = new NativeArray(contentCopy);
    nativeArray.setPrototype(ScriptableObject.getArrayPrototype(scope));
    return nativeArray;
  }

  private void wrapContent(Context cx, Scriptable scope, Object[] content) {
    for (int i = 0; i < content.length; i++) {
      content[i] = wrap(cx, scope, content[i], null);
    }
  }
}
