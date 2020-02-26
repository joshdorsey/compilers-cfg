/*
Jamison Hubbard
CFG Implementation
CSCI 498A Compilers 
Feb 2020

main.cpp
*/

#include <iostream>
#include <string>

#include "cfg.h"

using namespace std;

void help();
void test(CFG g);

int main(int argc, char *argv[]) {

    // input verification
    if (argc != 3) {
        if (argc == 2) {
            string helpMessage = argv[1];
            if (helpMessage == "--help") {
                help();
                exit(0);
            }
        }

        cout << "Error: Wrong number of inputs" << endl;
        cout << "type \"./CFG --help\" for help" << endl;
        exit(4);
    }

    // input collection
    string filename = argv[1];
    string action = argv[2];

    // generate CFG from file
    CFG grammar("../" + filename);

    // take given action
    if (action == "--print") grammar.printMap();
    else if (action == "--help") help();
    else if (action == "--test") test(grammar);

    // action code not recognized


    return 0;
}

void help() {
    cout << endl << "Welcome to CFG Implementation!" << endl;
    cout << endl << "Correct Syntax: \"./CFG exampleFile.cfg --action_code\"" << endl;
    
    cout << endl << "Action Codes: " << endl;
    cout << "\t--print\t\tPrints the CFG and its basic properties\n";
    cout << "\t--help\t\tPrints this help message\n";
    cout << "\t--test\t\tRun whatever test is set to run\n";

    cout << endl << "Error Codes: " << endl;
    cout << "\t0\t\tSuccessful Execution of CFG\n";
    cout << "\t1\t\tFile could not be opened\n";
    cout << "\t2\t\tMultiple productions include \"$\". Incorrect input format of CFG\n";
    cout << "\t3\t\tProductions requested for non-existent non-terminal\n";
    cout << "\t4\t\tWrong number of inputs given\n";
}

void test(CFG g) {

    // Current Test: firstSet()
    
    // g.testDerivesToLambda();

    g.testFirstSet();
}