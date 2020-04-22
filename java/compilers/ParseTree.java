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

	static ParseNode bottomUpParse(CFG grammar, InputQueue input) throws ParseException {
		grammar.buildLRItemSets();
		Deque<ParseNode> parseQueue = convertTokens(input);
		Deque<Tuple<Integer, ParseNode>> parseStack = new ArrayDeque<>();
		parseStack.push(Tuple.of(0, new ParseNode()));
		while (!parseQueue.isEmpty()) {
			ParseNode next = parseQueue.peek();
			Action act = ItemSet.actionTable.get(parseStack.peek().getFirst()).get(next.identifier);
			if (act == null)
				throw new ParseException("No suitable action for token: " + next.identifier, 0);
			if (act instanceof Action.Shift) {
				parseStack.push(Tuple.of(((Action.Shift)act).getStateNum(), next));
				if (!next.identifier.equals(Symbol.EOF))
					parseQueue.pop();
			} else if (act instanceof Action.Reduce) {
				if (act.isAccepting()) {
					ParseNode root = new ParseNode(Symbol.START);
					Iterator<Tuple<Integer, ParseNode>> remaining = parseStack.descendingIterator();
					while (remaining.hasNext())
						root.addChild(remaining.next().getSecond());
					return root;
				}
				ParseNode reduced = new ParseNode(((Action.Reduce)act).getRule().getLeft());
				reduced.addChild(parseStack.pop().getSecond());
				parseQueue.push(reduced);
			}
		}
		throw new ParseException("Parse terminated without reaching accepting state", 0);
	}

	private static Deque<ParseNode> convertTokens(InputQueue tokens) {
		Deque<ParseNode> converted = new ArrayDeque<>(tokens.size());
		for (Symbol s : tokens)
			converted.offer(new ParseNode(s));
		converted.offer(new ParseNode(Symbol.EOF));
		return converted;
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
