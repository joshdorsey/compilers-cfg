package compilers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.LinkedList;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import static java.util.Arrays.stream;

public class CFGReader {
    private static CFG readGrammar(File cfg) {
        ArrayListValuedHashMap<Symbol, LinkedList<Symbol>> grammar = new ArrayListValuedHashMap<>();

        try (Scanner in = new Scanner(cfg)) {
            Symbol currentProd = null;

            while (in.hasNext()) {
                Symbol[] symbols = stream(in.nextLine().split("\\s+"))
                        .map(Symbol::new)
                        .filter(s -> s.type != null)
                        .toArray(Symbol[]::new);


                LinkedList<Symbol> currentRule = new LinkedList<>();

                int i = 1;
                if (symbols[0].type == SymbolType.NONTERMINAL) {
                    currentProd = symbols[0];
                    i = 2;
                }
                for (; i < symbols.length; i++) {
                    switch (symbols[i].type) {
                        case ALT:
                            grammar.put(currentProd, currentRule);
                            currentRule = new LinkedList<>();
                            break;
                        default:
                            currentRule.add(symbols[i]);
                            break;
                    }
                }

                grammar.put(currentProd, currentRule);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        return new CFG(grammar);
    }

    public static void main(String... args) {
        if (args.length != 1) {
            System.out.println("Please provide one argument: the name of a grammar file.");
            System.exit(1);
        }

        CFG grammar = readGrammar(new File(args[0]));

        grammar.productions().forEach(rule ->
            System.out.println(rule.getLeft() + "->" + rule.getRight())
        );

        grammar.nonterminals().forEach(nonterminal ->
            System.out.println("derivesToLambda(" + nonterminal + ") = " + grammar.derivesToLambda(nonterminal))
        );
    }
}
