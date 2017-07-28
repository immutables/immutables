package org.immutables.fixture.builder;

import static org.immutables.check.Checkers.check;

import java.util.Arrays;
import java.util.List;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.FirstPartyWithBuilderExtension;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutable.Builder;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.ImmutableSamePackageVanillaAttributeBuilderParent;
import org.immutables.fixture.builder.attribute_builders.SamePackageVanillaAttributeBuilderParent;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithBuilderClassCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithBuilderInstanceCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithNestedBuilder;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithValueClassCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithValueInstanceCopyMethod;
import org.immutables.fixture.builder.detection.ImmutableNewTokenAttributeBuilderParent;
import org.immutables.fixture.builder.detection.ImmutableNoNewTokenAttributeBuilderParent;
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
  public void basicApiForJdkOnlyParent() {
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

    // builder setter api
    {
      AttributeBuilderBuilderI<AbstractClassT> builder = newBuilder.newBuilder();
      //builder.firstPartyImmutable(firstPartyImmutable);
      builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
      builder.thirdPartyImmutable(thirdPartyImmutable);
      builder.addFirstPartyImmutable(firstPartyImmutable);
      builder.addThirdPartyImmutable(thirdPartyImmutable);

      ImmutableFirstPartyImmutable.Builder firstPartyBuilder = ImmutableFirstPartyImmutable
          .builder().value("first party through setter");
      builder.firstPartyImmutableBuilder(firstPartyBuilder);

      ImmutableClassT copy = copyFunction.copy(builder.build());
      check(copy.firstPartyImmutable().value()).is("first party through setter");

      // Make sure that we aren't copying the builder we set.
      firstPartyBuilder.value("another value");
      ImmutableClassT copy2 = copyFunction.copy(builder.build());
      check(copy2.firstPartyImmutable().value()).is("another value");
    }

    // Builder List modification
    {
      AttributeBuilderBuilderI<AbstractClassT> builder = newBuilder.newBuilder();
      builder.firstPartyImmutable(firstPartyImmutable);
      builder.firstPartyImmutableWithDifferentStyle(firstPartyImmutableWithDifferentStyle);
      builder.thirdPartyImmutable(thirdPartyImmutable);
      //builder.addFirstPartyImmutable(firstPartyImmutable);
      builder.addThirdPartyImmutable(thirdPartyImmutable);

      ImmutableFirstPartyImmutable.Builder first = ImmutableFirstPartyImmutable
          .builder().value("first party through setter 1");
      ImmutableFirstPartyImmutable.Builder second = ImmutableFirstPartyImmutable
          .builder().value("first party through setter 2");
      ImmutableFirstPartyImmutable.Builder third = ImmutableFirstPartyImmutable
          .builder().value("first party through setter 3");
      builder.addAllFirstPartyImmutableBuilders(first);
      builder.addAllFirstPartyImmutableBuilders(Arrays.asList(second, third));

      List<Builder> builderList = builder.firstPartyImmutableBuilders();
      check(builderList.size()).is(3);
      boolean thrown = false;
      try {
        builderList.add(ImmutableFirstPartyImmutable.builder());
      } catch(Exception e) {
        thrown = true;
      }
      check("Should not have been able to modify builder list, but could", thrown);
      check(builderList.size()).is(3);
      builderList.get(1).value("first party through setter munged");

      ImmutableClassT copy = copyFunction.copy(builder.build());
      check(copy.firstPartyImmutableList().size()).is(3);
      check(copy.firstPartyImmutableList().get(0).value()).is("first party through setter 1");
      check(copy.firstPartyImmutableList().get(1).value()).is("first party through setter munged");
      check(copy.firstPartyImmutableList().get(2).value()).is("first party through setter 3");

      // Make sure that we aren't copying the builder we add.
      third.value("MUNGING");
      ImmutableClassT copy2 = copyFunction.copy(builder.build());
      check(copy2.firstPartyImmutableList().get(2).value()).is("MUNGING");
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
    ThirdPartyImmutableWithNestedBuilder tpiWithNestedBuilder = new ThirdPartyImmutableWithNestedBuilder.Builder()
        .setValue("third party")
        .doTheBuild();

    //builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);
    builder.tpiWithNestedBuilder(tpiWithNestedBuilder);

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
    ThirdPartyImmutableWithNestedBuilder tpiWithNestedBuilder = new ThirdPartyImmutableWithNestedBuilder.Builder()
        .setValue("third party")
        .doTheBuild();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    //builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);
    builder.tpiWithNestedBuilder(tpiWithNestedBuilder);

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
    ThirdPartyImmutableWithNestedBuilder tpiWithNestedBuilder = new ThirdPartyImmutableWithNestedBuilder.Builder()
        .setValue("third party")
        .doTheBuild();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    //builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);
    builder.tpiWithNestedBuilder(tpiWithNestedBuilder);

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
    ThirdPartyImmutableWithNestedBuilder tpiWithNestedBuilder = new ThirdPartyImmutableWithNestedBuilder.Builder()
        .setValue("third party")
        .doTheBuild();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    //builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);
    builder.tpiWithNestedBuilder(tpiWithNestedBuilder);

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
    ThirdPartyImmutableWithNestedBuilder tpiWithNestedBuilder = new ThirdPartyImmutableWithNestedBuilder.Builder()
        .setValue("third party")
        .doTheBuild();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    //builder.fpWithBuilderExtension(fpWithBuilderExtension);
    builder.tpiWithNestedBuilder(tpiWithNestedBuilder);

    FirstPartyWithBuilderExtension.Builder fpWithBuilderExtensionBuilder =
        builder.fpWithBuilderExtensionBuilder()
            .value("first party through attributeBuilder");

    ImmutableNeapolitanAttributeBuilderParent copy = ImmutableNeapolitanAttributeBuilderParent
        .copyOf(builder.build());
    check(copy.fpWithBuilderExtension().value())
        .is(FirstPartyWithBuilderExtension.EXTENSION_OVERRIDE
            + "first party through attributeBuilder");
  }

  @Test
  public void testThirdPartyApiWithNestedBuilder() {
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
    ThirdPartyImmutableWithNestedBuilder tpiWithNestedBuilder = new ThirdPartyImmutableWithNestedBuilder.Builder()
        .setValue("third party")
        .doTheBuild();

    builder.tpiWithValueInstanceCopyMethod(tpiWithValueInstanceCopyMethod);
    builder.tpiWithValueClassCopyMethod(tpiWithValueClassCopyMethod);
    builder.tpiWithBuilderInstanceCopyMethod(tpiWithBuilderInstanceCopyMethod);
    builder.tpiWithBuilderClassCopyMethod(tpiWithBuilderClassCopyMethod);
    builder.fpWithBuilderExtension(fpWithBuilderExtension);
    //builder.tpiWithNestedBuilder(tpiWithNestedBuilder);

    ThirdPartyImmutableWithNestedBuilder.Builder tpiWithNestedBuilderBuilder =
        builder.tpiWithNestedBuilderBuilder()
            .setValue("third party through attributeBuilder");

    ImmutableNeapolitanAttributeBuilderParent copy = ImmutableNeapolitanAttributeBuilderParent
        .copyOf(builder.build());
    check(copy.tpiWithNestedBuilder().getValue())
        .is("third party through attributeBuilder");
  }

  @Test
  public void newKeywordNeededForNestedBuilder() {
    boolean thrown = false;
    String thirdPartyBuilder = "thirdPartyBuilder";

    try {
      check(ImmutableNewTokenAttributeBuilderParent.Builder.class
          .getMethod(thirdPartyBuilder, null));
    } catch (NoSuchMethodException e) {
      check("Could not find method when it should have been generated", false);
    }
    try {
      check(ImmutableNoNewTokenAttributeBuilderParent.Builder.class
          .getMethod(thirdPartyBuilder, null));
    } catch (NoSuchMethodException e) {
      thrown = true;
    }

    check("Generated nested builder when we should not have", thrown);
  }

}
