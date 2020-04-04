/*
    Jamison Hubbard
    April 2020
    CFG Implementation
    item.cpp
*/

using namespace std;

#include "rule.h"
#include "item.h"

// Constructors
Item::Item() {
    ruleID = -1;
    index = -1;
    symbolCount = -1;
}

Item::Item(Rule r, int id, int ind) {
    rule = r;
    ruleID = id;
    index = ind;
    symbolCount = rule.getRHS().size();
}

// Access Functions
Rule Item::getRule() {return rule;}
int Item::getRuleID() {return ruleID;}
int Item::getIndex() {return index;}
int Item::getSymbolCount() {return symbolCount;}

// Other
// Other
bool Item::operator<(const Item &other) const {
    // this operator is defined because std::set containers use the
    // < operator to determine if two objects are the same (I think).
    // So this operator tests if the items are the same and returns
    // true if they're not so std::set handles them as different.

    if (symbolCount != other.symbolCount) return true;
    if (index != other.index) return true;
    if (ruleID != other.ruleID) return true;
    return false;
}