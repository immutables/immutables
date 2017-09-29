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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.google.gson.TypeAdapter;
import com.mongodb.MongoClient;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonParser;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;

/**
 * MongoDB driver specific encoding and jumping hoops.
 */
@SuppressWarnings("resource")
public final class BsonEncoding {
  private static final BsonFactory BSON_FACTORY = new BsonFactory()
      .enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH);

  private static final JsonFactory JSON_FACTORY = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

  /**
   * This field name will cause an MongoDB confuse if not unwrapped correctly so it may be a good
   * choice.
   */
  private static final String PREENCODED_VALUE_WRAPPER_FIELD_NAME = "$";

  private BsonEncoding() {}

  public static Object unwrapBsonable(Support.Adapted<?> adapted) {
    GsonCodecs.GsonCodec<Object> codec = new GsonCodecs.GsonCodec<Object>((Class<Object>) adapted.value.getClass(), (TypeAdapter<Object>) adapted.adapter);
    return marshalDocument(adapted.value, codec);
  }

  public static Bson unwrapJsonable(String json) {
    return BsonDocument.parse(json);
  }

  public static <T> Bson marshalDocument(T document, Codec<T> codec) {
    BsonDocument bson = new BsonDocument();
    org.bson.BsonWriter writer = new BsonDocumentWriter(bson);
    codec.encode(writer, document, EncoderContext.builder().build());
    return bson;
  }

  private static org.bson.AbstractBsonReader readerFor(Bson bson) {
    if (bson instanceof BsonDocument) {
      return new BsonDocumentReader((BsonDocument) bson);
    } else  {
      return new BsonDocumentReader(bson.toBsonDocument(BsonDocument.class,
              MongoClient.getDefaultCodecRegistry()));
    }

  }



}
