package org.immutables.fixture.builder;

import static org.immutables.check.Checkers.check;

import org.immutables.fixture.builder.ImmutableNestedBuilderApiParent.Builder;
import org.junit.Test;

public class NestedBuilderApiTest {

  @Test
  public void testFirstPartyApiForSingle() {
    Builder builder = ImmutableNestedBuilderApiParent.builder();
    FirstPartyImmutable firstPartyImmutable = ImmutableFirstPartyImmutable
        .builder()
        .value("first party")
        .build();
    FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle = ImmutableFirstPartyImmutableWithDifferentStyle
        .getTheBuilder()
        .value("first party")
        .doIIT();
    ThirdPartyImmutable thirdPartyImmutable = ThirdPartyImmutable
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithValueInstanceCopyMethod thirdPartyImmutableWithValueInstanceCopyMethod = ThirdPartyImmutableWithValueInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .build();

    //builder.firstPartyImmutable(firstPartyImmutable);
    builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
    builder.thirdPartyImmutable(thirdPartyImmutable);
    builder.addFirstPartyImmutable(firstPartyImmutable);
    builder.addThirdPartyImmutable(thirdPartyImmutable);
    builder.thirdPartyImmutableWithInstanceCopyMethod(thirdPartyImmutableWithValueInstanceCopyMethod);

    ImmutableFirstPartyImmutable.Builder firstPartyBuilder =
        builder.getFirstPartyImmutableBuilder().value("first party through nestedBuilder");

    ImmutableNestedBuilderApiParent copy = ImmutableNestedBuilderApiParent.copyOf(builder.build());
    check(copy.firstPartyImmutable().value()).is("first party through nestedBuilder");
  }

  @Test
  public void testFirstPartyApiWithDifferentStyleForSingle() {
    Builder builder = ImmutableNestedBuilderApiParent.builder();
    FirstPartyImmutable firstPartyImmutable = ImmutableFirstPartyImmutable
        .builder()
        .value("first party")
        .build();
    FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle = ImmutableFirstPartyImmutableWithDifferentStyle
        .getTheBuilder()
        .value("first party")
        .doIIT();
    ThirdPartyImmutable thirdPartyImmutable = ThirdPartyImmutable
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithValueInstanceCopyMethod thirdPartyImmutableWithValueInstanceCopyMethod = ThirdPartyImmutableWithValueInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .build();

    builder.firstPartyImmutable(firstPartyImmutable);
    //builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
    builder.thirdPartyImmutable(thirdPartyImmutable);
    builder.addFirstPartyImmutable(firstPartyImmutable);
    builder.addThirdPartyImmutable(thirdPartyImmutable);
    builder.thirdPartyImmutableWithInstanceCopyMethod(thirdPartyImmutableWithValueInstanceCopyMethod);

    ImmutableFirstPartyImmutableWithDifferentStyle.Abonabon firstPartyBuilder =
        builder.getFirstPartyImmutableWithDifferentStyleBuilder().value("first party new style through nestedBuilder");

    ImmutableNestedBuilderApiParent copy = ImmutableNestedBuilderApiParent.copyOf(builder.build());
    check(copy.firstPartyImmutableWithDifferentStyle().value()).is("first party new style through nestedBuilder");
  }

  /*

  @Test
  public void testThirdPartyApiForSingl() {
    Builder builder = ImmutableCompositionImmutable.builder();
    FirstPartyImmutable firstPartyImmutable = ImmutableFirstPartyImmutable
        .builder()
        .value("first party")
        .build();
     FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle = ImmutableFirstPartyImmutableWithDifferentStyle
        .getTheBuilder()
        .value("first party")
        .doIIT();
     ThirdPartyImmutable thirdPartyImmutable = ThirdPartyImmutable
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();

    builder.firstPartyImmutable(firstPartyImmutable);
    //builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle); TODO: uncomment
    //builder.thirdPartyImmutable(thirdPartyImmutable);
    builder.addFirstPartyImmutable(firstPartyImmutable);
    builder.addThirdPartyImmutable(thirdPartyImmutable);
    builder.thirdPartyImmutableWithInstanceCopyMethod(thirdPartyImmutable);
    builder.thirdPartyImmutableWithMarkerAnnotationDefinition(thirdPartyImmutable);
    builder.thirdPartyImmutableWithBuilderClassCopyMethod(thirdPartyImmutable);

    ThirdPartyImmutable.Builder thirdPartyImmutableBuilder =
        builder.getThirdPartyImmutableBuilder().setValue("third party through nestedBuilder");

    ImmutableCompositionImmutable copy = ImmutableCompositionImmutable.copyOf(builder.build());
    check(copy.thirdPartyImmutable().getValue()).is("third party through nestedBuilder");
  }

  @Test
  public void testFirstPartyApiForList() {
    Builder builder = ImmutableCompositionImmutable.builder();
    FirstPartyImmutable firstPartyImmutable = ImmutableFirstPartyImmutable
        .builder()
        .value("first party")
        .build();
    FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle = ImmutableFirstPartyImmutableWithDifferentStyle
        .getTheBuilder()
        .value("first party")
        .doIIT();
     ThirdPartyImmutable thirdPartyImmutable = ThirdPartyImmutable
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();

    builder.firstPartyImmutable(firstPartyImmutable);
    //builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle); TOODO: uncomment
    builder.thirdPartyImmutable(thirdPartyImmutable);
    //builder.addFirstPartyImmutable(firstPartyImmutable);
    builder.addThirdPartyImmutable(thirdPartyImmutable);
    builder.thirdPartyImmutableWithInstanceCopyMethod(thirdPartyImmutable);
    builder.thirdPartyImmutableWithMarkerAnnotationDefinition(thirdPartyImmutable);
    builder.thirdPartyImmutableWithBuilderClassCopyMethod(thirdPartyImmutable);

    ImmutableFirstPartyImmutable.Builder firstPartyBuilder =
        builder.addFirstPartyImmutableBuilder().value("first party through nestedBuilder");

    ImmutableCompositionImmutable copy = ImmutableCompositionImmutable.copyOf(builder.build());
    check(copy.firstPartyImmutableList().get(0).value()).is("first party through nestedBuilder");
  }
  */

}
