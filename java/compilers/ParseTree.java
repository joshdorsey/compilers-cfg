package compilers;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ListIterator;
import java.util.HashMap;

import compilers.util.Tuple;

class ParseTree {
	ParseNode root;
	static final Symbol MARKER = new Symbol("*");
	static final Symbol START = new Symbol("S");

	ParseTree(CFG grammar, Deque<Symbol> input) throws Exception {
		HashMap<Tuple<Symbol, Symbol>, CFG.Rule> table = grammar.buildLLParseTable();
		root = new ParseNode();
		ParseNode cur = root;
		ArrayDeque<Symbol> symbols = new ArrayDeque<>();
		symbols.push(START);
		while (!symbols.isEmpty()) {
			Symbol s = symbols.pop();
			if (s.isNonTerminal()) {
				CFG.Rule rule = table.get(Tuple.of(s, input.peek()));
				if (rule == null)
					throw new Exception(input.peek() + ": cannot find rule for nonterminal: " + s);
				symbols.push(MARKER);
				List<Symbol> rhs = rule.getRight();
				ListIterator<Symbol> it = rhs.listIterator(rhs.size());
				while (it.hasPrevious())
					symbols.push(it.previous());
				ParseNode node = new ParseNode(s);
				cur.addChild(node);
				cur = node;
			} else if (s.isTerminal() || s == Symbol.EOF || s == Symbol.LAMBDA) {
				if (s != Symbol.LAMBDA) {
					if (!s.equals(input.peek()))
						throw new Exception(input.peek() + ": does not match expected terminal: " + s);
					input.pop();
				}
				cur.addChild(new ParseNode(s));
			} else if (s == MARKER)
				cur = cur.getParent();
		}
	}
}
