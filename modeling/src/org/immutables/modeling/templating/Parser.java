package org.immutables.modeling.templating;

import org.immutables.modeling.templating.ParboiledTrees.AccessExpression;
import org.immutables.modeling.templating.ParboiledTrees.ApplyExpression;
import org.immutables.modeling.templating.ParboiledTrees.AssignGenerator;
import org.immutables.modeling.templating.ParboiledTrees.Else;
import org.immutables.modeling.templating.ParboiledTrees.ElseIf;
import org.immutables.modeling.templating.ParboiledTrees.For;
import org.immutables.modeling.templating.ParboiledTrees.ForEnd;
import org.immutables.modeling.templating.ParboiledTrees.Identifier;
import org.immutables.modeling.templating.ParboiledTrees.If;
import org.immutables.modeling.templating.ParboiledTrees.IfEnd;
import org.immutables.modeling.templating.ParboiledTrees.InvokableDeclaration;
import org.immutables.modeling.templating.ParboiledTrees.Invoke;
import org.immutables.modeling.templating.ParboiledTrees.InvokeEnd;
import org.immutables.modeling.templating.ParboiledTrees.IterationGenerator;
import org.immutables.modeling.templating.ParboiledTrees.Let;
import org.immutables.modeling.templating.ParboiledTrees.LetEnd;
import org.immutables.modeling.templating.ParboiledTrees.Newline;
import org.immutables.modeling.templating.ParboiledTrees.Parameter;
import org.immutables.modeling.templating.ParboiledTrees.Template;
import org.immutables.modeling.templating.ParboiledTrees.TextBlock;
import org.immutables.modeling.templating.ParboiledTrees.TextFragment;
import org.immutables.modeling.templating.ParboiledTrees.TypeDeclaration;
import org.immutables.modeling.templating.ParboiledTrees.TypeIdentifier;
import org.immutables.modeling.templating.ParboiledTrees.Unit;
import org.immutables.modeling.templating.ParboiledTrees.ValueDeclaration;
import org.immutables.modeling.templating.ParboiledTrees.Comment;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.ExplicitActionsOnly;
import org.parboiled.annotations.MemoMismatches;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

@ExplicitActionsOnly
public class Parser extends BaseParser<Object> {

  public Rule Unit() {
    return Sequence(Unit.builder(),
        Spacing(),
        OneOrMore(Sequence(
            FirstOf(
                OpeningDirective(Comment()),
                Sequence(TemplateDirective(), Unit.addParts())),
            Spacing())),
        EOI, Unit.build());
  }

  Rule Comment() {
    return Sequence(COMMENT, Optional(TextBlock(), Extractions.popped()), Comment.of());
  }

  Rule TextBlock() {
    return Sequence(TextBlock.builder(),
        TextFragment(), TextBlock.addParts(TextFragment.of()),
        ZeroOrMore(Sequence(
            Newline(), TextBlock.addParts(Newline.of()),
            TextFragment(), TextBlock.addParts(TextFragment.of()))),
        TextBlock.build());
  }

  Rule TemplateDirective() {
    return Sequence(Template.builder(),
        TemplateStart(),
        TemplateBody(),
        TemplateEnd(), Template.build());
  }

  Rule TemplateStart() {
    return OpeningDirective(Template());
  }

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
    return Sequence(AccessExpression(), InvokeEnd.of());
  }

  Rule InvokeStart() {
    return Sequence(Invoke.builder(),
        AccessExpression(), Invoke.access(),
        Optional(Expression(), Invoke.invoke()),
        Invoke.build());
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

  Rule Parens(Rule expression) {
    return Sequence(Literal("("), expression, Literal(")"));
  }

  Rule AccessExpression() {
    return Sequence(AccessExpression.builder(),
        Identifier(), AccessExpression.addPath(),
        ZeroOrMore(Sequence(DOT,
            Identifier(), AccessExpression.addPath())),
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
        Optional(IF, Expression(), IterationGenerator.condition()),
        IterationGenerator.build());
  }

  Rule AssignGenerator() {
    return Sequence(AssignGenerator.builder(),
        ValueDeclaration(), AssignGenerator.declaration(),
        ASSIGN,
        Expression(), AssignGenerator.from(),
        AssignGenerator.build());
  }

  @DontLabel
  Rule DisambiguatedExpression() {
    return FirstOf(
        Parens(ApplyExpression()),
        AccessExpression());
  }

  Rule Expression() {
    return FirstOf(
        Parens(ApplyExpression()),
        ApplyExpression());
  }

  Rule ApplyExpression() {
    return Sequence(ApplyExpression.builder(),
        OneOrMore(DisambiguatedExpression(), ApplyExpression.addParams()),
        ApplyExpression.build());
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

  Rule Template() {
    return Sequence(
        TEMPLATE,
        InvokableDeclaration(), Template.declaration());
  }

  Rule ParameterDeclarations() {
    return Optional(Sequence(ParameterDeclaration(),
        ZeroOrMore(Sequence(COMMA,
            ParameterDeclaration()))));
  }

  Rule Name() {
    return Identifier();
  }

  Rule Type() {
    return Sequence(TypeDeclaration.builder(),
        TypeIdentifer(), TypeDeclaration.type(),
        Optional(Ellipsis(), TypeDeclaration.kind(Trees.TypeDeclaration.Kind.ITERABLE)),
        TypeDeclaration.build());
  }

  Rule COMMENT = Literal("--");
  Rule ASSIGN = Literal("=");
  Rule DOT = Literal(".");
  Rule COMMA = Literal(",");
  Rule ELLIPSIS = Literal("...");
  Rule IN = Literal(KEYWORD_IN);
  Rule FOR = Literal(KEYWORD_FOR);
  Rule LET = Literal(KEYWORD_LET);
  Rule IF = Literal(KEYWORD_IF);
  Rule ELSE = Literal(KEYWORD_ELSE);
  Rule TEMPLATE = Literal(KEYWORD_TEMPLATE);

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
        Sequence(Sequence(IdentifierStartLetter(), ZeroOrMore(LetterOrDigit())), Identifier.of()),
        Spacing());
  }

  @SuppressSubnodes
  @MemoMismatches
  Rule TypeIdentifer() {
    return Sequence(TestNot(Keyword()),
        Sequence(Sequence(TypeStartLetter(), ZeroOrMore(LetterOrDigit())), TypeIdentifier.of()),
        Spacing());
  }

  @SuppressSubnodes
  Rule Newline() {
    return FirstOf("\n", "\n\r");
  }

  @SuppressSubnodes
  Rule TextFragment() {
    return ZeroOrMore(NoneOf("[]\n\r"));
  }

  @SuppressSubnodes
  Rule Ellipsis() {
    return Sequence(ELLIPSIS, Spacing());
  }

  Rule IdentifierStartLetter() {
    return FirstOf(CharRange('a', 'z'), '_');
  }

  Rule TypeStartLetter() {
    return CharRange('A', 'Z');
  }

  @MemoMismatches
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
