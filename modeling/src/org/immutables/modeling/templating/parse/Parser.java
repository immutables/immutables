package org.immutables.modeling.templating.parse;

import org.immutables.modeling.templating.ParboiledTrees.*;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.ExplicitActionsOnly;
import org.parboiled.annotations.MemoMismatches;
import org.parboiled.annotations.SkipNode;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

@BuildParseTree
@ExplicitActionsOnly
public class Parser extends BaseParser<Object> {

  Rule TemplateUnit() {
    return Sequence(Unit.builder(),
        Spacing(),
        OneOrMore(Sequence(
            TopDirective(),
            Spacing())),
        EOI, Unit.build());
  }

  @SkipNode
  Rule TopDirective() {
    return FirstOf(
        OpeningDirective(Comment()),
        TemplateDirective());
  }

  @SuppressNode
  Rule Comment() {
    return Sequence(COMMENT, TextBlock(), Extractions.popped());
  }

  Rule TextBlock() {
    return Sequence(TextBlock.builder(),
        TextFragment(), TextBlock.addParts(TextFragment.of()),
        ZeroOrMore(Sequence(
            Newline(), TextBlock.addParts(Newline.of()),
            TextFragment(), TextBlock.addParts(TextFragment.of()))),
        TextBlock.build());
  }

  @SuppressSubnodes
  Rule Newline() {
    return FirstOf("\n", "\n\r");
  }

  @SuppressSubnodes
  Rule TextFragment() {
    return ZeroOrMore(NoneOf("[]\n\r"));
  }

  Rule TemplateDirective() {
    return Sequence(Template.builder(),
        TemplateStart(),
        TemplateBody(),
        TemplateEnd(), Template.build(), Unit.addParts());
  }

  @SkipNode
  Rule TemplateStart() {
    return OpeningDirective(Template());
  }

  @SuppressSubnodes
  Rule TemplateEnd() {
    return ClosingDirective(TEMPLATE);
  }

  Rule TemplateBody() {
    return Sequence(
        TextBlock(), Template.addParts(),
        ZeroOrMore(
        Sequence(
            Directive(), Template.addParts(),
            TextBlock(), Template.addParts())));
  }

  Rule DirectiveStart() {
    return OpeningDirective(FirstOf(
        Comment(),
        Let(),
        If(),
        ElseIf(),
        Else(),
        For(),
        InvokeStart()));
  }

  Rule InvokeEnd() {
    return Sequence(InvokeEnd.builder(),
        AccessExpression(), InvokeEnd.access(),
        InvokeEnd.build());
  }

  Rule InvokeStart() {
    return Sequence(InvokeStart.builder(),
        AccessExpression(), InvokeStart.access(), InvokeExpression.builder(),
        Optional(Expression()),
        InvokeStart.invoke(), InvokeStart.build());
  }

  Rule DirectiveEnd() {
    return ClosingDirective(FirstOf(
        IfEnd(),
        LetEnd(),
        ForEnd(),
        InvokeEnd()));
  }

  Rule IfEnd() {
    return Sequence(IF, IfEnd.of());
  }

  Rule LetEnd() {
    return Sequence(LET, LetEnd.of());
  }

  Rule ForEnd() {
    return Sequence(FOR, ForEnd.of());
  }

  Rule Directive() {
    return FirstOf(
        DirectiveEnd(),
        DirectiveStart());
  }

  Rule OpeningDirective(Rule directive) {
    return Sequence("[", directive, "]");
  }

  Rule ClosingDirective(Rule directive) {
    return Sequence("[/", directive, "]");
  }

  @SkipNode
  Rule Parens(Rule expression) {
    return Sequence(Literal("("), expression, Literal(")"));
  }

  Rule AccessExpression() {
    return Sequence(AccessExpression.builder(),
        Identifier(), AccessExpression.addPath(Identifier.of()),
        ZeroOrMore(Sequence(DOT,
            Identifier(), AccessExpression.addPath(Identifier.of()))),
        AccessExpression.build());
  }

  Rule GeneratorDeclaration() {
    return FirstOf(
        AssignGenerator(),
        IterationGenerator());
  }

  Rule IterationGenerator() {
    return Sequence(IterationGenerator.builder(),
        ValueDeclaration(), IterationGenerator.declaration(),
        IN,
        Expression(), IterationGenerator.from(),
        Optional(If(), IterationGenerator.condition()),
        IterationGenerator.build());
  }

  Rule AssignGenerator() {
    return Sequence(AssignGenerator.builder(),
        ValueDeclaration(), AssignGenerator.declaration(),
        ASSIGN,
        Expression(), AssignGenerator.from(),
        AssignGenerator.build());
  }

  @SkipNode
  @DontLabel
  Rule DisambiguatedExpression() {
    return FirstOf(
        Parens(InvokeExpression()),
        AccessExpression());
  }

