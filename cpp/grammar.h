/*
    Jamison Hubbard
    March 2020
    CFG Implementation
    grammar.h
*/

#ifndef GRAMMAR_H
#define GRAMMAR_H

using namespace std;

#include <string>
#include <map>
#include <set>
#include "rule.h"
#include "item.h"

class Grammar {
public:
    // Constructor
    Grammar(string filename);

    // Access Functions
    set<string> getNonTerminals();
    set<string> getTerminals();
    Rule getRule(int ruleID);
    int getRuleID(Rule rule);
    int numRules();
    string getStartSymbol();
    map<string, vector<int>> getLL1Table();
    bool isAmbiguous();
    
    // Algorithms
    set<string> firstSet(string nonterminal);
    set<string> followSet(string symbol);
    bool derivesToLambda(string nonterminal);
    set<string> predictSet(int ruleID);
    void generateLL1Table();
    set<Item> Closure(set<Item> I);
    set<Item> GoTo(set<Item> I);

    // Other
    void print();
    void report();
    void printLL1Table();
    string trimEdges(string in);
    set<Rule> getRulesDerivedFrom(string nonterminal);
    set<Rule> getRulesDerivedTo(string symbol);
    bool isTerminal(string symbol);
    bool isNonTerminal(string symbol);
    bool itemSetsEqual(set<Item> iOne, set<Item> iTwo);
    bool rulesEqual(Rule rOne, Rule rTwo);

private:
    map<int, Rule> rules;
    int ruleCount;

    set<string> nonterminals;
    set<string> terminals;
    string startSymbol;

    map<string, vector<int>> ll1_table;
    map<string, int> ll1_index;
    bool ambiguous;
};

#endif /*GRAMMAR_H*/