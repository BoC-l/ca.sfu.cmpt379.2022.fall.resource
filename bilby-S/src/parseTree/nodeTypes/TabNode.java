package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class TabNode extends ParseNode {

    public TabNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visit(this);
    }
}
