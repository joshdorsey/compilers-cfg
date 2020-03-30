package compilers;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.Objects;

class ItemSet implements Cloneable {
	private Set<Item> items;

	ItemSet() {
		items = new LinkedHashSet<>();
	}

	@Override
	protected Object clone() {
		ItemSet copy = new ItemSet();
		copy.items.addAll(this.items);
		return copy;
	}

	ItemSet closure(CFG grammar) {
		ItemSet copy = (ItemSet) clone();
		boolean changed = true;
		while (changed) {
			for (Item i : items) {
				Symbol s = i.next();
				if (s.isNonTerminal())
					changed = items.addAll(grammar.productions(s)
							.map(r -> new Item(r, 0))
							.collect(Collectors.toSet()));
			}
		}
		return copy;
	}

	ItemSet goTo(CFG grammar, Symbol symbol) {
		ItemSet copy = (ItemSet) clone();
		copy.items.removeIf(i -> !i.next().equals(symbol));
		copy.items.forEach(i -> i.advance());
		return copy.closure(grammar);
	}

	static class Item {
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
			return marker == production.getRight().size();
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
			return item.production.equals(this.production) &&
				item.marker == this.marker;
		}

		@Override
		public int hashCode() {
			return Objects.hash(production, marker);
		}
	}
}
