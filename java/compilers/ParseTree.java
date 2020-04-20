package compilers;

import compilers.util.*;
import java.util.*;
import java.text.ParseException;

class ParseTree {
	private static final Symbol MARKER = Symbol.of("*");

	private ParseTree() {}
	
	static ParseNode topDownParse(CFG grammar, InputQueue input) throws ParseException {
		Map<Tuple<Symbol, Symbol>, CFG.Rule> table = grammar.buildLLParseTable();
		ParseNode root = new ParseNode(), cur = root;
		ArrayDeque<Symbol> symbols = new ArrayDeque<>();
		symbols.push(Symbol.START);
		while (!symbols.isEmpty()) {
			Symbol s = symbols.pop();
			if (s.isNonTerminal()) {
				CFG.Rule rule = table.get(Tuple.of(s, input.peek()));
				if (rule == null)
					throw new ParseException(input.peek() + ": cannot find rule for nonterminal: " + s, 0);
				symbols.push(MARKER);
				List<Symbol> rhs = rule.getRight();
				ListIterator<Symbol> it = rhs.listIterator(rhs.size());
				while (it.hasPrevious())
					symbols.push(it.previous());
				ParseNode node = new ParseNode(s);
				cur.addChild(node);
				cur = node;
			} else if (s.isAugmentedSigma() || s.equals(Symbol.LAMBDA)) {
				if (s.isAugmentedSigma()) {
					if (!s.equals(input.peek()))
						throw new ParseException(input.peek() + ": does not match expected terminal: " + s, 0);
					if (s != Symbol.EOF)
						input.poll();
				}
				cur.addChild(new ParseNode(s));
			} else if (s == MARKER)
				cur = cur.getParent();
		}
		return root;
	}

	static class ParseNode {
		private Symbol identifier;
		private ParseNode parent;
		private List<ParseNode> children;

		ParseNode() {
			identifier = Symbol.of("");
			parent = null;
			children = new LinkedList<>();
		}

		ParseNode(Symbol symbol) {
			identifier = symbol;
			parent = null;
			children = new LinkedList<>();
		}

		ParseNode getParent() {
			return parent;
		}

		void addChild(ParseNode child) {
			child.parent = this;
			children.add(child);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(identifier)
				.append(" -> ")
				.append(children)
				.append("\n");
			return sb.toString();
		}
	}
}
