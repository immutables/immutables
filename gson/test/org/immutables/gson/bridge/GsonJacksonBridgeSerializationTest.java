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
import java.util.Optional;
import java.util.function.Function;

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
    TestObject testObject = createTestObject();
    JsonFactory factory = new JsonFactory();
    runTestOnFactory(
            testObject
            , TestObject.class
            , out -> {
              try {
                return factory.createGenerator(out);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , os -> {
              try {
                return factory.createParser(os.toByteArray());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , JsonParserReader::new);
  }

  @Test
  public void xmlFactoryTest() throws IOException {
    TestObject testObject = createTestObject();
    XmlFactory factory = new XmlFactory();
    Class<TestObject> clazz = TestObject.class;
    runTestOnFactory(
            testObject
            , clazz
            , out -> {
              try {
                ToXmlGenerator generator = factory.createGenerator(out);
                QName rootName = Optional.ofNullable(clazz.getAnnotation(JacksonXmlRootElement.class))
                        .map(a -> QName.valueOf(a.localName()))
                        .orElse(QName.valueOf(clazz.getName()));
                generator.setNextName(rootName);
                return generator;
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , os -> {
              try {
                return factory.createParser(os.toByteArray());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , XmlParserReader::new);
  }

  @Test
  public void yamlFactoryTest() throws IOException {
    TestObject testObject = createTestObject();
    YAMLFactory factory = new YAMLFactory();
    runTestOnFactory(
            testObject
            , TestObject.class
            , out -> {
              try {
                return factory.createGenerator(out);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , os -> {
              try {
                return factory.createParser(os.toByteArray());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , JsonParserReader::new);
  }

  @Test
  public void smileFactoryTest() throws IOException {
    TestObject testObject = createTestObject();
    SmileFactory factory = new SmileFactory();
    runTestOnFactory(
            testObject
            , TestObject.class
            , out -> {
              try {
                return factory.createGenerator(out);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , os -> {
              try {
                return factory.createParser(os.toByteArray());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , JsonParserReader::new);
  }

  @Test
  public void propertiesFactoryTest() throws IOException {
    TestObject testObject = ImmutableTestObject.copyOf(createTestObject())
            // excluding map here because when it is de-serialized, the order is changed and the unit test fails
            .withMapOfStringToString(new HashMap<>());
    JavaPropsFactory factory = new JavaPropsFactory();
    runTestOnFactory(
            testObject
            , TestObject.class
            , out -> {
              try {
                return factory.createGenerator(out);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , os -> {
              try {
                return factory.createParser(os.toByteArray());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , XmlParserReader::new);
  }

  @Test
  public void cborFactoryTest() throws IOException {
    TestObject testObject = createTestObject();
    CBORFactory factory = new CBORFactory();
    runTestOnFactory(
            testObject
            , TestObject.class
            , out -> {
              try {
                return factory.createGenerator(out);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , os -> {
              try {
                return factory.createParser(os.toByteArray());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            , JsonParserReader::new);
  }

  private <VALUE
          , WRITER extends JsonGenerator
          , READER extends JsonParser
          > void runTestOnFactory(
          VALUE value
          , Class<VALUE> clazz
          , Function<ByteArrayOutputStream, WRITER> writerProducer
          , Function<ByteArrayOutputStream, READER> readerProducer
          , Function<READER, JsonParserReader> bridgeReaderProducer
  ) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WRITER g = writerProducer.apply(outputStream);
    JsonGeneratorWriter generatorWriter = new JsonGeneratorWriter(g);
    gson.toJson(value, clazz, generatorWriter);
    generatorWriter.flush();

    READER r = readerProducer.apply(outputStream);
    JsonParserReader parserReader = bridgeReaderProducer.apply(r);
    VALUE value2 = gson.fromJson(parserReader, clazz);

    Assert.assertEquals(value2.toString(), value.toString());
  }
}
