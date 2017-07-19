package org.immutables.fixture.builder;

import static org.immutables.check.Checkers.check;

import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.FirstPartyWithBuilderExtension;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.ImmutableSamePackageVanillaAttributeBuilderParent;
import org.immutables.fixture.builder.attribute_builders.SamePackageVanillaAttributeBuilderParent;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithBuilderClassCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithBuilderInstanceCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithValueClassCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithValueInstanceCopyMethod;
import org.immutables.fixture.builder.functional.AttributeBuilderBuilderI;
import org.immutables.fixture.builder.functional.AttributeBuilderValueI;
import org.immutables.fixture.builder.functional.BuilderFunction;
import org.immutables.fixture.builder.functional.CopyFunction;
import org.junit.Test;

public class AttributeBuilderTest {

  @Test
  public void basicApiForVanillaParent() {
    assertBasicApi(ImmutableVanillaAttributeBuilderParent.class,
        VanillaAttributeBuilderParent.class,
        ImmutableVanillaAttributeBuilderParent::copyOf, VanillaAttributeBuilderParent.Builder::new);
  }

  @Test
  public void basicApiForNoJdkParent() {
    assertBasicApi(ImmutableJdkOnlyAttributeBuilderParent.class,
        JdkOnlyAttributeBuilderParent.class,
        ImmutableJdkOnlyAttributeBuilderParent::copyOf, JdkOnlyAttributeBuilderParent.Builder::new);
  }

  @Test
  public void basicApiForGuavaCollectionsParent() {
    assertBasicApi(ImmutableGuavaAttributeBuilderParent.class,
        GuavaAttributeBuilderParent.class,
        ImmutableGuavaAttributeBuilderParent::copyOf, GuavaAttributeBuilderParent.Builder::new);
  }

  @Test
  public void basicApiForSamePackageParent() {
    assertBasicApi(ImmutableSamePackageVanillaAttributeBuilderParent.class,
        SamePackageVanillaAttributeBuilderParent.class,
        ImmutableSamePackageVanillaAttributeBuilderParent::copyOf,
        SamePackageVanillaAttributeBuilderParent.Builder::new);
  }

  // Allows sharing tests between guava collections, jdk only collections and whatever other combinations are needed.
  private <ImmutableClassT extends AttributeBuilderValueI, AbstractClassT extends AttributeBuilderValueI>
  void assertBasicApi(Class<ImmutableClassT> immutableType, Class<AbstractClassT> returnType,
      CopyFunction<ImmutableClassT, AbstractClassT> copyFunction,
      BuilderFunction<AbstractClassT> newBuilder) {
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

    {
      AttributeBuilderBuilderI<AbstractClassT> builder = newBuilder.newBuilder();
      //builder.firstPartyImmutable(firstPartyImmutable);
      builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
      builder.thirdPartyImmutable(thirdPartyImmutable);
      builder.addFirstPartyImmutable(firstPartyImmutable);
      builder.addThirdPartyImmutable(thirdPartyImmutable);

      ImmutableFirstPartyImmutable.Builder _firstPartyBuilder =
          builder.firstPartyImmutableBuilder().value("first party through attributeBuilder");

      ImmutableClassT copy = copyFunction.copy(builder.build());
      check(copy.firstPartyImmutable().value()).is("first party through attributeBuilder");
    }

    {
      AttributeBuilderBuilderI<AbstractClassT> builder = newBuilder.newBuilder();
      builder.firstPartyImmutable(firstPartyImmutable);
      //builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
      builder.thirdPartyImmutable(thirdPartyImmutable);
      builder.addFirstPartyImmutable(firstPartyImmutable);
      builder.addThirdPartyImmutable(thirdPartyImmutable);

      ImmutableFirstPartyImmutableWithDifferentStyle.Abonabon _firstPartyBuilderWithDifferentStyle =
          builder.firstPartyImmutableWithDifferentStyleBuilder()
              .value("first party through attributeBuilder");

      ImmutableClassT copy = copyFunction.copy(builder.build());
      check(copy.firstPartyImmutableWithDifferentStyle().value())
          .is("first party through attributeBuilder");
    }

    {
      AttributeBuilderBuilderI<AbstractClassT> builder = newBuilder.newBuilder();
      builder.firstPartyImmutable(firstPartyImmutable);
      builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
      //builder.thirdPartyImmutable(thirdPartyImmutable);
      builder.addFirstPartyImmutable(firstPartyImmutable);
      builder.addThirdPartyImmutable(thirdPartyImmutable);

      ThirdPartyImmutable.Builder thirdPartyImmutableBuilder =
          builder.thirdPartyImmutableBuilder().setValue("third party through attributeBuilder");

      ImmutableClassT copy = copyFunction.copy(builder.build());
      check(copy.thirdPartyImmutable().getValue()).is("third party through attributeBuilder");
    }

    {
      AttributeBuilderBuilderI<AbstractClassT> builder = newBuilder.newBuilder();
      builder.firstPartyImmutable(firstPartyImmutable);
      builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
      builder.thirdPartyImmutable(thirdPartyImmutable);
      //builder.addFirstPartyImmutable(firstPartyImmutable);
      builder.addThirdPartyImmutable(thirdPartyImmutable);

      ImmutableFirstPartyImmutable.Builder firstPartyBuilder =
          builder.addFirstPartyImmutableBuilder().value("first party through attributeBuilder");

      ImmutableClassT copy = copyFunction.copy(builder.build());
      check(copy.firstPartyImmutableList().get(0).value())
          .is("first party through attributeBuilder");

    }

    {
      AttributeBuilderBuilderI<AbstractClassT> builder = newBuilder.newBuilder();
      builder.firstPartyImmutable(firstPartyImmutable);
      builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
      builder.thirdPartyImmutable(thirdPartyImmutable);
      builder.addFirstPartyImmutable(firstPartyImmutable);
      //builder.addThirdPartyImmutable(thirdPartyImmutable);

      ThirdPartyImmutable.Builder thirdPartyBuilder =
          builder.addThirdPartyImmutableBuilder().setValue("third party through attributeBuilder");

      ImmutableClassT copy = copyFunction.copy(builder.build());
      check(copy.thirdPartyImmutableList().get(0).getValue())
          .is("third party through attributeBuilder");

    }
  }

