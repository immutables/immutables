package org.immutables.mongo.bson4gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonType;
import org.bson.codecs.DateCodec;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import static org.immutables.check.Checkers.check;

public class GsonCodecsTest {

  @Test
  public void reflectiveTypeAdapter() {
    check(!GsonCodecs.isReflectiveTypeAdapter(new GsonBuilder().create().getAdapter(BigDecimal.class)));
  }

  @Test
  public void typeAdapterFromDateCodec() throws IOException {
    TypeAdapter<Date> adapter = GsonCodecs.typeAdapterFromCodec(new DateCodec());
    Date date = new Date();
    BsonDocument doc = new BsonDocument();
    BsonDocumentWriter writer = new BsonDocumentWriter(doc);
    writer.writeStartDocument();
    writer.writeName("$date");
    adapter.write(new BsonWriter(writer), date);
    writer.writeEndDocument();

    check(doc.keySet()).hasSize(1);
    check(doc.get("$date").getBsonType()).is(BsonType.DATE_TIME);
    check(doc.get("$date").asDateTime().getValue()).is(date.getTime());
  }

  @Test
  public void newGsonWithBsonSupport() {
    Gson gson = new Gson();
    Gson actual = GsonCodecs.newGsonWithBsonSupport(gson);

    check(actual).not().same(gson);

    check(gson.getAdapter(BsonDecimal128.class)).isA(ReflectiveTypeAdapterFactory.Adapter.class);
    check(actual.getAdapter(BsonDecimal128.class)).not().isA(ReflectiveTypeAdapterFactory.Adapter.class);
  }

}