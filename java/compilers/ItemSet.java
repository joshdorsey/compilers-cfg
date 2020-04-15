package compilers;

import java.util.*;
import java.util.stream.Collectors;

class ItemSet implements Cloneable {
	private Set<Item> items;
	private static List<ItemSet> workList = new LinkedList<>();
	static List<Map<Symbol, Action>> actionTable = new ArrayList<>();

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

	private boolean isComplete() {
		if (items.size() != 1)
			return false;
		return items.stream().allMatch(Item::isReducible);
	}

	private boolean isGoal() {
		if (items.size() != 1)
			return false;
		return items.stream().allMatch(i -> i.isReducible() && i.production.isEnd());
	}

	private boolean allowsLambda() {
		return items.stream().anyMatch(i -> i.production.isLambda());
	}

	private CFG.Rule reduction() {
		for (Item i : items)
			return i.production;
		return null;
	}

	private CFG.Rule lambdaReduction() {
		for (Item i : items)
			if (i.production.isLambda())
				return i.production;
		return null;
	}

	private ItemSet closure(CFG grammar) {
		ItemSet copy = (ItemSet) clone(), prev = null;
		while (!copy.equals(prev)) {
			prev = (ItemSet) copy.clone();
			Set<Item> temp = new HashSet<>(copy.items);
			temp.removeIf(i -> i.isReducible() || !i.next().isNonTerminal());
			temp.forEach(i -> copy.items.addAll(grammar.productions(i.next())
						.map(r -> new Item(r, 0))
						.collect(Collectors.toSet())));
		}
		return copy;
	}

	private void goTo(CFG grammar, List<ItemSet> states) {
		ItemSet closed = closure(grammar);
		final int fromIndex = addState(states, closed, true);
		grammar.getSymbolList().forEach(s -> {
				ItemSet rel = closed.advanceMarkers(s).closure(grammar);
				if (!rel.items.isEmpty()) {
					int toIndex = addState(states, rel, true);
					actionTable.get(fromIndex).put(s,
						       	new Action.Shift(toIndex));
				}
			});
	}

	private ItemSet advanceMarkers(Symbol symbol) {
		ItemSet advanced = new ItemSet();
		for (Item i : items) {
			if (symbol.equals(i.next())) {
				Item copy = (Item) i.clone();
				copy.advance();
				advanced.items.add(copy);
			}
		}
		return advanced;
	}

	static void generate(CFG grammar, List<ItemSet> states) {
		ItemSet state;
		while (!workList.isEmpty()) {
			state = workList.remove(0);
			state.goTo(grammar, states);
		}
		for (int i = 0; i < states.size(); i++) {
			state = states.get(i);
			CFG.Rule reduceWith;
			if (state.isGoal()) {
				// TODO make separate method in CFG to get starting production
				// also consider verifying that there is only one starting
				// production in the grammar
				reduceWith = grammar.getProductions(Symbol.START).get(0);
				for (Symbol s : grammar.getSymbolList())
					actionTable.get(i).put(s,
							new Action.Reduce(reduceWith, true));
			} else if (state.isComplete() || state.allowsLambda()) {
				reduceWith = state.allowsLambda() ? state.lambdaReduction() :
				       	state.reduction();
				for (Symbol f : grammar.followSet(reduceWith.getLeft()))
					actionTable.get(i).put(f,
						       	new Action.Reduce(reduceWith, false));
			}
		}
	}

	static int addState(List<ItemSet> states, ItemSet state, boolean finalState) {
		if (!states.contains(state)) {
			if (finalState) {
				states.add(state);
				actionTable.add(new HashMap<>());
			}
			workList.add(state);
		}
		return states.indexOf(state);
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
			if (!isReducible())
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
