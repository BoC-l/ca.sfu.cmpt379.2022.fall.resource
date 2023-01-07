package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class CastNode extends ParseNode {

    public CastNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public static ParseNode withChildren(Token castToken, ParseNode expression, ParseNode type) {
        CastNode result = new CastNode(castToken);
        result.appendChild(expression);
        result.appendChild(type);
        return result;
    }
}
