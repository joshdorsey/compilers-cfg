import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.LinkedList;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.MapIterator;
import static java.util.Arrays.stream;

public class CFGReader {
	private ArrayListValuedHashMap<Symbol, LinkedList<Symbol>> grammar;

	public CFGReader(File cfg) {
		grammar = new ArrayListValuedHashMap<>();
		try (Scanner in = new Scanner(cfg)) {
			Symbol currentProd = null;
			LinkedList<Symbol> currentRule;
			while(in.hasNext()) {
				Symbol[] symbols = stream(in.nextLine().split(" "))
					.map(s -> new Symbol(s))
					.filter(s -> s.type != null)
					.toArray(Symbol[]::new);
				int i = 1;
				if (symbols[0].type == Symbol.SymbolType.NONTERMINAL) {
					currentProd = symbols[0];
					i = 2;
				}
				currentRule = new LinkedList<>();
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
			System.exit(1);
		}
		MapIterator<Symbol, LinkedList<Symbol>> it = grammar.mapIterator();
		while (it.hasNext()) {
			it.next();
			System.out.println(it.getKey() + "->" + it.getValue());
		}
	}

	public static void main(String... args) {
		if (args.length != 1)
			System.exit(1);
		CFGReader reader = new CFGReader(new File(args[0]));
	}

	static class Symbol {
		String token;
		SymbolType type;

		Symbol(String token) {
			this.token = token;
			type = matchSymbol(token);
		}

		@Override
		public String toString() {
			return token;
		}

		static SymbolType matchSymbol(String token) {
			switch (token) {
				case "->": return SymbolType.RULE;
				case "|": return SymbolType.ALT;
				case "$": return SymbolType.EOF;
				case "lambda": return SymbolType.LAMBDA;
				default:
		 			if (token.matches("[a-z]+"))
	 					return SymbolType.TERMINAL;
 					if (token.matches("[A-Z]+"))
						return SymbolType.NONTERMINAL;
					return null;
			}
		}

		enum SymbolType { NONTERMINAL, TERMINAL, RULE, ALT, EOF, LAMBDA; }
	}
}
