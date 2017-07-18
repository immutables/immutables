package org.immutables.fixture.builder;

import static org.immutables.check.Checkers.check;

import org.immutables.fixture.builder.ImmutableAttributeBuilderParent.Builder;
import org.junit.Test;

public class AttributeBuilderTest {

  @Test
  public void testFirstPartyApiForSingle() {
    Builder builder = ImmutableAttributeBuilderParent.builder();
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
        builder.firstPartyImmutableBuilder().value("first party through nestedBuilder");

    ImmutableAttributeBuilderParent copy = ImmutableAttributeBuilderParent.copyOf(builder.build());
    check(copy.firstPartyImmutable().value()).is("first party through nestedBuilder");
  }

  @Test
  public void testFirstPartyApiWithDifferentStyleForSingle() {
    Builder builder = ImmutableAttributeBuilderParent.builder();
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
        builder.firstPartyImmutableWithDifferentStyleBuilder().value("first party new style through nestedBuilder");

    ImmutableAttributeBuilderParent copy = ImmutableAttributeBuilderParent.copyOf(builder.build());
    check(copy.firstPartyImmutableWithDifferentStyle().value()).is("first party new style through nestedBuilder");
  }

  @Test
  public void testThirdPartyApiForSingle() {
    Builder builder = ImmutableAttributeBuilderParent.builder();
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
    builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
    //builder.thirdPartyImmutable(thirdPartyImmutable);
    builder.addFirstPartyImmutable(firstPartyImmutable);
    builder.addThirdPartyImmutable(thirdPartyImmutable);
    builder.thirdPartyImmutableWithInstanceCopyMethod(thirdPartyImmutableWithValueInstanceCopyMethod);

    ThirdPartyImmutable.Builder thirdPartyImmutableBuilder =
        builder.thirdPartyImmutableBuilder().setValue("third party through nestedBuilder");

    ImmutableAttributeBuilderParent copy = ImmutableAttributeBuilderParent.copyOf(builder.build());
    check(copy.thirdPartyImmutable().getValue()).is("third party through nestedBuilder");
  }

  @Test
  public void testFirstPartyApiForList() {
    Builder builder = ImmutableAttributeBuilderParent.builder();
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
    builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
    builder.thirdPartyImmutable(thirdPartyImmutable);
    //builder.addFirstPartyImmutable(firstPartyImmutable);
    builder.addThirdPartyImmutable(thirdPartyImmutable);
    builder.thirdPartyImmutableWithInstanceCopyMethod(thirdPartyImmutableWithValueInstanceCopyMethod);

    ImmutableFirstPartyImmutable.Builder firstPartyBuilder =
        builder.addFirstPartyImmutableBuilder().value("first party through nestedBuilder");

    ImmutableAttributeBuilderParent copy = ImmutableAttributeBuilderParent.copyOf(builder.build());
    check(copy.firstPartyImmutableList().get(0).value()).is("first party through nestedBuilder");
  }
}
