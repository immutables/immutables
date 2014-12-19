/*
    Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.generator.processor;

import com.google.common.collect.ImmutableList;
import org.immutables.generator.processor.ImmutableTrees.Unit;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

public class RunParser {
  static String input0 =
      "[template our M x] [/template]  [-- Comment --] [template ff] [if a]sd[let s M... s]fsd[g]sdg\n\nsdg[/let][else if not b][let x]sdg\nsd[gds yyy]UUU[/gds]dgsdg[/let][else] [/if][/template]";

  static String input = ""
      + "\n  [template name String param ,Integer p2]"
      + "\n  some text"
      + "\n    [-- param--]"
      + "\n  other[params] text[let s]UUU[s]U[param]XXXX[/let]ddd[g]"
      + "\n[/template]";

  static final String input2 = "[template x][if x][else][/if][/template]";

  public static void main(String... args) {

    Parser templateParser = Parboiled.createParser(Parser.class);
    ParsingResult<Object> result = new ReportingParseRunner<>(templateParser.Unit()).run(input2);

    ImmutableList<Object> copy = ImmutableList.copyOf(result.valueStack.iterator());

    if (!copy.isEmpty()) {
      Unit unit = (Unit) copy.get(0);

      Unit balance = Balancing.balance(unit);
      System.out.println(balance);
    }

    if (result.hasErrors()) {
      System.err.println(ErrorUtils.printParseErrors(result.parseErrors));
    }
    // System.out.println(ParseTreeUtils.printNodeTree(result));
  }
}
