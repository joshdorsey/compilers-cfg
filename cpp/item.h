/*
    Jamison Hubbard
    April 2020
    CFG Implementation
    item.h
*/

#ifndef ITEM_H
#define ITEM_H

using namespace std;

#include "rule.h"

class Item {
public:
    // Constructors
    Item();
    Item(Rule r, int id, int ind);

    // Access Functions
    Rule getRule();
    int getRuleID();
    int getIndex();
    int getSymbolCount();

    // Other
    bool operator<(const Item &other) const;

private:
    Rule rule;
    int ruleID;

    // index denotes the index of the symbol that
    // the progress marker ("dot") is in front of
    // e.g. index = 0 is a fresh start
    // and index will never equal symbolCount
    int index;
    int symbolCount;
};

#endif /*ITEM_H*/