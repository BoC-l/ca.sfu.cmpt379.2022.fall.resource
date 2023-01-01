package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class AssignmentStatementNode extends ParseNode {

    public AssignmentStatementNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public static ParseNode withChildren(Token token, ParseNode identifier, ParseNode expression) {
        AssignmentStatementNode node = new AssignmentStatementNode(token);
        node.appendChild(identifier);
        node.appendChild(expression);
        return node;
    }

}