  @Test
  public void testThirdPartyApiWithValueInstanceCopy() {
    ImmutableNeapolitanAttributeBuilderParent.Builder builder
        = ImmutableNeapolitanAttributeBuilderParent.builder();

    ThirdPartyImmutableWithValueInstanceCopyMethod tpiWithValueInstanceCopyMethod = ThirdPartyImmutableWithValueInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .build();
    ThirdPartyImmutableWithValueClassCopyMethod tpiWithValueClassCopyMethod = ThirdPartyImmutableWithValueClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderInstanceCopyMethod tpiWithBuilderInstanceCopyMethod = ThirdPartyImmutableWithBuilderInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderClassCopyMethod tpiWithBuilderClassCopyMethod = ThirdPartyImmutableWithBuilderClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    FirstPartyWithBuilderExtension fpWithBuilderExtension = new FirstPartyWithBuilderExtension.Builder()
        .value("first praty")
        .build();

    //builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);

    ThirdPartyImmutableWithValueInstanceCopyMethod.Builder thirdPartyBuilder =
        builder.tpiWithValueInstanceCopyMethodBuilder()
            .setValue("third party through attributeBuilder");

    ImmutableNeapolitanAttributeBuilderParent copy = ImmutableNeapolitanAttributeBuilderParent
        .copyOf(builder.build());
    check(copy.tpiWithValueInstanceCopyMethod().getValue())
        .is("third party through attributeBuilder");
  }

  @Test
  public void testThirdPartyApiWithValueClassCopy() {
    ImmutableNeapolitanAttributeBuilderParent.Builder builder
        = ImmutableNeapolitanAttributeBuilderParent.builder();

    ThirdPartyImmutableWithValueInstanceCopyMethod tpiWithValueInstanceCopyMethod = ThirdPartyImmutableWithValueInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .build();
    ThirdPartyImmutableWithValueClassCopyMethod tpiWithValueClassCopyMethod = ThirdPartyImmutableWithValueClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderInstanceCopyMethod tpiWithBuilderInstanceCopyMethod = ThirdPartyImmutableWithBuilderInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderClassCopyMethod tpiWithBuilderClassCopyMethod = ThirdPartyImmutableWithBuilderClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    FirstPartyWithBuilderExtension fpWithBuilderExtension = new FirstPartyWithBuilderExtension.Builder()
        .value("first praty")
        .build();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    //builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);

    ThirdPartyImmutableWithValueClassCopyMethod.Builder thirdPartyBuilder =
        builder.tpiWithValueClassCopyMethodBuilder()
            .setValue("third party through attributeBuilder");

    ImmutableNeapolitanAttributeBuilderParent copy = ImmutableNeapolitanAttributeBuilderParent
        .copyOf(builder.build());
    check(copy.tpiWithValueClassCopyMethod().getValue())
        .is("third party through attributeBuilder");
  }

