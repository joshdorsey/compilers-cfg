package compilers;

import compilers.util.*;
import java.util.*;

class ParseTree {
	ParseNode root;
	private static final Symbol MARKER = Symbol.of("*");

	ParseTree(CFG grammar, InputQueue input) throws Exception {
		HashMap<Tuple<Symbol, Symbol>, CFG.Rule> table = grammar.buildLLParseTable();
		root = new ParseNode();
		ParseNode cur = root;
		ArrayDeque<Symbol> symbols = new ArrayDeque<>();
		symbols.push(Symbol.START);
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
			} else if (s.isAugmentedSigma() || s.equals(Symbol.LAMBDA)) {
				if (s.isAugmentedSigma()) {
					if (!s.equals(input.peek()))
						throw new Exception(input.peek() + ": does not match expected terminal: " + s);
					if (s != Symbol.EOF)
						input.poll();
				}
				cur.addChild(new ParseNode(s));
			} else if (s == MARKER)
				cur = cur.getParent();
		}
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
