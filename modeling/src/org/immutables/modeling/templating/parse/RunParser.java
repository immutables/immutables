package org.immutables.modeling.templating.parse;

import com.google.common.collect.ImmutableList;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class RunParser {
  public static void main(String... args) {
    String input =
        "  [-- Comment --][template ff] [if a]sd[let s M s]fsdgsdg\n\nsdg[else if not b][let x]sdg\nsdgdsgsdgsdg[/let][else] [/template]";
    Parser templateParser = Parboiled.createParser(Parser.class);
    ParsingResult<Object> result = new ReportingParseRunner<>(templateParser.TemplateUnit()).run(input);

    ImmutableList<Object> copy = ImmutableList.copyOf(result.valueStack.iterator());

    System.err.println("!!! " + copy);
    if (result.hasErrors()) {
      System.err.println(ErrorUtils.printParseErrors(result.parseErrors));
    }
    System.out.println(ParseTreeUtils.printNodeTree(result));
  }
}
