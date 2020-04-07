package compilers;

import java.util.Objects;

enum SymbolType { NONTERMINAL, TERMINAL, RULE, ALT, EOF, LAMBDA }

public class Symbol {
    static final Symbol LAMBDA = new Symbol("lambda", SymbolType.LAMBDA);
    public static final Symbol EOF = new Symbol("$", SymbolType.EOF);
    static final Symbol START = new Symbol("S", SymbolType.NONTERMINAL);
    private static final Symbol ALT = new Symbol("|", SymbolType.ALT);
    private static final Symbol RULE = new Symbol("->", SymbolType.RULE);

    final String token;
    final SymbolType type;

    Symbol() {
	    token = "";
	    type = null;
    }
    
    private Symbol(String token, SymbolType type) {
        this.token = token;
        this.type = type;
    }

    static Symbol of(String token) {
        switch (token) {
            case "->": return RULE;
            case "|": return ALT;
            case "$": return EOF;
            case "lambda": return LAMBDA;
            default:
                if (token.matches("[a-z]+"))
                    return new Symbol(token, SymbolType.TERMINAL);
                if (token.matches("[A-Z]+"))
                    return new Symbol(token, SymbolType.NONTERMINAL);
                return new Symbol(token, null);
        }
    }

    boolean isTerminal() {
        return type == SymbolType.TERMINAL;
    }

    boolean isNonTerminal() {
        return type == SymbolType.NONTERMINAL;
    }

    boolean isAugmentedSigma() {
        return equals(EOF) || isTerminal();
    }

    @Override
    public String toString() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
	if (o == null)
		return false;
        if (o == this)
		return true;
        if (o.getClass() != this.getClass())
		return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(token, symbol.token) &&
                type == symbol.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, type);
    }
}
