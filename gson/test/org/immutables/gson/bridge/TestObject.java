package org.immutables.gson.bridge;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Gson.TypeAdapters
@Value.Immutable
@JacksonXmlRootElement(localName = "test-object")
public interface TestObject {
  int intVal();

  Integer integerVal();

  List<Integer> listOfInteger();

  long longVal();

  Long longObjVal();

  List<Long> listOfLong();

  float floatVal();

  Float floatObjVal();

  List<Float> listOfFloat();

  double doubleVal();

  Double doubleObjVal();

  List<Double> listOfDouble();

  BigDecimal bigDecimalVal();

  List<BigDecimal> listOfBigDecimal();

  boolean booleanVal();

  Boolean booleanObjVal();

  List<Boolean> listOfBoolean();

  String stringVal();

  List<String> listOfString();

  Set<String> setOfString();

  Map<String, String> mapOfStringToString();

  TestEnum testEnumVal();

  List<TestEnum> listOfTestEnum();

  Map<TestEnum, String> mapOfTestEnumToString();

  List<TestSubObject> listOfSubObject();
}
