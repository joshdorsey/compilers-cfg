from typing import Optional, Dict, List, Callable, Final, Set, Tuple
from functools import partial


ARROW: Final[str] = "->"
ALTERNATION: Final[str] = "|"
END: Final[str] = "$"
LAMBDA: Final[str] = "lambda"


def is_terminal(sym: str) -> bool:
    """ Used to check if a symbol is a valid terminal. """
    return sym.islower() and sym != LAMBDA


def is_nonterminal(sym: str) -> bool:
    """ Used to check if a symbol is a valid nonterminal. """
    return sym.isupper()


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
            if self.goal == 'S':
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

    def all_rules(self, LHS=None, RHS=None):
        if LHS and RHS:
            raise IndentationError("Yeah, passing LHS and RHS to this function won't work.")
        
        if LHS:
            if self.is_nonterminal(LHS):
                return [(LHS, R) for R in self.rules[LHS]]
            else:
                return []

        allRules = [(L, R) for L in self.nonterminals for R in self.rules[L]]
        
        if RHS:
            return filter(lambda rule: RHS in rule[1], allRules)
        else:
            return allRules

    def derives_to_lambda_impl(self, L : str, T : List[Tuple[Tuple[str, List[str]], str]]) -> bool:
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

    def derives_to_lambda(self, L : str) -> bool:
        return self.derives_to_lambda_impl(L, [])

    def first_set_impl(self, x : List[str], firstSet : Set[str]) -> Set[str]:
        if len(x) == 0:
            return set()

        first, rest = x[0], x[1:]

        if first == END or first in self.terminals:
            return set([first])

        update = set()

        if first not in firstSet:
            firstSet.add(first)
            for rule in self.all_rules(LHS=first):
                newValues = self.first_set_impl(rule[1], firstSet)
                update.update(newValues)

        if self.derives_to_lambda(first):
            newValues = self.first_set_impl(rest, firstSet)
            update.update(newValues)

        return update

    def first_set(self, x : List[str]) -> Set[str]:
        return self.first_set_impl(x, set())

    def follow_set_impl(self, nt: str, follow: Set[str]) -> Set[str]:
        if nt in follow:
            return set()

        follow.add(nt)
        update = set()

        for rule in self.all_rules(RHS = nt):
            lhs, rhs = rule

            for pair in enumerate(rhs, start=1):
                idx, sym = pair
                if sym != nt:
                    continue

                tail = rhs[idx:]

                doFollow = False

                if len(tail) != 0:
                    update.update(self.first_set(tail))
                else:
                    doFollow = True

                if doFollow:
                    update.update(self.follow_set_impl(lhs, follow))
                else:
                    anyAugmentedSigma = any(map(self.is_augmented_sigma, tail))
                    allToLambda = all(map(self.derives_to_lambda, tail))
                    if not anyAugmentedSigma and allToLambda:
                        update.update(self.follow_set_impl(lhs, follow))

        return update

    def follow_set(self, nt: str) -> Set[str]:
        return self.follow_set_impl(nt, set())

    def predict_set(self, rule: Tuple[str, List[str]]) -> Set[str]:
        lhs, rhs = rule
        answer = self.first_set(rhs)

        if self.derives_to_lambda(lhs):
            answer.update(self.follow_set(lhs))

        return answer

    def predict_sets_disjoint(self) -> bool:
        from itertools import combinations

        for nt in self.nonterminals:
            for pair in combinations(self.all_rules(LHS=nt), 2):
                first, second = pair
                first = self.predict_set(first)
                second = self.predict_set(second)

                if not first.isdisjoint(second):
                    return False
        
        return True

    def is_terminal(self, x):
        return x in self.terminals

    def is_nonterminal(self, x):
        return x in self.nonterminals

    def is_augmented_sigma(self, x):
        return x == END or self.is_terminal(x)

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
