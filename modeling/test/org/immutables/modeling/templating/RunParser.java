package org.immutables.modeling.templating;

import com.google.common.collect.ImmutableList;
import org.immutables.modeling.templating.Balancing;
import org.immutables.modeling.templating.ImmutableTrees.Unit;
import org.immutables.modeling.templating.Parser;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

public class RunParser {
  static String input0 =
      "[template our M x] [/template]  [-- Comment --] [template ff] [if a]sd[let s M... s]fsd[g]sdg\n\nsdg[/let][else if not b][let x]sdg\nsd[gds yyy]UUU[/gds]dgsdg[/let][else] [/if][/template]";

  static String input = ""
      + "\n  [template name String param]"
      + "\n  some text"
      + "\n    [-- param--]"
      + "\n  other[params] text[let s]UUU[s]U[param]XXXX[/let]ddd[g]"
      + "\n[/template]";

  public static void main(String... args) {

    Parser templateParser = Parboiled.createParser(Parser.class);
    ParsingResult<Object> result = new ReportingParseRunner<>(templateParser.Unit()).run(input);

    ImmutableList<Object> copy = ImmutableList.copyOf(result.valueStack.iterator());

    if (!copy.isEmpty()) {
      Unit unit = (Unit) copy.get(0);

      Unit balance = Balancing.balance(unit);
    }

    if (result.hasErrors()) {
      System.err.println(ErrorUtils.printParseErrors(result.parseErrors));
    }
    // System.out.println(ParseTreeUtils.printNodeTree(result));
  }
}
