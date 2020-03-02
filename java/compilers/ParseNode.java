package compilers;

import java.util.List;

class ParseNode {
	private Symbol identifier;
	private ParseNode parent;
	private List<ParseNode> children;
}
