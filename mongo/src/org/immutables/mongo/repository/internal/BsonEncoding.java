/*
   Copyright 2013-2015 Immutables Authors and Contributors

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
package org.immutables.mongo.repository.internal;

import com.google.gson.TypeAdapter;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;

/**
 * MongoDB driver specific encoding and jumping hoops.
 */
final class BsonEncoding {

  /**
   * This field name will cause an MongoDB confuse if not unwrapped correctly so it may be a good
   * choice.
   */
  private static final String PREENCODED_VALUE_WRAPPER_FIELD_NAME = "$";

  private BsonEncoding() {}

  /**
   * Bson doesn't allow to write directly scalars / primitives, they have to be embedded in a document.
   */
  static Object unwrapBsonable(Support.Adapted<?> adapted) {
    @SuppressWarnings("unchecked")
    GsonCodecs.GsonCodec<Object> codec = new GsonCodecs.GsonCodec<Object>((Class<Object>) adapted.value.getClass(), (TypeAdapter<Object>) adapted.adapter);
    BsonDocument bson = new BsonDocument();
    org.bson.BsonWriter writer = new BsonDocumentWriter(bson);
    writer.writeStartDocument();
    writer.writeName(PREENCODED_VALUE_WRAPPER_FIELD_NAME);
    codec.encode(writer, adapted.value, EncoderContext.builder().build());
    writer.writeEndDocument();
    writer.flush();
    return bson.get(PREENCODED_VALUE_WRAPPER_FIELD_NAME);
  }

  static Bson unwrapJsonable(String json) {
    return BsonDocument.parse(json);
  }

}
