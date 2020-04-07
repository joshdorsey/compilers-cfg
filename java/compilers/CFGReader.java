package compilers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
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
                String line = in.nextLine();
                if(line.isBlank()) {
                    continue;
                }
                Symbol[] symbols = stream(line.split("\\s+"))
                        .map(Symbol::of)
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
        if (args.length < 1) {
            System.out.println("usage: CFGReader.jar grammarfile inputfile");
            System.exit(1);
        }

        CFG grammar = readGrammar(new File(args[0]));

	if (args.length == 2) {
		LinkedList<Symbol> lines = new LinkedList<>();
		try (Scanner in = new Scanner(new File(args[1]))) {
			while (in.hasNext())
				lines.add(Symbol.of(in.nextLine().trim()));
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		try {
			ParseTree tree = new ParseTree(grammar, lines);
			System.out.println(tree.root);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.exit(0);
	}

        System.out.println("Grammar Rules:");
        System.out.println("==============");
        grammar.productions().forEach(rule ->
                System.out.println(rule.getLeft() + " -> " + rule.getRight())
        );

        System.out.println("\nderivesToLambda Tests:");
        System.out.println("======================");
        grammar.nonterminals().forEach(nonterminal -> {
            System.out.print("derivesToLambda(" + nonterminal + ") = ");
            System.out.println(grammar.derivesToLambda(nonterminal));
        });

        System.out.println("\nfirstSet Tests:");
        System.out.println("===============");
        grammar.nonterminals().forEach(nonterminal -> {
            System.out.print("firstSet(" + nonterminal + ") = ");
            System.out.println(grammar.firstSet(List.of(nonterminal)));
        });
        grammar.productions().map(CFG.Rule::getRight).forEach(rhs -> {
            System.out.print("firstSet(" + rhs + ") = ");
            System.out.println(grammar.firstSet(rhs));
        });

        System.out.println("\nfollowSet Tests:");
        System.out.println("================");
        grammar.nonterminals().forEach(nonterminal -> {
            System.out.print("followSet(" + nonterminal + ") = ");
            System.out.println(grammar.followSet(nonterminal));
        });

        System.out.println("\npredictSet Tests:");
        System.out.println("=================");
        grammar.productions().forEach(rule -> {
            System.out.print("predictSet(" + rule + ") = ");
            System.out.println(grammar.predictSet(rule));
        });

        System.out.print("\nPredict sets disjoint? ");
        System.out.println(grammar.predictSetsDisjoint() ? "Yes." : "No.");

        grammar.printLLParseTable(grammar.buildLLParseTable());

	grammar.printLRItemSets(grammar.buildLRItemSets());
	grammar.printSLRActionTable();
    }
}
