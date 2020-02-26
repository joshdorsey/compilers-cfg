/*
Jamison Hubbard
CFG Implementation 
CSCI 498A Compilers
Feb 2020

cfg.cpp
*/

using namespace std;

#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <map>
#include <set>

#include "cfg.h"

// Constructor

CFG::CFG(string filename) {

    ifstream fileIn(filename);
    if (!fileIn) exit(1);

    string line;
    string currentNT;

    while (getline(fileIn, line)) {

        istringstream lineIn(line);
        string first, second, temp, rule;
        vector<string> lineEnd, currentRule;

        lineIn >> first;
        lineIn >> second;
        while (lineIn >> temp) lineEnd.push_back(temp);

        if (first == "|") {
            currentRule = cfgMap[currentNT];
            rule = second;
            if (isTerminal(rule)) terminals.insert(rule);
            else nonterminals.insert(rule);
        }
        else {
            if (cfgMap.find(first) == cfgMap.end()) {
                currentNT = first;
                if (isTerminal(currentNT)) terminals.insert(currentNT);
                else nonterminals.insert(currentNT);
            }
            else {
                currentRule = cfgMap[currentNT];
            }
        }

        for (int i = 0; i < lineEnd.size(); ++i) {
                
            if (lineEnd[i] == "|") {
                currentRule.push_back(rule);
                rule = "";
                continue;
            }

            if (lineEnd[i] == "$") {
                if (startNonterminal != "") exit(2);
                startNonterminal = currentNT;
                rule += " $";
                break;
            }

            if (rule != "") rule += " ";
            rule +=  lineEnd[i];

            if (isTerminal(lineEnd[i])) terminals.insert(lineEnd[i]);
            else nonterminals.insert(lineEnd[i]);
        }

        currentRule.push_back(rule);
        cfgMap[currentNT] = currentRule;
    }

    fileIn.close();
    terminals.erase("lambda");
}

// Internal Methods

bool CFG::isTerminal(string symbol) {
    char first = symbol[0];
    return !(isupper(first));
}

// Getters

set<string> CFG::getNT() {return nonterminals;}
set<string> CFG::getT() {return terminals;}

vector<string> CFG::getProductionsFor(string nonterminal) {
    if (nonterminals.find(nonterminal) == nonterminals.end()) exit (3);

    return cfgMap[nonterminal];
}

vector<string> CFG::getRHSOccurencesFor(string symbol) {
    vector<string> occurences;

    map<string, vector<string>>::iterator mit = cfgMap.begin();
    for (pair<string, vector<string>> nonterm : cfgMap) {
        for (int i = 0; i < nonterm.second.size(); ++i) {
            string rhs = nonterm.second[i];
            istringstream rhsIn(rhs);
            bool inRHS = false;
            string word;

            while (rhsIn >> word) {
                if (word == symbol) inRHS = true;
            }

            if (inRHS) occurences.push_back(rhs);
        }
    }

    return occurences;
}

// Test Methods

void CFG::printMap() {
    int ruleCount = 1;
    map<string, vector<string>>::iterator mit = cfgMap.begin();
    for (pair<string, vector<string>> row : cfgMap) {
        bool barNeeded = false;
        cout << to_string(ruleCount) << "   " << row.first << " -> ";
        for (int i = 0; i < row.second.size(); ++i) {
            if (barNeeded) cout << to_string(ruleCount) << "      | ";
            else barNeeded = true;

            cout << row.second[i] << endl;
            ruleCount++;
        }
    }

    cout << endl << "Start Symbol:   " << startNonterminal << endl << endl;
    cout << "Terminals:      ";
    set<string>::iterator sit = terminals.begin();
    for (string term : terminals) {
        cout << term << " ";
    }
    cout << endl << endl << "Nonterminals:   ";
    sit = nonterminals.begin();
    for (string nonterm : nonterminals) {
        cout << nonterm << " ";
    }
    cout << endl;
}

// cfg algorithms

bool CFG::derivesToLambda(string nonTerm) {
    map<string, vector<string>> emptyMap;
    return derivesToLambda(nonTerm, emptyMap);
}

