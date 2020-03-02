#!/usr/bin/env -S python -i
from typing import Optional, Dict, List, Callable, Final
from functools import partial
from sys import argv


ARROW: Final[str] = "->"
ALTERNATION: Final[str] = "|"
END: Final[str] = "$"
LAMBDA: Final[str] = "lambda"


def is_terminal(sym: str) -> bool:
    """ Used to check if a symbol is a valid terminal. """
    return sym.islower() and sym != LAMBDA


def is_nonterminal(sym: str) -> bool:
    """ Used to check if a symbol is a valid nonterminal. """
    return not is_terminal(sym)


def is_end(rule: List[str]) -> bool:
    """ Used to check if a production rule is end-of-input. """
    return rule[-1] == END


class ContextFreeGrammar:
    def __init__(self,
                 goal: Optional[str] = "S",
                 terminals: Optional[set] = None,
                 nonterminals: Optional[set] = None,
                 rules: Optional[Dict[str, List[List[str]]]] = None):
        # Fill in mutable default args
        if not terminals:
            terminals = set()

        if not nonterminals:
            nonterminals = set()

        if not rules:
            rules = {}

        # All the parts of the grammar
        self.goal = goal
        self.terminals = terminals
        self.nonterminals = nonterminals
        self.rules = rules

    def add_rule(self, LHS: str, RHS: List[List[str]]) -> None:
        """
            Adds a rule to this CFG.  The RHS of a production is a list
        of strings with no whitespace in them.  This function accepts
        a list of productions, if a LHS has multiple alternations.
            This method will throw an AssertionError if this rule would
        conflict with the existing goal for this CFG.
        """
        # Check if this new rule is a goal state
        endRules = map(is_end, RHS)

        goal = False
        if any(endRules):
            # If any of the productions end with END, they all have to
            assert all(endRules), (
                "Grammar had a rule with some (but not all)"
                f" rules ending with {END}")
            goal = True

        if goal:
            if not self.goal:
                self.goal = LHS
            assert self.goal == LHS, (
                "Rule conflicts with existing goal state")

        # At this point, we should have a fairly valid production rule, so
        # add it and update the T/NT sets.
        if LHS not in self.rules:
            self.rules[LHS] = RHS
        else:
            self.rules[LHS].extend(RHS)

        # Get the set of all symbols (terminal and non-terminal)
        # used in this set of production rules.
        allSymbols = set(sum([[LHS], *RHS], []))

        # Update the terminal and nonterminal sets with any new symbols
        self.terminals |= set(filter(is_terminal, allSymbols))
        self.nonterminals |= set(filter(is_nonterminal, allSymbols))

    def all_rules(self, LHS=None):
        if LHS:
            return [(LHS, R) for R in self.rules[LHS]]

        return [(L, R) for L in self.nonterminals for R in self.rules[L]]

    def derives_to_lambda_impl(self, L, T):
        for lhs, rhs in self.all_rules(L):
            # If this rule is just lambda, we have derived to lambda
            if rhs == [LAMBDA]:
                return True

            # If the rule contains end-of-input or a terminal, we can skip it.
            if END in rhs or any(map(is_terminal, rhs)):
                continue

            # Recurse on each nonterminal on the RHS
            def recurse(nt):
                stackItem = ((lhs, rhs), nt)
                # If we've already traversed this item, skip it
                if stackItem in T:
                    return True

                # Otherwise append it to the stack and continue
                T.append(stackItem)

                # Recurse
                derivesToLambda = self.derives_to_lambda_impl(nt, T)

                T.pop()

                return derivesToLambda

            nonterminals = filter(is_nonterminal, rhs)

            # If all the nonterminals on the RHS of the equation derive to
            # lambda, then the expression derives to lambda.
            if all(map(recurse, nonterminals)):
                return True

        return False

    def derives_to_lambda(self, L):
        return self.derives_to_lambda_impl(L, [])

    def is_terminal(self, x):
        return x in self.terminals

    def is_nonterminal(self, x):
        return x in self.nonterminals

    def __str__(self):
        assert set(self.rules.keys()) == self.nonterminals

        nonTerminals = f"{{{', '.join(self.nonterminals)}}}"
        terminals = f"{{{', '.join(self.terminals)}}}"

        # Print the goal state first, then alphabetical
        def rule_print_key(rule):
            if self.goal == rule:
                return -1
            else:
                return ord(rule[:1].upper())

        rules = []

        for nt in sorted(self.nonterminals, key=rule_print_key):
            prefix = f"{nt} {ARROW} "

            alts = []
            for alt in self.rules[nt]:
                # Build a string for this alternation, and it to
                # the list of all alternation strings.
                alts.append(" ".join(alt))

            rules.append(prefix + f"\n   {ALTERNATION} ".join(alts))

        rules = "\n".join(rules)

        return (f"Terminals: {terminals}\n"
                f"Nonterminals: {nonTerminals}\n"
                f"Goal = {self.goal}\n"
                f"Rules:\n{rules}")


def cfg_from_file(filename: str) -> ContextFreeGrammar:
    """ Reads in the lines of a file and creates a CFG """
    cfg = ContextFreeGrammar()

    rules = []
    with open(filename) as file:
        # It's possible we'll join some lines together as we
        # read, these variables keep track of that.
        lastLineStartedRule = False
        rule = ""

        for line in file:
            line = line.strip()

            # Ignore empty lines
            if not line:
                continue

            # If the line is an alternation, just append
            if line[0] == ALTERNATION:
                rule += " " + line
                lastLineStartedRule = False

            # If it's not an alternation, we may be finishing
            # up a rule with several alternations
            elif ARROW in line:
                if not lastLineStartedRule and rule:
                    rules += [rule]
                rule = line

        # We might have been building a rule at the end of the
        # file and not actually put it in our list (because it
        # could have had alternations)
        if rule:
            rules += [rule]

    for rule in rules:
        # Split the left and right sides
        LHS, RHS = rule.split(ARROW)

        # Trim any whitespace on the production name
        LHS = LHS.strip()

        assert is_nonterminal(LHS), (
            f"Invalid production rule name {LHS}, "
            "should be uppercase.")

        # Seperate the right side by alternations, and build a list
        # of stripped symbols for each one.
        alternations = RHS.split(ALTERNATION)
        RHS = [[x.strip() for x in alt.split()] for alt in alternations]

        cfg.add_rule(LHS, RHS)

    return cfg

if "__main__" == __name__:
    args = argv[1:]
    file = "grammar.cfg" if len(args) < 1 else args[0]

    try:
        cfg = cfg_from_file(file)
    except FileNotFoundError:
        print(f"Couldn't open file '{file}', "
              "please pass in a valid filename.")
        cfg = ContextFreeGrammar(goal="ERROR")

    print(f"{cfg}")

    for nt in cfg.nonterminals:
        print(f"derivesToLambda('{nt}') = {cfg.derives_to_lambda(nt)}")
