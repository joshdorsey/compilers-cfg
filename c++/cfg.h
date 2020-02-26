/*
Jamison Hubbard
CFG Implementation
CSCI 498A Compilers
Feb 2020

cfg.h
*/

#ifndef CFG_H
#define CFG_H

using namespace std;

#include <string>
#include <vector>
#include <map>
#include <set>

class CFG {
    public:
    // Constructor
    CFG(string filename);

    // Internal methods
    bool isTerminal(string symbol);

    // getters
    set<string> getNT(); // get nonterminals
    set<string> getT(); // get terminals
    vector<string> getProductionsFor(string nonterminal);
    vector<string> getRHSOccurencesFor(string symbol);

    // testing methods
    void printMap();
    void testDerivesToLambda();
    void testFirstSet();

    // cfg algorithms
    bool derivesToLambda(string nonTerm);
    bool derivesToLambda(string nonTerm, map<string, vector<string>> &ruleStack);
    set<string> firstSet(string seq);
    set<string> firstSet(string seq, set<string> T);
    set<string> followSet(string nonT);

    private:
    map<string, vector<string>> cfgMap;
    set<string> nonterminals;
    set<string> terminals;
    string startNonterminal;
};

#endif /* CFG_H */