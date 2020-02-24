package compilers;

class Symbol {
    public enum SymbolType { NONTERMINAL, TERMINAL, RULE, ALT, EOF, LAMBDA }

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
}