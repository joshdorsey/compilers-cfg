package compilers;

import compilers.util.Tuple;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.list.SetUniqueList;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CFG {
    private ArrayListValuedHashMap<Symbol, LinkedList<Symbol>> map;

    CFG(ArrayListValuedHashMap<Symbol, LinkedList<Symbol>> map) {
        this.map = map;
    }

    //<editor-fold desc="Getters and Things-that-return-streams">
    Stream<Rule> productions() {
        return map.entries().stream()
                .map(e -> new Rule(e.getKey(), e.getValue()));
    }

    Stream<Rule> productions(Symbol lhs) {
        return productions().filter(rule -> rule.getLeft().equals(lhs));
    }

    Stream<Symbol> terminals() {
        return productions()
                .flatMap(Rule::symbols)
                .filter(Symbol::isTerminal)
                .distinct();
    }

    Stream<Symbol> nonterminals() {
        return productions()
                .flatMap(Rule::symbols)
                .filter(Symbol::isNonTerminal)
                .distinct();
    }

    List<Rule> getProductions() {
        return productions().collect(Collectors.toList());
    }

    List<Rule> getProductions(Symbol lhs) {
        return productions(lhs).collect(Collectors.toList());
    }

    Set<Symbol> getTerminals() {
        return terminals().collect(Collectors.toSet());
    }

    Set<Symbol> getNonTerminals() {
        return nonterminals().collect(Collectors.toSet());
    }

    List<Symbol> getSymbolList() {
	List<Symbol> list = new LinkedList<>();
	list.addAll(getTerminals());
	list.add(Symbol.EOF);
	list.addAll(getNonTerminals());
	list.remove(Symbol.START);
	return list;
    }
    //</editor-fold>

    //<editor-fold desc="Algorithms">
    boolean derivesToLambda(Symbol s, ArrayDeque<Tuple<Rule, Symbol>> recurseStack) {
        for (Rule rule : getProductions(s)) {
            // If this rule contains only lambda, we can short-circuit
            if (rule.isLambda()) {
                return true;
            }

            // If the rule contains terminals, or the EOF character, we can skip it
            if (rule.hasTerminals() || rule.isEnd()) {
                continue;
            }

            // This function makes a recursive call to this function, and guards against recursive loops.
            Predicate<Symbol> recurse = (nt) -> {
                Tuple<Rule, Symbol> stackItem = Tuple.of(rule, nt);

                // If we've already seen this item, skip it
                if (recurseStack.contains(stackItem)) {
                    return true;
                }

                // Otherwise, recurse and bookend the recursive call with the stack operations
                recurseStack.push(stackItem);
                boolean derivesToLambda = derivesToLambda(nt, recurseStack);
                recurseStack.pop();

                return derivesToLambda;
            };

            Stream<Symbol> ruleNonterminals = rule.getRight().stream().filter(Symbol::isNonTerminal);
            // If all the nonterminals in the RHS of this rule derive to lambda, then return true
            if (ruleNonterminals.allMatch(recurse)) {
                return true;
            }
        }

        return false;
    }

    boolean derivesToLambda(Symbol s) {
        return derivesToLambda(s, new ArrayDeque<>());
    }

    Set<Symbol> firstSet(List<Symbol> seq, Set<Symbol> firstSet) {
        if (seq.isEmpty()) {
            return new HashSet<>();
        }

        Symbol firstSymbol = seq.get(0);
        List<Symbol> rest = seq.subList(1, seq.size());

        if (Symbol.EOF.equals(firstSymbol) || getTerminals().contains(firstSymbol)) {
            return SetUtils.hashSet(firstSymbol);
        }

        Set<Symbol> updateSet = new HashSet<>();

        if (!firstSet.contains(firstSymbol)) {
            firstSet.add(firstSymbol);
            for (Rule rule : getProductions(firstSymbol)) {
                updateSet.addAll(firstSet(rule.getRight(), firstSet));
            }
        }

        if (derivesToLambda(firstSymbol)) {
            updateSet.addAll(firstSet(rest, firstSet));
        }

        return updateSet;
    }

    Set<Symbol> firstSet(List<Symbol> seq) {
        return firstSet(seq, new HashSet<>());
    }

    Set<Symbol> followSet(Symbol nt, Set<Symbol> followSet) {
        if (followSet.contains(nt)) {
            return new HashSet<>();
        }

        followSet.add(nt);
        Set<Symbol> updateSet = new HashSet<>();

        // Get the RHSs of production rules that contain nt
        List<Rule> productions = productions()
                .filter(rule -> rule.getRight().contains(nt))
                .collect(Collectors.toList());

        for (Rule rule : productions) {
            List<Symbol> rhs = rule.getRight();
            Symbol lhs = rule.getLeft();
            for (int i = 0; i < rhs.size(); i++) {
                // If we're not at an instance of nt, there's nothing to do
                if (!rhs.get(i).equals(nt)) {
                    continue;
                }

                // If we are, consider the sequence of symbols after nt
                List<Symbol> tail = rhs.subList(i + 1, rhs.size());

                if (!tail.isEmpty()) {
                    Set<Symbol> firstSet = firstSet(tail);
                    updateSet.addAll(firstSet);
                }

                boolean noAugmentedSigma = tail.stream().noneMatch(Symbol::isAugmentedSigma);
                boolean allToLambda = tail.stream().allMatch(this::derivesToLambda);
                if (tail.isEmpty() || (noAugmentedSigma && allToLambda)) {
                    Set<Symbol> follow = followSet(lhs, followSet);
                    updateSet.addAll(follow);
                }
            }
        }

        return updateSet;
    }

    Set<Symbol> followSet(Symbol nt) {
        return followSet(nt, new HashSet<>());
    }

    Set<Symbol> predictSet(Rule r) {
        Set<Symbol> answer = firstSet(r.getRight());

        if (derivesToLambda(r.getLeft())) {
            answer.addAll(followSet(r.getLeft()));
        }

        return answer;
    }

    boolean predictSetsDisjoint() {
        for (Symbol nt : getNonTerminals()) {
            List<Rule> productions = getProductions(nt);
            for (int i = 0; i < productions.size(); i++) {
                for (int j = i + 1; j < productions.size(); j++) {
                    Rule a = productions.get(i);
                    Rule b = productions.get(j);

                    if (!Collections.disjoint(predictSet(a), predictSet(b))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
    //</editor-fold>

    HashMap<Tuple<Symbol, Symbol>, Rule> buildLLParseTable() {
        // if(!this.predictSetsDisjoint()) {
        //     System.out.println("Predict sets are not disjoint, cannot construct LL(1) parse table!");
        //     return new HashMap<Tuple<Symbol, Symbol>, Integer>();
        // }
        ArrayList<Tuple<Rule, Set<Symbol>>> predictSets = new ArrayList<Tuple<Rule, Set<Symbol>>>();
        this.productions().forEach(rule -> {
            predictSets.add(Tuple.of(rule, this.predictSet(rule)));
        });
        HashMap<Tuple<Symbol, Symbol>, Rule> parseTable = new HashMap<Tuple<Symbol, Symbol>, Rule>();
        for(Tuple<Rule, Set<Symbol>> pred : predictSets) {
	    Rule rule = pred.getFirst();
	    Symbol row = rule.getLeft();
            for(Symbol col : pred.getSecond()) {
                parseTable.put(Tuple.of(row, col), rule);
            }
        }
        return parseTable;
    }

    void printLLParseTable(HashMap<Tuple<Symbol, Symbol>, Rule> table) {
        ArrayList<Symbol> terms = new ArrayList<>(getTerminals());
        System.out.print("\t");
        for(Symbol s : terms) {
            System.out.print(s + "\t");
        }
        System.out.println();
        for(Symbol nonterm : this.getNonTerminals()) {
            System.out.print(nonterm + " | \t");
            for(Symbol term : terms) {
                Rule val = table.get(Tuple.of(nonterm, term));
                if(val != null) {
                    System.out.print(val + " | \t");
                } else {
                    System.out.print("# | \t");
                }
            }
            System.out.println();
        }
    }

    List<ItemSet> buildLRItemSets() {
	    ItemSet initial = new ItemSet(getProductions(Symbol.START));
	    List<ItemSet> states = SetUniqueList.setUniqueList(new ArrayList<>());
	    ItemSet.addState(states, initial, false);
	    ItemSet.generate(this, states);
	    return states;
    }

    void printLRItemSets(List<ItemSet> sets) {
	    System.out.println("Item Sets");
	    for (int i = 0; i < sets.size(); i++) {
		    System.out.println("Item Set " + i + " " + sets.get(i));
	    }
    }

    void printSLRActionTable() {
	    List<Symbol> symbols = getSymbolList();
	    System.out.print("\t");
	    for (Symbol s : symbols)
		    System.out.print(s + "\t");
	    System.out.println();
	    for (int i = 0; i < ItemSet.actionTable.size(); i++) {
		    System.out.print(i + " | \t");
		    for (Symbol s : symbols) {
			    Action act = ItemSet.actionTable.get(i).get(s);
			    if (act != null)
				    System.out.print(act + " | \t");
			    else
				    System.out.print("# | \t");
		    }
		    System.out.println();
	    }
    }

    static class Rule {
        private Symbol left;
        private List<Symbol> right;

        Rule(Symbol lhs, List<Symbol> rhs) {
            this.left = lhs;
            this.right = rhs;
        }

        Symbol getLeft() {
            return left;
        }

        List<Symbol> getRight() {
            return right;
        }

        Stream<Symbol> symbols() {
            return getSymbols().stream();
        }

        Set<Symbol> getSymbols() {
            Set<Symbol> symbols = new HashSet<>(List.of(left));
            symbols.addAll(right);
            return symbols;
        }

        boolean isLambda() {
            return right.size() == 1 && right.get(0).equals(Symbol.LAMBDA);
        }

        boolean hasTerminals() {
            return right.stream().anyMatch(symbol -> SymbolType.TERMINAL.equals(symbol.type));
        }

        boolean isEnd() {
            return right.contains(Symbol.EOF);
        }

        @Override
        public String toString() {
            return left + "->" + right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Rule rule = (Rule) o;
            return Objects.equals(left, rule.left) &&
                    Objects.equals(right, rule.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }
    }
}
