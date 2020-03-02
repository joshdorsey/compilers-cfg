#!/usr/bin/env -S python3 -i
from sys import argv
from cfg import *

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
        lhs, rhs = rule.split(ARROW)

        # Trim any whitespace on the production name
        lhs = lhs.strip()

        assert is_nonterminal(lhs), (
            f"Invalid production rule name {lhs}, "
            "should be uppercase.")

        # Seperate the right side by alternations, and build a list
        # of stripped symbols for each one.
        alternations = rhs.split(ALTERNATION)
        rhs = [[x.strip() for x in alt.split()] for alt in alternations]

        cfg.add_rule(lhs, rhs)

    return cfg

if "__main__" == __name__:
    args = argv[1:]
    file = "../grammars/predict-set-test0.cfg" if len(args) < 1 else args[0]

    try:
        cfg = cfg_from_file(file)
    except FileNotFoundError:
        print(f"Couldn't open file '{file}', "
              "please pass in a valid filename.")
        cfg = ContextFreeGrammar(goal="ERROR")

    print(f"{cfg}")

    print("\nderives_to_lambda tests")
    for nt in cfg.nonterminals:
        print(f"derives_to_lambda('{nt}') = {cfg.derives_to_lambda(nt)}")

    print("\nfirst_set tests")
    for nt in cfg.nonterminals:
        print(f"first_set('{nt}') = {cfg.first_set([nt])}")

    print("\nfollow_set tests")
    for nt in cfg.nonterminals:
        print(f"follow_set('{nt}') = {cfg.follow_set(nt)}")

    print("\npredict_set tests")
    for rule in cfg.all_rules():
        print(f"predict_set('{rule}') = {cfg.predict_set(rule)}")
