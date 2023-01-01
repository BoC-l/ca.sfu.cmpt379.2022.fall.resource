package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class CallStatementNode extends ParseNode {

    public CallStatementNode(Token token) {
        super(token);
    }

    ///////////////////////////////////////////////////////////
    // boilerplate for visitors
    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public static ParseNode withChildren(Token token, ParseNode functionInvocation) {
        CallStatementNode node = new CallStatementNode(token);
        node.appendChild(functionInvocation);
        return node;
    }

}