  @SkipNode
  Rule Expression() {
    return FirstOf(
        Parens(InvokeExpression()),
        InvokeExpression());
  }

  @SkipNode
  Rule InvokeExpression() {
    return Sequence(InvokeExpression.builder(),
        OneOrMore(DisambiguatedExpression(), InvokeExpression.addParams()),
        InvokeExpression.build());
  }

  Rule If() {
    return Sequence(
        IF, If.builder(),
        Expression(), If.condition(),
        If.build());
  }

  Rule ElseIf() {
    return Sequence(
        ELSE, IF, ElseIf.builder(),
        Expression(), ElseIf.condition(),
        ElseIf.build());
  }

  Rule Else() {
    return Sequence(ELSE, Else.of());
  }

  Rule For() {
    return Sequence(
        FOR, For.builder(),
        ForDeclaration(),
        For.build());
  }

  Rule ForDeclaration() {
    return Sequence(GeneratorDeclaration(), For.addDeclaration(),
        ZeroOrMore(Sequence(COMMA,
            GeneratorDeclaration(), For.addDeclaration())));
  }

  Rule Let() {
    return Sequence(
        LET, Let.builder(),
        InvokableDeclaration(), Let.declaration(),
        Let.build());
  }

  Rule ValueDeclaration() {
    return Sequence(ValueDeclaration.builder(),
        Optional(Type(), ValueDeclaration.type()),
        Name(), ValueDeclaration.name(),
        ValueDeclaration.build());
  }

  Rule InvokableDeclaration() {
    return Sequence(InvokableDeclaration.builder(),
        Name(), InvokableDeclaration.name(),
        ParameterDeclarations(),
        InvokableDeclaration.build());
  }

  Rule ParameterDeclaration() {
    return Sequence(Parameter.builder(),
        Type(), Parameter.type(),
        Name(), Parameter.name(),
        Parameter.build(),
        InvokableDeclaration.addParameters());
  }

  @SkipNode
  Rule Template() {
    return Sequence(
        TEMPLATE,
        InvokableDeclaration(), Template.declaration());
  }

  @SkipNode
  Rule ParameterDeclarations() {
    return Optional(Sequence(ParameterDeclaration(),
        ZeroOrMore(Sequence(COMMA,
            ParameterDeclaration()))));
  }

  @SuppressSubnodes
  Rule Name() {
    return Sequence(
        Identifier(),
        Identifier.of());
  }

  @SuppressSubnodes
  Rule Type() {
    return Sequence(
        TypeReference(),
        TypeReference.of());
  }

  Rule COMMENT = Literal("--");
  Rule ASSIGN = Literal("=");
  Rule DOT = Literal(".");
  Rule COMMA = Literal(",");
  Rule IN = Literal(KEYWORD_IN);
  Rule FOR = Literal(KEYWORD_FOR);
  Rule LET = Literal(KEYWORD_LET);
  Rule IF = Literal(KEYWORD_IF);
  Rule ELSE = Literal(KEYWORD_ELSE);
  Rule TEMPLATE = Literal(KEYWORD_TEMPLATE);
//  Rule REQUIRE = Literal("require");

//  private static final String KEYWORD_AS = "as";
  private static final String KEYWORD_IN = "in";
  private static final String KEYWORD_FOR = "for";
  private static final String KEYWORD_LET = "let";
  private static final String KEYWORD_IF = "if";
  private static final String KEYWORD_ELSE = "else";
  private static final String KEYWORD_TEMPLATE = "template";

  @MemoMismatches
  @SuppressNode
  Rule Keyword() {
    return Sequence(
        FirstOf(KEYWORD_IN, KEYWORD_FOR, KEYWORD_LET, KEYWORD_IF, KEYWORD_ELSE, KEYWORD_TEMPLATE),
        TestNot(LetterOrDigit()));
  }

  @DontLabel
  @SuppressSubnodes
  Rule Literal(String string) {
    return Sequence(
        String(string),
        Spacing());
  }

  @SuppressSubnodes
  @MemoMismatches
  Rule Identifier() {
    return Sequence(
        TestNot(Keyword()),
        Sequence(IdentifierStartLetter(), ZeroOrMore(LetterOrDigit())),
        Spacing());
  }

  @SuppressSubnodes
  @MemoMismatches
  Rule TypeReference() {
    return Sequence(TestNot(Keyword()), TypeStartLetter(), ZeroOrMore(LetterOrDigit()), Spacing());
  }

  Rule IdentifierStartLetter() {
    return FirstOf(CharRange('a', 'z'), '_');
  }

  Rule TypeStartLetter() {
    return CharRange('A', 'Z');
  }

  Rule Letter() {
    return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_', '$');
  }

  @MemoMismatches
  Rule LetterOrDigit() {
    return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), CharRange('0', '9'), '_', '$');
  }

  @SuppressNode
  Rule Spacing() {
    return ZeroOrMore(AnyOf(" \n\r\f\t"));
  }

}
