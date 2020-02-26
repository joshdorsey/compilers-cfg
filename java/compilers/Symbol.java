package compilers;

import java.util.Objects;

enum SymbolType { NONTERMINAL, TERMINAL, RULE, ALT, EOF, LAMBDA }

class Symbol {
    static final Symbol LAMBDA = new Symbol("lambda");
    static final Symbol EOF = new Symbol("$");

    String token;
    SymbolType type;

    Symbol(String token) {
        this.token = token;
        type = matchSymbol(token);
    }

    public boolean isTerminal() {
        return SymbolType.TERMINAL.equals(type);
    }

    public boolean isNonTerminal() {
        return SymbolType.NONTERMINAL.equals(type);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(token, symbol.token) &&
                type == symbol.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, type);
    }
}