bool CFG::derivesToLambda(string nonTerm, map<string, vector<string>> & ruleStack) {

    set<string>::iterator sit = nonterminals.begin();
    vector<string> rules = cfgMap[nonTerm];
    for (int i = 0; i < rules.size(); ++i) {
        // if rule is lambda, then yes it derives to lambda
        if (rules[i] == "lambda") {
            return true;
        }

        // if rule contains a terminal or $, it can't derive
        // to lambda, so continue to next rule
        bool containsTerminal = false;
        istringstream words(rules[i]);
        string word;
        while (words >> word) {
            if (word == "$" || isTerminal(word)) {
                containsTerminal = true;
                break;
            }
        }
        if (containsTerminal) continue;

        // if no terminals, then every symbol is a nonterminal
        // therefore every symbol must deriveToLambda, or the 
        // root symbol can't for this production
        bool allDeriveLambda = true;
        istringstream symbols(rules[i]);
        string symbol;
        bool ruleAndSymbolInStack = false;
        while (symbols >> symbol) {
            // if production and symbol combo is on stack, continue
            if (ruleStack.find(nonTerm+symbol) != ruleStack.end()) {
                vector<string> ntRules = ruleStack[nonTerm+symbol];
                for (int j = 0; j < ntRules.size(); ++j) {
                    if (ntRules[j] == rules[i]) ruleAndSymbolInStack = true;
                }
            }
            if (ruleAndSymbolInStack) continue;

            // push production and symbol combo to stack
            if (ruleStack.find(nonTerm+symbol) == ruleStack.end()) {
                vector<string> newRules;
                newRules.push_back(rules[i]);
                ruleStack[nonTerm+symbol] = newRules;
            }
            else {
                vector<string> oldRules = ruleStack[nonTerm+symbol];
                oldRules.push_back(rules[i]);
                ruleStack[nonTerm+symbol] = oldRules;
            }

            // recursive call to find if all derive to lambda
            allDeriveLambda = derivesToLambda(symbol, ruleStack);

            // pop production and symbol combo from stack
            vector<string> oldRules = ruleStack[nonTerm+symbol];
            vector<string>::iterator vit = oldRules.begin();
            while (vit != oldRules.end()) {
                if (*vit == rules[i]) {
                    oldRules.erase(vit);
                    break;
                }
                vit++;
            }

            if (!allDeriveLambda) break;
        }

        if (allDeriveLambda) return true;

    }

    return false;
}

set<string> CFG::firstSet(string seq) {
    set<string> emptySet;
    return firstSet(seq, emptySet);
}

set<string> CFG::firstSet(string seq, set<string> T) {

    set<string> F;

    // converting seq to chi beta
    istringstream separator(seq);
    string chi, beta, temp;
    separator >> chi;
    while (separator >> temp) beta += temp + " ";

    // if chi is a terminal or $, then it is the first set
    if (isTerminal(chi) || chi == "$") {
        if (chi != "lambda") F.insert(chi);
        return F;
    }

    bool chiNotInT = true;
    set<string>::iterator sit = T.begin();
    for (string s : T) {
        if (s == chi) chiNotInT = false;
    }
    if (chiNotInT) {
        T.insert(chi);
        vector<string> chiProductions = getProductionsFor(chi);
        for (int i = 0; i < chiProductions.size(); ++i) {
            string R = chiProductions[i];
            set<string> G = firstSet(R, T);

            set<string>::iterator sit = G.begin();
            for (string s : G) if (s != "lambda") F.insert(s);
        }
    }

    if (derivesToLambda(chi)) {
        set<string> G = firstSet(beta, T);

        set<string>::iterator sit = G.begin();
        for (string s : G) if (s != "lambda") F.insert(s);
    }

    return F;
}

set<string> CFG::followSet(string nonT) {

    // error prevention
    set<string> emptySet;
    return emptySet;
}

void CFG::testDerivesToLambda() {
    cout << endl << "Testing derivesToLambda" << endl;
    set<string>::iterator sit = nonterminals.begin();
    for (string nonT : nonterminals) {
        cout << nonT << "\t";
        bool DTL = derivesToLambda(nonT);

        if (DTL) cout << "True\n";
        else cout << "False\n";
    }
}

void CFG::testFirstSet() {
    cout << "Testing testFirstSet" << endl << endl;
    set<string>::iterator sit = nonterminals.begin();
    for (string nonT : nonterminals) {
        cout << nonT << "\t";
        set<string> first = firstSet(nonT);

        set<string>::iterator sit2 = first.begin();
        for (string s2 : first) cout << endl << s2 << " ";
        cout << endl;
    }
}
