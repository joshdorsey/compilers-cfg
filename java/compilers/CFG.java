package compilers;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CFG {
    private ArrayListValuedHashMap<Symbol, LinkedList<Symbol>> map;
    private Set<Symbol> terminals;
    private Set<Symbol> nonterminals;

    CFG(ArrayListValuedHashMap<Symbol, LinkedList<Symbol>> map) {
        this.map = map;

        List<Rule> rules = productions().collect(Collectors.toList());

        Set<Symbol> terminals = new HashSet<>();
        Set<Symbol> nonterminals = new HashSet<>();

        for(Rule r : rules) {
            // TODO make terminal/nonterminal sets
        }
    }

    Stream<Rule> productions() {
        return map.entries().stream()
                .map(e -> new Rule(e.getKey(), e.getValue()));
    }

    Set<Symbol> enclose() {
        return new HashSet<>();
    }

    public class Rule {
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
    }
}
