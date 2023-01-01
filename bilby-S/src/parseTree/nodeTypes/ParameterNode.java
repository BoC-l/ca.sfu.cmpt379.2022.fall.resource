package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ParameterNode extends ParseNode {

    public ParameterNode(Token token) {
        super(token);
    }

    public static ParseNode withChildren(Token nowReading, ParseNode type, ParseNode identifier) {
        ParameterNode node = new ParameterNode(nowReading);
        node.appendChild(type);
        node.appendChild(identifier);
        return node;
    }

    ///////////////////////////////////////////////////////////
    // boilerplate for visitors
    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }
}
