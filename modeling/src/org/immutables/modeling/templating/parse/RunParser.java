package org.immutables.modeling.templating.parse;

import org.immutables.modeling.templating.Balancing;
import org.immutables.modeling.templating.ImmutableTrees.Unit;
import com.google.common.collect.ImmutableList;
import org.immutables.modeling.templating.Parser;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

public class RunParser {
  public static void main(String... args) {
    String input =
        "  [-- Comment --][template ff] [if a]sd[let s M s]fsd[g]sdg\n\nsdg[/let][else if not b][let x]sdg\nsd[gds yyy]UUU[/gds]dgsdg[/let][else] [/if][/template]";
    Parser templateParser = Parboiled.createParser(Parser.class);
    ParsingResult<Object> result = new ReportingParseRunner<>(templateParser.Unit()).run(input);

    ImmutableList<Object> copy = ImmutableList.copyOf(result.valueStack.iterator());

    if (!copy.isEmpty()) {
      Unit unit = (Unit) copy.get(0);

      Unit balance = Balancing.balance(unit);

      System.err.println("!!! " + balance);
    }
    if (result.hasErrors()) {
      System.err.println(ErrorUtils.printParseErrors(result.parseErrors));
    }
    // System.out.println(ParseTreeUtils.printNodeTree(result));
  }
}
