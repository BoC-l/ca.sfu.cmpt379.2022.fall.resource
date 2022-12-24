package symbolTable;

import java.util.HashMap;

import logging.BilbyLogger;
import tokens.Token;

public class SymbolTable extends HashMap<String, Binding> {	
	
	///////////////////////////////////////////////////////////////////////
	//error reporting

	public void errorIfAlreadyDefined(Token token) {
		if(containsKey(token.getLexeme())) {		
			multipleDefinitionError(token);
		}
	}
	protected static void multipleDefinitionError(Token token) {
		BilbyLogger log = BilbyLogger.getLogger("compiler.symbolTable");
		log.severe("variable \"" + token.getLexeme() + 
				          "\" multiply defined at " + token.getLocation());
	}

	///////////////////////////////////////////////////////////////////////
	// toString

	public String toString() {
		StringBuffer result = new StringBuffer("    symbol table: \n");
		this.entrySet().forEach((entry) -> {
			result.append("        " + entry + "\n");
		});
		return result.toString();
	}
}
