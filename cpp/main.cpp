/*
    Jamison Hubbard
    March 2020
    CFG Implementation
    rule.cpp
*/

using namespace std;

#include <string>
#include "grammar.h"

int main(int argc, char *argv[]) {
    if (argc != 2) exit(3);

    string filename = argv[1];

    Grammar g(filename);
    g.report();

    return 0;
}