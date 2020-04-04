/*
    Jamison Hubbard
    March 2020
    CFG Implementation
    grammar.cpp
*/

using namespace std;

#include <iostream>
#include <string>
#include <map>
#include <set>
#include <vector>
#include <fstream>
#include <sstream>
#include "rule.h"
#include "item.h"
#include "grammar.h"

// Constructor
Grammar::Grammar(string filename) {

    // open file stream to given CFG file
    ifstream fileIn("../grammars/" + filename);
    if (!fileIn) exit(1);

    // this bool will allow us the save the start symbol
    bool isStart = true;

    // setup a while loop to read line-by-line, with rule counter
    string line;
    string currentNT;
    int ruleCounter = 0;
    while(getline(fileIn, line)) {

        // read the first two words to see what line format
        // the current line is
        stringstream lineIn(line);
        string first, second;
        lineIn >> first;
        lineIn >> second;

        if (second == "->") {
            // set new nonterminal and get rhs of rule
            currentNT = trimEdges(first);
            nonterminals.insert(currentNT);
            vector<string> rhs;
            string word = " ";

            // if new NT is the start symbol, save it
            if (isStart) {
                isStart = false;
                startSymbol = currentNT;
            }

            // while the line still has unread words
            while (word != "") {
                word = "";
                lineIn >> word;
                if (word != "|") {
                    if (word != "") rhs.push_back(word);
                    terminals.insert(word);
                }
                else {
                    // if a "|" is found create the new rule
                    // and reset to start a new one

                    Rule newRule(currentNT, rhs);
                    rules.insert(pair<int, Rule>(ruleCounter, newRule));
                    ++ruleCounter;
                    while (rhs.size() > 0) {rhs.pop_back();}
                }
            }

            if (rhs.size() > 0) {
                // create new rule and add it to the rule set
                Rule newRule(currentNT, rhs);
                rules.insert(pair<int, Rule>(ruleCounter, newRule));
                ++ruleCounter;
            }
        }
        else if (first == "|") {
            // use second to start the rhs
            vector<string> rhs;
            rhs.push_back(second);
            terminals.insert(second);
            string word = " ";

            // while the line still has unread words
            while (word != "") {
                word = "";
                lineIn >> word;
                if (word != "|") {
                    if (word != "") rhs.push_back(word);
                    terminals.insert(word);
                }
                else {
                    // if a "|" is found create the new rule
                    // and reset to start a new one

                    Rule newRule(currentNT, rhs);
                    rules.insert(pair<int, Rule>(ruleCounter, newRule));
                    ++ruleCounter;
                    while (rhs.size() > 0) {rhs.pop_back();}
                }
            }

            if (rhs.size() > 0) {
                // create new rule and add it to the rule set
                Rule newRule(currentNT, rhs);
                rules.insert(pair<int, Rule>(ruleCounter, newRule));
                ++ruleCounter;
            }
        }
        else {
            cout << "\n\nIncorrectly formatted file at rule ";
            cout << to_string(ruleCount) << "\n\n";
            exit(2);
        }
    }

    // correct any nonterminals that were placed in the
    // terminals set, as well as $ and lambda
    set<string> removeFromTerminalSet;
    set<string>::iterator sit1 = nonterminals.begin();
    for (string nt : nonterminals) {
        set<string>::iterator sit2 = terminals.begin();
        for (string t : terminals) {
            if (nt == t) {
                removeFromTerminalSet.insert(nt);
                break;
            }
        }
    }
    sit1 = terminals.begin();
    for (string t : terminals) {
        if (t == "$" || t == "lambda") {
            removeFromTerminalSet.insert(t);
        }
    }
    sit1 = removeFromTerminalSet.begin();
    for (string nt : removeFromTerminalSet) {
        terminals.erase(nt);
    }

    fileIn.close();
    ruleCount = ruleCounter;

    // generate the LL(1) Table
    // this will also generate the LL1 Index, and determine the
    // grammar's ambiguity state

    generateLL1Table();
}

