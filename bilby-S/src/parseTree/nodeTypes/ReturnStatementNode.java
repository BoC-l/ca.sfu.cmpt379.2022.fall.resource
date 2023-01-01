package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ReturnStatementNode extends ParseNode {

    public ReturnStatementNode(Token token) {
        super(token);
    }

    public static ReturnStatementNode withChildren(Token token, ParseNode expression) {
        ReturnStatementNode node = new ReturnStatementNode(token);
        node.appendChild(expression);
        return node;
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public FunctionDefinitionNode getFunctionDefinitionNode() {
        ParseNode node = this.getParent();
        while (node != null) {
            if (node.getClass() == FunctionDefinitionNode.class) {
                return (FunctionDefinitionNode) node;
            }
            node = node.getParent();
        }
        return null;
    }

}
