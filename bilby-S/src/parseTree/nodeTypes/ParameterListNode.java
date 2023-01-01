package parseTree.nodeTypes;

import java.util.ArrayList;
import java.util.List;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class ParameterListNode extends ParseNode {

    public ParameterListNode(Token token) {
        super(token);
    }

    ///////////////////////////////////////////////////////////
    // boilerplate for visitors
    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public List<Type> getTypes() {
        List<Type> types = new ArrayList<Type>();
        for (int i = 0; i < nChildren(); i++) {
            types.add(child(i).getType());
        }
        return types;
    }
}