// Access Functions
set<string> Grammar::getNonTerminals() {
    return nonterminals;
}
set<string> Grammar::getTerminals() {
    return terminals;
}
Rule Grammar::getRule(int ruleID) {
    if (ruleID >= ruleCount) {
        return Rule();
    }
    
    return rules[ruleID];
}
int Grammar::getRuleID(Rule rule) {
    map<int, Rule>::iterator mit = rules.begin();
    for (pair<int, Rule> production : rules) {
        if (rulesEqual(rule, production.second)) return production.first; 
    }
    return -1;
}
int Grammar::numRules() {
    return ruleCount;
}
string Grammar::getStartSymbol() {
    return startSymbol;
}
map<string, vector<int>> Grammar::getLL1Table() {
    return ll1_table;
}
bool Grammar::isAmbiguous() {
    return ambiguous;
}

// Algorithms
set<string> Grammar::firstSet(string nonterminal) {
    // if not a nonterminal, return itself
    // unless it's lambda, then return an empty set
    if (!isNonTerminal(nonterminal)) {
        set<string> notNonTerminal;
        if (nonterminal != "lambda") notNonTerminal.insert(nonterminal);
        return notNonTerminal;
    }

    // create set to contain terminals in firstSet
    set<string> first;

    // iterate through rules with nonterminal on LHS
    set<Rule> rulesFromNT = getRulesDerivedFrom(nonterminal);
    set<Rule>::iterator sit1 = rulesFromNT.begin();
    for (Rule r : rulesFromNT) {
        // find firstSet(NT) and add it to set
        // if NT derivesToLambda, then bump forward and
        // repeat entire process
        int front = 0;

        while(true) {
            // look at current symbol on RHS
            string currentSymbol = r.getRHS()[front];

            // if a T, add it to set and move on to next rule
            if (isTerminal(currentSymbol)) {
                first.insert(currentSymbol);
                break;
            }

            // if it's lambda or $, also move on to next rule
            if (currentSymbol == "lambda" || currentSymbol == "$") break;

            // check to make sure it's an NT then
            if (!isNonTerminal(currentSymbol)) {exit(4);}

            // if current NT, move on to next rule
            if (currentSymbol == nonterminal) {
                break;
            }

            // otherwise, find first(NT) and add it to set
            set<string> firstOfCurrent = firstSet(currentSymbol);
            set<string>::iterator sit2 = firstOfCurrent.begin();
            for (string symbol : firstOfCurrent) {
                first.insert(symbol);
            }

            // check if NT can derive to lambda
            // if so, adjust front and repeat, otherwise break
            if (derivesToLambda(currentSymbol)) {
                ++front;
                
                // if front exceeds the number of symbols
                // on the RHS, then we need to break
                if (front >= r.getRHS().size()) break;
                continue;
            }
            else {
                break;
            }

        }
    }

    // return the finished set
    return first;
}
set<string> Grammar::followSet(string symbol) {
    // return an empty set if lambda or $
    if (symbol == "lambda" || symbol == "$") {
        set<string> emptySet;
        return emptySet;
    }

    // create set to contain terminals in the follow set
    set<string> follow;

    // iterate through rules with symbol on the RHS
    set<Rule> rulesToSymbol = getRulesDerivedTo(symbol);
    set<Rule>::iterator sit = rulesToSymbol.begin();
    for (Rule r : rulesToSymbol) {
        // find the "index" of the symbol in the rhs of the rule
        int symbolIndex;
        for (int i = 0; i < r.getRHS().size(); ++i) {
            if (r.getRHS()[i] == symbol) {
                symbolIndex = i;
                break;
            }
        }

        // if the symbol was the last element, we need to find the
        // follow set of the nonterminal for the current rule
        if (r.getRHS().size() == symbolIndex + 1) {
            // unless the nonterminal is also the symbol
            if (r.getLHS() == symbol) continue;

            set<string> followOfNT = followSet(r.getLHS());
            set<string>::iterator sit2 = followOfNT.begin();
            for (string terminal : followOfNT) {follow.insert(terminal);}
        }
        else {
            // get the next symbol
            string nextSymbol = r.getRHS()[symbolIndex+1];

            // check if the next symbol is a terminal or $. If so,
            // add it to set and move on
            if (isTerminal(nextSymbol) || nextSymbol == "$") {
                follow.insert(nextSymbol);
                continue;
            }

            // if it's lambda move on
            if (nextSymbol == "lambda") {continue;}

            // now it has to be a nonterminal, so find its
            // first set and add that
            set<string> firstOfNext = firstSet(nextSymbol);
            set<string>::iterator sit2 = firstOfNext.begin();
            for (string terminal : firstOfNext) {follow.insert(terminal);}
        }
    }

    // return the follow set
    return follow;
}
bool Grammar::derivesToLambda(string nonterminal) {
    // if not a nonterminal, return false
    // unless it's lambda, then return true
    if (!isNonTerminal(nonterminal)) {
        if (nonterminal == "lambda") return true;
        return false;
    }

    // iterate through rules with nonterminal on LHS
    set<Rule> rulesFromNT = getRulesDerivedFrom(nonterminal);
    set<Rule>::iterator sit1 = rulesFromNT.begin();
    for (Rule r : rulesFromNT) {
        // if rule is simply lambda return true
        if (r.getRHS().size() == 1 && r.getRHS()[0] == "lambda") return true;

        // if any symbol is a terminal or cannot
        // deriveToLambda, then move on
        bool cannotD2L = false;
        for (int i = 0; i < r.getRHS().size(); ++i) {
            if (!derivesToLambda(r.getRHS()[i])) {
                cannotD2L = true;
                break;
            }
        }

        if (cannotD2L) continue;
        else return true;
    }

    // if all rules have been seen and non deriveToLambda
    return false;
}
set<string> Grammar::predictSet(int ruleID) {
    Rule rule = rules[ruleID];

    // create a set to hold the terminals in the predict set
    set<string> predict;

    // find the first set a symbol in the RHS, starting at the first
    // one, and add it to the predict set. If the symbol doesn't
    // derive to lambda, break, otherwise continue.

    bool allDeriveToLambda = true;

    for (int i = 0; i < rule.getRHS().size(); ++i) {
        set<string> firstOfSymbol = firstSet(rule.getRHS()[i]);
        set<string>::iterator sit = firstOfSymbol.begin();
        for (string terminal : firstOfSymbol) {
            predict.insert(terminal);
        }
        if (!derivesToLambda(rule.getRHS()[i])) {
            allDeriveToLambda = false;
            break;
        }
    }

    // if not all derive to lambda, then return the predict set
    if (!allDeriveToLambda) return predict;

    // otherwise, find the follow set of the nonterminal and add
    // that as well to the predict set

    set<string> followOfNT = followSet(rule.getLHS());
    set<string>::iterator sit = followOfNT.begin();
    for (string terminal : followOfNT) {
        predict.insert(terminal);
    }

    // return the predict set
    return predict;
}
void Grammar::generateLL1Table() {
    // at the beginning, set ambiguous to false
    ambiguous = false;

    // fill the table with -1 for the correct sizing
    // the table has rows for each nonterminal, and columns for
    // each terminal + $
    set<string>::iterator sit = nonterminals.begin();
    for (string nt : nonterminals) {
        vector<int> columns;
        for (int i = 0; i <= terminals.size(); ++i) {
            columns.push_back(-1);
        }
        ll1_table.insert(pair<string, vector<int>>(nt, columns));
    }

    // map each terminal to an index for the columns
    // $ will always be last
    int indexCount = 0;
    sit = terminals.begin();
    for (string t : terminals) {
        ll1_index.insert(pair<string, int>(t, indexCount));
        ++indexCount;
    }
    ll1_index.insert(pair<string, int>("$", indexCount));

    // for each rule, get its predict set
    map<int, Rule>::iterator mit = rules.begin();
    for (pair<int, Rule> rule : rules) {
        set<string> predict = predictSet(rule.first);
        
        // get the nonterminal for the rule
        string nt = rule.second.getLHS();

        // for each symbol in the predict set, place the rule number
        // in the table in the row corresponding to the nt
        // and the column given by the index map
        sit = predict.begin();
        for (string symbol : predict) {
            // if the cell already has a value, the grammar is ambiguous
            if (ll1_table[nt][ll1_index[symbol]] != -1) ambiguous = true;

            ll1_table[nt][ll1_index[symbol]] = rule.first;
        }
    }
}
set<Item> Grammar::Closure(set<Item> I) {
    // create a copy of I, called C, and an empty set called oldC
    set<Item> C;
    set<Item> oldC;
    set<Item>::iterator sit = I.begin();
    for (Item i : I) {C.insert(i);}

    // loop while C and oldC are not the same
    while(!itemSetsEqual(C, oldC)) {
        // set oldC to be equal to C
        sit = C.begin();
        for (Item i : C) {oldC.insert(i);}

        // iterate through every item in C
        sit = C.begin();
        for (Item i : C) {
            // if the item has its "dot" not immediately in front
            // of a nonterminal, move on to the next item
            string afterDot = i.getRule().getRHS()[i.getIndex()];
            if (isNonTerminal(afterDot)) {
                // iterate thorugh all the rules of that nonterminal
                set<Rule> afterDotRules = getRulesDerivedFrom(afterDot);
                set<Rule>::iterator sit2 = afterDotRules.begin();
                for (Rule rule : afterDotRules) {
                    // if a rule is not in C, create a fresh start
                    // for that rule and add it to C
                    bool isInC = false;
                    set<Item>::iterator sit3 = C.begin();
                    for (Item item : C) {
                        if (item.getRuleID() == getRuleID(rule)) {
                            isInC = true;
                        }
                    }

                    if (!isInC) {
                        Item freshStart (rule, getRuleID(rule), 0);
                        C.insert(freshStart);
                    }
                }
            }
            else continue;
        }
    }

    return C;

    // TODO Test Closure
}
set<Item> Grammar::GoTo(set<Item> I) {

    // TODO Implement GoTo
}

