package compilers;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ListIterator;

class ParseTree {
	private ParseNode root;
	static final Symbol MARKER = new Symbol("*");
	static final Symbol START = new Symbol("S");

	ParseTree(CFG grammar, ParseTable table, Deque<Token> input) throws Exception {
		root = new ParseNode();
		ParseNode cur = root;
		ArrayDeque<Symbol> symbols = new ArrayDeque<>();
		symbols.push(START);
		while (!symbols.isEmpty()) {
			Symbol s = symbols.pop();
			if (s.isNonTerminal()) {
				CFG.Rule rule = table.get(s, input.peek());
				if (rule = null)
					throw new Exception();
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
					if (!s.matchToken(input.peek()))
						throw new Exception();
					input.pop();
				}
				cur.addChild(new ParseNode(s));
			} else if (s == MARKER)
				cur = cur.getParent();
		}
	}
}
