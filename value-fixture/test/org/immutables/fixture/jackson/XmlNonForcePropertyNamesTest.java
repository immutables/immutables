package org.immutables.fixture.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.immutables.value.Value;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class XmlNonForcePropertyNamesTest {

  @Target({ElementType.PACKAGE, ElementType.TYPE})
  @Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
  @JsonSerialize
  @Value.Style(
      forceJacksonPropertyNames = false // otherwise we can't use RosettaNamingStrategies
  )
  @interface CustomStyle {}

  @Value.Immutable
  @CustomStyle
  public interface MyObject {
    @JacksonXmlProperty(isAttribute = true)
    String getPlaceholder();
  }

  @Test
  public void itWorks() throws Exception {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());
    ImmutableMyObject object = xmlMapper.readValue(
        "<MyObject placeholder=\"This is a placeholder\" />", ImmutableMyObject.class);

    check(object.getPlaceholder()).is("This is a placeholder");
  }
}
