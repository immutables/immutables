package org.immutables.gson.bridge;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.immutables.gson.stream.JsonGeneratorWriter;
import org.immutables.gson.stream.JsonParserReader;
import org.immutables.gson.stream.XmlParserReader;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Test the various Jackson serialization formats against the Gson/Jackson bridge.
 */
public class GsonJacksonBridgeSerializationTest {

  private final Gson gson = new GsonBuilder()
          .registerTypeAdapterFactory(new GsonAdaptersTestObject())
          .registerTypeAdapterFactory(new GsonAdaptersTestSubObject())
          .create();

  private TestObject createTestObject() {
    return ImmutableTestObject.builder()
            .intVal(123)
            .integerVal(123)
            .addListOfInteger(1, 2, 3, 4, 5, 6)
            .longVal(2349823948398472394L)
            .longObjVal(2349823948398472394L)
            .addListOfLong(2349823948398472394L, 2349822348398472394L, 2349823948123332394L)
            .floatVal(123123.0980F)
            .floatObjVal(123123.0980F)
            .addListOfFloat(123123.0980F, 124234.0980F, 1233455.0980F)
            .doubleVal(123123123.1231231)
            .doubleObjVal(123123123.1231231)
            .addListOfDouble(123123.1231, 123123123.123213, 234234234.23423)
            .bigDecimalVal(new BigDecimal(4234234.1312313123))
            .addListOfBigDecimal(new BigDecimal(123123.123123), new BigDecimal(3534345.345345), new BigDecimal(35252.2411))
            .booleanVal(true)
            .booleanObjVal(false)
            .addListOfBoolean(true, false, true)
            .stringVal("This is a test")
            .addListOfString("a", "b", "c")
            .addSetOfString("a", "b", "c")
            .putMapOfStringToString("a", "1")
            .putMapOfStringToString("b", "2")
            .testEnumVal(TestEnum.VALUE1)
            .putMapOfTestEnumToString(TestEnum.VALUE2, "test2")
            .putMapOfTestEnumToString(TestEnum.VALUE3, "test3")
            .addListOfSubObject(ImmutableTestSubObject.builder()
                    .a("Test")
                    .b(1231)
                    .build())
            .build();
  }

  @Test
  public void jsonFactoryTest() throws IOException {
    TestObject value = createTestObject();
    JsonFactory factory = new JsonFactory();
    Class<TestObject> clazz = TestObject.class;
    ByteArrayOutputStream outputStream = testWriting(value, factory, clazz);
    TestObject value2 = testReading(factory, clazz, outputStream);
    Assert.assertEquals(value2.toString(), value.toString());
  }

  @Test
  public void xmlFactoryTest() throws IOException {
    TestObject value = createTestObject();
    XmlFactory factory = new XmlFactory();
    Class<TestObject> clazz = TestObject.class;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ToXmlGenerator g = factory.createGenerator(outputStream);
    g.setNextName(QName.valueOf(clazz.getAnnotation(JacksonXmlRootElement.class).localName()));
    JsonGeneratorWriter generatorWriter = new JsonGeneratorWriter(g);
    gson.toJson(value, clazz, generatorWriter);
    generatorWriter.flush();
    TestObject value2 = testXmlReading(factory, clazz, outputStream);
    Assert.assertEquals(value2.toString(), value.toString());
  }

  @Test
  public void yamlFactoryTest() throws IOException {
    TestObject value = createTestObject();
    YAMLFactory factory = new YAMLFactory();
    Class<TestObject> clazz = TestObject.class;
    ByteArrayOutputStream outputStream = testWriting(value, factory, clazz);
    TestObject value2 = testReading(factory, clazz, outputStream);
    Assert.assertEquals(value2.toString(), value.toString());
  }

  @Test
  public void smileFactoryTest() throws IOException {
    TestObject value = createTestObject();
    SmileFactory factory = new SmileFactory();
    Class<TestObject> clazz = TestObject.class;
    ByteArrayOutputStream outputStream = testWriting(value, factory, clazz);
    TestObject value2 = testReading(factory, clazz, outputStream);
    Assert.assertEquals(value2.toString(), value.toString());
  }

  @Test
  public void propertiesFactoryTest() throws IOException {
    TestObject value = ImmutableTestObject.copyOf(createTestObject())
            // excluding map here because when it is de-serialized, the order is changed and the unit test fails
            .withMapOfStringToString(new HashMap<String, String>());
    JavaPropsFactory factory = new JavaPropsFactory();
    Class<TestObject> clazz = TestObject.class;
    ByteArrayOutputStream outputStream = testWriting(value, factory, clazz);
    TestObject value2 = testXmlReading(factory, clazz, outputStream);
    Assert.assertEquals(value2.toString(), value.toString());
  }

  @Test
  public void cborFactoryTest() throws IOException {
    TestObject value = createTestObject();
    CBORFactory factory = new CBORFactory();
    Class<TestObject> clazz = TestObject.class;
    ByteArrayOutputStream outputStream = testWriting(value, factory, clazz);
    TestObject value2 = testReading(factory, clazz, outputStream);
    Assert.assertEquals(value2.toString(), value.toString());
  }

  private ByteArrayOutputStream testWriting(TestObject value, JsonFactory factory, Class<TestObject> clazz) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonGenerator g = factory.createGenerator(outputStream);
    JsonGeneratorWriter generatorWriter = new JsonGeneratorWriter(g);
    gson.toJson(value, clazz, generatorWriter);
    generatorWriter.flush();
    return outputStream;
  }

  private TestObject testReading(JsonFactory factory, Class<TestObject> clazz, ByteArrayOutputStream outputStream) throws IOException {
    JsonParser r = factory.createParser(outputStream.toByteArray());
    JsonParserReader parserReader = new JsonParserReader(r);
    return gson.fromJson(parserReader, clazz);
  }

  private TestObject testXmlReading(JsonFactory factory, Class<TestObject> clazz, ByteArrayOutputStream outputStream) throws IOException {
    JsonParser r = factory.createParser(outputStream.toByteArray());
    JsonParserReader parserReader = new XmlParserReader(r);
    return gson.fromJson(parserReader, clazz);
  }
}
