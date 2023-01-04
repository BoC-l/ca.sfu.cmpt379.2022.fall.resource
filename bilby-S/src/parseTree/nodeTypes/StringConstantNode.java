package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;
import tokens.StringToken;

public class StringConstantNode extends ParseNode {

    public StringConstantNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visit(this);
    }

    public String getValue() {
        return ((StringToken) token).getValue();
    }
}
