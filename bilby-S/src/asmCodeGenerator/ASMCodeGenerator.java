package asmCodeGenerator;

import java.util.HashMap;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.AssignmentStatementNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CharConstantNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ExpressionListNode;
import parseTree.nodeTypes.FunctionDefinitionNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.OperatorNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.SpaceNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	ParseNode root;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}
	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
	}
	
	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		code.append( RunTime.getEnvironment() );
		code.append( globalVariableBlockASM() );
		code.append( programASM() );
//		code.append( MemoryManager.codeForAfterApplication() );
		
		return code;
	}
	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();
		
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}
	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		code.add(    Label, RunTime.MAIN_PROGRAM_LABEL);
		code.append( programCode());
		
		return code;
	}
	private ASMCodeFragment programCode() {
        ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		code.append(visitor.removeRootCode(root));
        code.add(Halt);
        code.append(visitor.functions);
        return code;
	}


	protected class CodeVisitor extends ParseNodeVisitor.Default {
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;
        ASMCodeFragment functions;
		
		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
            functions = new ASMCodeFragment(GENERATES_VOID);
		}


		////////////////////////////////////////////////////////////////////
        // Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}
		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}
		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

	    ////////////////////////////////////////////////////////////////////
        // Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(node);
			return result;
		}
	    public  ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}		
		ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}		
		private ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}		
		ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}
		
	    ////////////////////////////////////////////////////////////////////
        // convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();
			
			if(code.isAddress()) {
				turnAddressIntoValue(code, node);
			}	
		}
		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			if(node.getType() == PrimitiveType.INTEGER) {
				code.add(LoadI);
			}	
			else if(node.getType() == PrimitiveType.BOOLEAN) {
				code.add(LoadC);
			}	
			else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}
		
	    ////////////////////////////////////////////////////////////////////
        // ensures all types of ParseNode in given AST have at least a visitLeave	
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
        public void visitLeave(FunctionDefinitionNode node) {
            // part 1: generate the function code
            ASMCodeFragment functionCode = new ASMCodeFragment(GENERATES_VOID);
            functionCode.add(Label, node.getName());
            // [... ra]
            // prologue

            // dynamic link: mem[sp - 4] <= fp
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            functionCode.add(PushI, 4);
            functionCode.add(Subtract);
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            functionCode.add(StoreI);

            // mem[sp - 8] <= return address
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            functionCode.add(PushI, 8);
            functionCode.add(Subtract);
            // [... ra sp-8]
            functionCode.add(Exchange);
            functionCode.add(StoreI);

            // fp <= sp
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            Macros.storeITo(functionCode, RunTime.FRAME_POINTER);
    
            // reserve space for dynamic link and return address, sp <= sp - 8
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            functionCode.add(PushI, 8);
            functionCode.add(Subtract);
            // reserve space for local variables, sp <= sp - scopeSize
            BlockStatementNode block = (BlockStatementNode) node.child(3);
            functionCode.add(PushI, block.getScope().getAllocatedSize());
            functionCode.add(Subtract);
            Macros.storeITo(functionCode, RunTime.STACK_POINTER);

            functionCode.append(removeVoidCode(node.child(3))); // [... rv?]

            // epilogue
            functionCode.add(Label, node.getEpilogueLabel());

            // restore ra, push to asm stack the value mem[fp - 8]
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            functionCode.add(PushI, 8);
            functionCode.add(Subtract);
            functionCode.add(LoadI);    // [... rv? ra]

            // restore sp <- fp
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            Macros.storeITo(functionCode, RunTime.STACK_POINTER);

            // restore fp
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            functionCode.add(PushI, 4);
            functionCode.add(Subtract);
            functionCode.add(LoadI);    // [... ra fp']
            Macros.storeITo(functionCode, RunTime.FRAME_POINTER);   // [... rv? ra]

            FunctionSignature signature = (FunctionSignature) node.getType();
            if(signature.resultType() != PrimitiveType.VOID) {
                functionCode.add(Exchange); // [... ra rv]
                // put the value on the call stack
                // sp <= sp - 4
                Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
                functionCode.add(PushI, 4);
                functionCode.add(Subtract);
                Macros.storeITo(functionCode, RunTime.STACK_POINTER);
                // mem[sp] <= rv
                Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
                functionCode.add(Exchange);
                functionCode.add(StoreI);
            }
            functionCode.add(Return);
            functions.append(functionCode);
            // part 2: set the function pointer
            newVoidCode(node);
            code.append(removeAddressCode(node.child(1)));
            code.add(PushD, node.getName());
            code.add(StoreI);
        }

        public void visitLeave(BlockStatementNode node) {
            newVoidCode(node);
            for(ParseNode child : node.getChildren()) {
                ASMCodeFragment childCode = removeVoidCode(child);
                code.append(childCode);
            }
        }

		///////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);
			new PrintStatementGenerator(code, this).generate(node);	
		}
		public void visit(NewlineNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
			code.add(Printf);
		}
		public void visit(SpaceNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.SPACE_PRINT_FORMAT);
			code.add(Printf);
		}
		
        public void visitLeave(CallStatementNode node) {
            newVoidCode(node);
            FunctionInvocationNode functionInvocation = (FunctionInvocationNode) node.child(0);
            code.append(removeValueCode(functionInvocation));
            code.add(Pop);
        }

        public void visitLeave(IfStatementNode node) {
            newVoidCode(node);

            Labeller labeller = new Labeller("if");
            String elseLabel = labeller.newLabel("else");
            String endLabel = labeller.newLabel("end");
            boolean hasElseBlock = node.nChildren() == 3;
			
            code.append(removeValueCode(node.child(0)));
            code.add(JumpFalse, hasElseBlock ? elseLabel : endLabel);
            code.append(removeVoidCode(node.child(1)));
            code.add(Jump, endLabel);
            if (hasElseBlock) {
                code.add(Label, elseLabel);
                code.append(removeVoidCode(node.child(2)));
            }
            code.add(Label, endLabel);
        }

        public void visitLeave(ReturnStatementNode node) {            
            newVoidCode(node);

            if (node.nChildren() == 1) {
                code.append(removeValueCode(node.child(0)));
            }

            FunctionDefinitionNode function = node.getFunctionDefinitionNode();
            assert function != null;
            code.add(Jump, function.getEpilogueLabel());
        }

		public void visitLeave(DeclarationNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			
			code.append(lvalue);
			code.append(rvalue);
			
			Type type = node.getType();
			code.add(opcodeForStore(type));
		}
		private ASMOpcode opcodeForStore(Type type) {
			if(type == PrimitiveType.INTEGER) {
				return StoreI;
			}
			if(type == PrimitiveType.BOOLEAN) {
				return StoreC;
			}
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}

        @Override
        public void visitLeave(AssignmentStatementNode node) {
            newVoidCode(node);
            ASMCodeFragment lvalue = removeAddressCode(node.child(0));
            ASMCodeFragment rvalue = removeValueCode(node.child(1));
            code.append(lvalue);
            code.append(rvalue);
            code.add(opcodeForStore(node.child(0).getType()));
        }

		///////////////////////////////////////////////////////////////////////////
		// expressions
		public void visitLeave(OperatorNode node) {
			Lextant operator = node.getOperator();
			
			if(operator == Punctuator.SUBTRACT && node.nChildren() == 1) {
				visitUnaryOperatorNode(node);
			}
			else if(operator == Punctuator.GREATER) {
				visitComparisonOperatorNode(node, operator);
			}
			else {
				visitNormalBinaryOperatorNode(node);
			}
		}
		private void visitComparisonOperatorNode(OperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			code.add(Subtract);
			
			code.add(JumpPos, trueLabel);
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}		
		private void visitUnaryOperatorNode(OperatorNode node) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			
			code.append(arg1);
			
			ASMOpcode opcode = opcodeForOperator(node);
			code.add(opcode);							// type-dependent! (opcode is different for floats and for ints)
		}
		private void visitNormalBinaryOperatorNode(OperatorNode node) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			code.append(arg1);
			code.append(arg2);
			
			ASMOpcode opcode = opcodeForOperator(node);
			code.add(opcode);							// type-dependent! (opcode is different for floats and for ints)
		}
		private ASMOpcode opcodeForOperator(OperatorNode node) {
            Lextant lextant = node.getOperator();
			assert(lextant instanceof Punctuator);
			Punctuator punctuator = (Punctuator)lextant;
            int nOperators = node.nChildren();
			switch(punctuator) {
			case ADD: 	   		return Add;				// type-dependent!
			case SUBTRACT:		return nOperators == 1 ? Negate : Subtract; // (unary subtract only) type-dependent!
			case MULTIPLY: 		return Multiply;		// type-dependent!
			default:
				assert false : "unimplemented operator in opcodeForOperator";
			}
			return null;
		}

        // identifier ( expression-list )
        @Override
        public void visitLeave(FunctionInvocationNode node) {

            // no matter what the return type is,
            // function invocation always returns a value
            newValueCode(node);

            // push arguments to call stack
            ExpressionListNode arguments = (ExpressionListNode) node.child(1);
            int sizeOfArguments = 0;
            // push arguments from right to left
            // ----------
            // | argN-1 |
            // | argN-2 |
            // | ...    |
            // | arg1   |
            // | arg0   | <- sp
            // ----------
            for (int i = arguments.nChildren() - 1; i >= 0; i--) {
                Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... sp]
                Type argumentType = arguments.child(i).getType();
                int argumentSize = argumentType.getSize();
                code.add(PushI, argumentSize); // [... sp size]
                code.add(Subtract); // [... sp - size]
                Macros.storeITo(code, RunTime.STACK_POINTER); // sp <= sp - size, [...]
                Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... sp]
                code.append(removeValueCode(arguments.child(i))); // [... sp value]
                code.add(storeCodeForType(argumentType)); // [...], mem[sp] <= value
                sizeOfArguments += argumentSize;
            }

            // call function
            IdentifierNode function = (IdentifierNode) node.child(0);
            code.append(removeAddressCode(function));
            code.add(LoadI);
            code.add(CallV);

            // get return value from mem[sp] and put it on accumulator stack
            FunctionSignature signature = (FunctionSignature) function.getType();
            Type returnType = signature.resultType();
            if(returnType != PrimitiveType.VOID) {
                // [... rv] <- mem[sp]
                Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... sp]
                code.add(loadForType(returnType)); // [... rv]
            } else {
                code.add(PushI, 0); // [... 0]
            }

            // clean up the stack, pull back the space for arguments and return value
            Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... rv sp]
            code.add(PushI, returnType.getSize() + sizeOfArguments); // [... rv sp size]
            code.add(Add); // [... rv sp + size]
            Macros.storeITo(code, RunTime.STACK_POINTER); // sp <= sp + size, [... rv]
        }

        private static ASMOpcode storeCodeForType(Type argumentType) {
            if(argumentType == PrimitiveType.BOOLEAN) {
                return StoreC;
            }
            if(argumentType == PrimitiveType.INTEGER) {
                return StoreI;
            }
            assert false : "unimplemented type in storeCodeForType";
            return null;
        }

        public ASMOpcode loadForType(Type returnType) {
            if(returnType == PrimitiveType.BOOLEAN) {
                return LoadC;
            }
            if(returnType == PrimitiveType.INTEGER) {
                return LoadI;
            }
            assert false : "unimplemented type in loadForType";
            return null;
        }


        ///////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}
		public void visit(IdentifierNode node) {
			newAddressCode(node);
			Binding binding = node.getBinding();
			
			binding.generateAddress(code);
		}		
		public void visit(IntegerConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValue());
		}
        public void visit(CharConstantNode node) {
            newValueCode(node);
            code.add(PushI, node.getValue());
        }
	}

}
