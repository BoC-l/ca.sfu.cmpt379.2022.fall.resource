package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.CharacterToken;
import tokens.Token;

public class CharConstantNode extends ParseNode {

    public CharConstantNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visit(this);
    }

    public char getValue() {
        return ((CharacterToken) token).getValue();
    }
}