  @Test
  public void testThirdPartyApiWithBuilderInstanceCopy() {
    ImmutableNeapolitanAttributeBuilderParent.Builder builder
        = ImmutableNeapolitanAttributeBuilderParent.builder();

    ThirdPartyImmutableWithValueInstanceCopyMethod tpiWithValueInstanceCopyMethod = ThirdPartyImmutableWithValueInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .build();
    ThirdPartyImmutableWithValueClassCopyMethod tpiWithValueClassCopyMethod = ThirdPartyImmutableWithValueClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderInstanceCopyMethod tpiWithBuilderInstanceCopyMethod = ThirdPartyImmutableWithBuilderInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderClassCopyMethod tpiWithBuilderClassCopyMethod = ThirdPartyImmutableWithBuilderClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    FirstPartyWithBuilderExtension fpWithBuilderExtension = new FirstPartyWithBuilderExtension.Builder()
        .value("first praty")
        .build();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    //builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);

    ThirdPartyImmutableWithBuilderInstanceCopyMethod.Builder thirdPartyBuilder =
        builder.tpiWithBuilderInstanceCopyMethodBuilder()
            .setValue("third party through attributeBuilder");

    ImmutableNeapolitanAttributeBuilderParent copy = ImmutableNeapolitanAttributeBuilderParent
        .copyOf(builder.build());
    check(copy.tpiWithBuilderInstanceCopyMethod().getValue())
        .is("third party through attributeBuilder");
  }

  @Test
  public void testThirdPartyApiWithBuilderClassCopy() {
    ImmutableNeapolitanAttributeBuilderParent.Builder builder
        = ImmutableNeapolitanAttributeBuilderParent.builder();

    ThirdPartyImmutableWithValueInstanceCopyMethod tpiWithValueInstanceCopyMethod = ThirdPartyImmutableWithValueInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .build();
    ThirdPartyImmutableWithValueClassCopyMethod tpiWithValueClassCopyMethod = ThirdPartyImmutableWithValueClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderInstanceCopyMethod tpiWithBuilderInstanceCopyMethod = ThirdPartyImmutableWithBuilderInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderClassCopyMethod tpiWithBuilderClassCopyMethod = ThirdPartyImmutableWithBuilderClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    FirstPartyWithBuilderExtension fpWithBuilderExtension = new FirstPartyWithBuilderExtension.Builder()
        .value("first party")
        .build();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    //builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);

    ThirdPartyImmutableWithBuilderClassCopyMethod.Builder thirdPartyBuilder =
        builder.tpiWithBuilderClassCopyMethodBuilder()
            .setValue("third party through attributeBuilder");

    ImmutableNeapolitanAttributeBuilderParent copy = ImmutableNeapolitanAttributeBuilderParent
        .copyOf(builder.build());
    check(copy.tpiWithBuilderClassCopyMethod().getValue())
        .is("third party through attributeBuilder");
  }

  @Test
  public void testFirstPartyApiWithExtendingBuilder() {
    ImmutableNeapolitanAttributeBuilderParent.Builder builder
        = ImmutableNeapolitanAttributeBuilderParent.builder();

    ThirdPartyImmutableWithValueInstanceCopyMethod tpiWithValueInstanceCopyMethod = ThirdPartyImmutableWithValueInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .build();
    ThirdPartyImmutableWithValueClassCopyMethod tpiWithValueClassCopyMethod = ThirdPartyImmutableWithValueClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderInstanceCopyMethod tpiWithBuilderInstanceCopyMethod = ThirdPartyImmutableWithBuilderInstanceCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    ThirdPartyImmutableWithBuilderClassCopyMethod tpiWithBuilderClassCopyMethod = ThirdPartyImmutableWithBuilderClassCopyMethod
        .generateNewBuilder()
        .setValue("third party")
        .doTheBuild();
    FirstPartyWithBuilderExtension fpWithBuilderExtension = new FirstPartyWithBuilderExtension.Builder()
        .value("first party")
        .build();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    //builder.fpWithBuilderExtension(fpWithBuilderExtension);

    FirstPartyWithBuilderExtension.Builder fpWithBuilderExtensionBuilder =
        builder.fpWithBuilderExtensionBuilder()
            .value("first party through attributeBuilder");

    ImmutableNeapolitanAttributeBuilderParent copy = ImmutableNeapolitanAttributeBuilderParent
        .copyOf(builder.build());
    check(copy.fpWithBuilderExtension().value())
        .is("first party through attributeBuilder");
  }
}
