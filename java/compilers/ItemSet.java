package compilers;

import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.Objects;

class ItemSet implements Cloneable {
	private Set<Item> items;
	private static List<ItemSet> workList = new LinkedList<>();

	ItemSet() {
		items = new LinkedHashSet<>();
	}

	ItemSet(List<CFG.Rule> rules) {
		this();
		rules.forEach(r -> items.add(new Item(r, 0)));
	}

	@Override
	protected Object clone() {
		ItemSet copy = new ItemSet();
		//copy.items.addAll(items);
		items.forEach(i -> copy.items.add((Item) i.clone()));
		return copy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Item i : items)
			sb.append(i)
				.append("; ");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (o.getClass() != this.getClass())
			return false;
		ItemSet set = (ItemSet) o;
		return Objects.equals(set.items, this.items);
	}

	@Override
	public int hashCode() {
		return Objects.hash(items);
	}

	private ItemSet closure(CFG grammar) {
		ItemSet copy = (ItemSet) clone(), prev = null;
		while (!copy.equals(prev)) {
			prev = (ItemSet) copy.clone();
			Set<Item> temp = new LinkedHashSet<>(copy.items);
			temp.removeIf(i -> i.next() == null || !i.next().isNonTerminal());
			temp.forEach(i -> copy.items.addAll(grammar.productions(i.next())
						.map(r -> new Item(r, 0))
						.collect(Collectors.toSet())));
		}
		return copy;
	}

	private void goTo(CFG grammar, List<ItemSet> states) {
		ItemSet closed = closure(grammar);
		addState(states, closed);
		Stream.concat(grammar.terminals(), grammar.nonterminals())
			.forEach(s -> {
				ItemSet rel = closed.advanceMarkers(s);
				if (!rel.items.isEmpty())
					addState(states, rel);
			});
	}

	private ItemSet advanceMarkers(Symbol symbol) {
		ItemSet advanced = new ItemSet();
		for (Item i : items) {
			if (symbol.equals(i.next()) || Symbol.EOF == i.next()) {
				Item copy = (Item) i.clone();
				copy.advance();
				advanced.items.add(copy);
			}
		}
		return advanced;
	}

	static void generate(CFG grammar, List<ItemSet> states) {
		while (!workList.isEmpty()) {
			ItemSet state = workList.remove(0);
			state.goTo(grammar, states);
		}
	}

	static void addState(List<ItemSet> states, ItemSet state) {
		if (!states.contains(state)) {
			states.add(state);
			workList.add(state);
		}
	}

	static class Item implements Cloneable {
		private CFG.Rule production;
		private int marker; // index of the RHS which the marker appears *before*

		Item(CFG.Rule rule, int index) {
			production = rule;
			marker = index;
		}

		boolean isFresh() {
			return marker == 0;
		}

		boolean isReducible() {
			return production.isLambda() || 
				marker == production.getRight().size();
		}

		Symbol next() {
			if (isReducible())
				return null;
			return production.getRight().get(marker);
		}

		void advance() {
			marker++;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (o == this)
				return true;
			if (o.getClass() != this.getClass())
				return false;
			Item item = (Item) o;
			return Objects.equals(item.production, this.production) &&
				item.marker == this.marker;
		}

		@Override
		public int hashCode() {
			return Objects.hash(production, marker);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(production.getLeft())
				.append(" ->");
			for (int i = 0; i < production.getRight().size(); i++) {
				sb.append(" ");
				if (i == marker)
					sb.append(". ");
				sb.append(production.getRight().get(i));
			}
			if (isReducible())
				sb.append(" . ");
			return sb.toString();
		}

		@Override
		protected Object clone() {
			return new Item(production, marker);
		}
	}
}
