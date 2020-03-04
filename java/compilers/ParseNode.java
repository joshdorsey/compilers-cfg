package compilers;

import java.util.List;
import java.util.LinkedList;

class ParseNode {
	private Symbol identifier;
	private ParseNode parent;
	private List<ParseNode> children;

	ParseNode() {
		identifier = new Symbol("");
		parent = null;
		children = new LinkedList<>();
	}
	
	ParseNode(Symbol symbol) {
		identifier = symbol;
		parent = null;
		children = new LinkedList<>();
	}

	ParseNode getParent() {
		return parent;
	}

	void addChild(ParseNode child) {
		child.parent = this;
		children.add(child);
	}
}
