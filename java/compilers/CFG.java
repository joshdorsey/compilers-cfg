package compilers;

import compilers.util.Tuple;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;
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
                Tuple<Rule, Symbol> stackItem = new Tuple<>(rule, nt);

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
            return new HashSet<>(Collections.singletonList(firstSymbol));
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
    //</editor-fold>

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