// Other
void Grammar::print() {
    for (int i = 0; i < ruleCount; ++i) {
        cout << to_string(i) << "\t";
        cout << rules[i].getLHS() << " ->";
        vector<string> rhs = rules[i].getRHS();
        for (int j = 0; j < rhs.size(); ++j) {
            cout << " " << rhs[j];
        }
        cout << endl;
    }
}
void Grammar::report() {
    // print cfg
    print();

    // print nonterminals
    cout << "\nNonterminals: ";
    set<string>::iterator sit = nonterminals.begin();
    for (string nt : nonterminals) {
        cout << nt << " ";
    }

    // print terminals
    cout << "\nTerminals: ";
    sit = terminals.begin();
    for (string t : terminals) {
        cout << t << " ";
    }

    // print all nonterminals that derive to lambda
    cout << "\n\nDerives To Lambda: ";
    sit = nonterminals.begin();
    for (string nt : nonterminals) {
        if (derivesToLambda(nt)) cout << nt << " ";
    }

    // print all first sets of nonterminals
    cout << endl << endl;
    sit = nonterminals.begin();
    for (string nt : nonterminals) {
        cout << "First(" << nt << "):";
        set<string> first = firstSet(nt);
        set<string>::iterator sit2 = first.begin();
        for (string symbol : first) {
            cout << " " << symbol;
        }
        cout << endl;
    }

    // print all follow sets of nonterminals
    cout << endl << endl;
    sit = nonterminals.begin();
    for (string nt : nonterminals) {
        cout << "Follow(" << nt << "):";
        set<string> follow = followSet(nt);
        set<string>::iterator sit2 = follow.begin();
        for (string symbol : follow) {
            cout << " " << symbol;
        }
        cout << endl;
    }

    // print all predict sets of rules
    cout << endl << endl;
    map<int, Rule>::iterator mit = rules.begin();
    for (pair<int, Rule> rule : rules) {
        cout << "Predict( Rule " << to_string(rule.first) << " ):";

        set<string> predict = predictSet(rule.first);
        sit = predict.begin();
        for (string terminal : predict) {cout << " " << terminal;}
        cout << endl;
    }

    // print ambiguity status
    cout << "\nAmbiguous: ";
    if (ambiguous) cout << "YES\n";
    else cout << "NO\n";

    // if not ambiguous, print the LL(1) table
    if (!ambiguous) {
        printLL1Table();
    }
    
    cout << endl;
}
void Grammar::printLL1Table() {
    // set a spacing string
    string spacing = "\t";

    // output all the terminals
    cout << endl << "LL(1) Parse Table" << endl << endl << spacing;
    set<string>::iterator sit = terminals.begin();
    for (string t : terminals) cout << t << spacing;
    cout << endl << endl;

    // for each row in the table, output the nt and all the numbers
    map<string, vector<int>>::iterator mit = ll1_table.begin();
    for (pair<string, vector<int>> row : ll1_table) {
        cout << row.first;
        for (int i = 0; i< row.second.size(); ++i) {
            cout << spacing;
            if (row.second[i] == -1) cout << "_";
            else cout << to_string(row.second[i]);
        }
        cout << endl << endl;
    }
}
string Grammar::trimEdges(string in) {
    // trim excess spaces from front and back of string
    while (in[0] == ' ') {
        in = in.substr(1, in.length()-1);
    }
    while (in[in.length()-1] == ' ') {
        in = in.substr(0, in.length()-1);
    }
    return in;
}
set<Rule> Grammar::getRulesDerivedFrom(string nonterminal) {
    // create set to return
    set<Rule> rulesFromNT;
    
    // iterate through rules adding ones with nonterminal
    // as the LHS to the set
    map<int, Rule>::iterator mit = rules.begin();
    for (pair<int, Rule> rule : rules) {
        string lhs = rule.second.getLHS();
        if (lhs == nonterminal) rulesFromNT.insert(rule.second);
    }

    // return set
    return rulesFromNT;
}
set<Rule> Grammar::getRulesDerivedTo(string symbol) {
    // create set to return
    set<Rule> rulesToSymbol;
    
    // iterate through rules adding ones with nonterminal
    // as the LHS to the set
    map<int, Rule>::iterator mit = rules.begin();
    for (pair<int, Rule> rule : rules) {
        vector<string> rhs = rule.second.getRHS();
        for (int i = 0; i < rhs.size(); ++i) {
            if (rhs[i] == symbol) {
                rulesToSymbol.insert(rule.second);
                break;
            }
        }
    }

    // return set
    return rulesToSymbol;
}
bool Grammar::isTerminal(string symbol) {
    set<string>::iterator sit = terminals.begin();
    for (string t : terminals) {
        if (symbol == t) return true;
    }
    return false;
}
bool Grammar::isNonTerminal(string symbol) {
    set<string>::iterator sit = nonterminals.begin();
    for (string nt : nonterminals) {
        if (symbol == nt) return true;
    }
    return false;
}
bool Grammar::itemSetsEqual(set<Item> iOne, set<Item> iTwo) {
    // check if same size
    if (iOne.size() != iTwo.size()) return false;

    // check if every item in first set is in the second as well
    set<Item>::iterator sit = iOne.begin();
    for (Item i : iOne) {
        if (iTwo.find(i) == iTwo.end()) return false;
    }

    return true;
}
bool Grammar::rulesEqual(Rule rOne, Rule rTwo) {
    if (rOne.getLHS() != rTwo.getLHS()) return false;
    if (rOne.getSymbolCount() != rTwo.getSymbolCount()) return false;
    for (int i = 0; i < rOne.getSymbolCount(); ++i) {
        if (rOne.getRHS()[i] != rTwo.getRHS()[i]) return false;
    }
    return true;
}