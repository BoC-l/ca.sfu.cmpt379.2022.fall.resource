package symbolTable;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class Binding {
	private Type type;
	private MemoryLocation memoryLocation;
	private String lexeme;
	
	public Binding(Type type, MemoryLocation memoryLocation, String lexeme) {
		this.type = type;
		this.memoryLocation = memoryLocation;
		this.lexeme = lexeme;
	}

	public String toString() {
		return "[" + lexeme +
				" " + type +	
				" " + memoryLocation +
				"]";
	}

	public Type getType() {
		return type;
	}

	public void generateAddress(ASMCodeFragment code) {
		memoryLocation.generateAddress(code, "%% " + lexeme);
	}

////////////////////////////////////////////////////////////////////////////////////
//Null Binding object
////////////////////////////////////////////////////////////////////////////////////

    private static final Binding nullInstance = new Binding(
        PrimitiveType.ERROR,
        MemoryLocation.nullInstance(),
        "the-null-binding");

	public static Binding nullInstance() {
		return nullInstance;
	}
}
