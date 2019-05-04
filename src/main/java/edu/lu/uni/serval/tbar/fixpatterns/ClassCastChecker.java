package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * For a class-casting statement, this template inserts an if() statement
 * checking that the castee is an object of the casting type (using instanceof
 * operator).
 * 
 * Context: CastExpression.
 * 
 * @author kui.liu
 *
 */
public class ClassCastChecker extends FixTemplate {
	/*
	 * PAR	ClassCastChecker
	 * FB	BCUnconfirmedCast: Fix patterns for BC_UNCONFIRMED_CAST and BC_UNCONFIRMED_CAST_OF_RETURN_VALUE violations.
	 * 
	 * Fuzzy fix patterns:
	 * SimFix	InsIfStmt
	 * SOFix	IfChecker
	 * SketchFix If-condition transformation
	 * 
	 * 
	 * Fix Pattern:
	 * Insert instanceof checking.
	 * 	   -  var_ = (T) var;
	 *     +  if (var instanceof T) {
	 *     +	  var_ = (T) var;
	 *     		  ...
	 *     +  } else {throw new IllegalArgumentException(...);}
	 * 
	 * 1. VariableDeclarationStatement.
	 * 2. Exp: Assignment.
	 * 3. One expression in the statement.
	 * 
	 */
	
	@Override
	public void generatePatches() {
		ITree suspStmtTree = this.getSuspiciousCodeTree();
		Map<ITree, String> castExps = identifyCastExpressions(suspStmtTree);
		if (castExps.isEmpty()) return;
		
		for (Map.Entry<ITree, String> entity : castExps.entrySet()) {
			//Generate Patches with CastExpression.
			ITree castExp = entity.getKey();
			String varName = entity.getValue();
			ITree castingType = castExp.getChild(0);
			int castTypeStartPos = castingType.getPos();
			int castTypeEndPos = castTypeStartPos + castingType.getLength();
			String castTypeStr = this.getSubSuspiciouCodeStr(castTypeStartPos, castTypeEndPos);
			if (castTypeStr.equals("double") || castTypeStr.equals("float") || castTypeStr.equals("long")
					|| castTypeStr.equals("int") || castTypeStr.equals("short") || castTypeStr.equals("byte")|| castTypeStr.equals("char")) continue;
			ITree castedExp = castExp.getChild(1);
			int castedExpStartPos = castedExp.getPos();
			int castedExpEndPos = castedExpStartPos + castedExp.getLength();
			String castedExpStr = this.getSubSuspiciouCodeStr(castedExpStartPos, castedExpEndPos);
			int castedExpType = castedExp.getType();
			
			int endPosition = this.suspCodeEndPos;
			if (!"".equals(varName)) { // statements related to variable.
				endPosition = ContextReader.identifyRelatedStatements(suspStmtTree, varName, this.suspCodeEndPos);
			}
			
			String fixedCodeStr1 = "";
			if (Checker.isSimpleName(castedExpType) || Checker.isFieldAccess(castedExpType) 
					|| Checker.isQualifiedName(castedExpType) || Checker.isSuperFieldAccess(castedExpType)) {
				// BC_UNCONFIRMED_CAST, PAR
				fixedCodeStr1 = "if (" + castedExpStr + " instanceof " + castTypeStr + ") {\n\t";
			} else if (Checker.isComplexExpression(castedExpType)) {
				// PAR
				fixedCodeStr1 = "Object _tempVar = " + castedExpStr + ";\n\t" +
						"if (_temVar instanceof " + castTypeStr + ") {\n\t";
				String fixedCodeStr2 = "\n\t} else {\n\tthrow new IllegalArgumentException(\"Illegal argument: " + castedExpStr + "\");\n}\n";
				generatePatch(suspCodeStartPos, endPosition, fixedCodeStr1, fixedCodeStr2);
				
				// BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
				fixedCodeStr1 = "Object _tempVar = " + castedExpStr + ";\n\t" +
								"if (_temVar != null && _temVar instanceof " + castTypeStr + ") {\n\t";
				 this.getSuspiciousCodeStr().replace(castedExpStr, "_temVar");
			}
			
			String fixedCodeStr2 = "\n\t} else {\n\tthrow new IllegalArgumentException(\"Illegal argument: " + castedExpStr + "\");\n}\n";
			generatePatch(suspCodeStartPos, endPosition, fixedCodeStr1, fixedCodeStr2);
		}
	}

	protected Map<ITree, String> identifyCastExpressions(ITree codeAst) {
		Map<ITree, String> castExps = new HashMap<>();
		
		List<ITree> children = codeAst.getChildren();
		if (children == null || children.isEmpty()) return castExps;
		
		int astNodeType = codeAst.getType();
		if (Checker.isVariableDeclarationStatement(astNodeType)) {
			boolean isType = true; // Identity data type
			for (ITree child : children) {
				int childNodeType = child.getType();
				if (Checker.isModifier(childNodeType)) {
					continue;
				}
				if (isType) { // Type Node.
					isType = false;
				} else if (Checker.isStatement(childNodeType)) {
					break;
				} else { //VariableDeclarationFragment(s)
					String varName = child.getChild(0).getLabel();
					if (child.getChildren().size() > 1) {
						ITree assignedExp = child.getChild(1);
						if (Checker.isCastExpression(assignedExp.getType())) {
							castExps.put(assignedExp, varName);
						}
						castExps.putAll(identifyCastExpressions(assignedExp));
					}
				}
			}
		} else if (Checker.isExpressionStatement(astNodeType)) {
			ITree expAst = children.get(0);
			int expAstType = expAst.getType();
			if (Checker.isAssignment(expAstType)) {
				String varName = expAst.getChild(0).getLabel();
				ITree subExpAst = expAst.getChild(2);
				int subExpType = subExpAst.getType();
				if (Checker.isCastExpression(subExpType)) {
					castExps.put(subExpAst, varName);
				}
				castExps.putAll(identifyCastExpressions(subExpAst));
			} else { // Other expressions.
				castExps.putAll(identifyCastExpressions(expAst));
			}
		} else if (Checker.isReturnStatement(astNodeType)) {
			ITree exp = children.get(0);
			int expType = exp.getType();
			if (Checker.isReturnStatement(expType)) { // Empty return statement, i.e., "return;".
			} else {
				if (Checker.isCastExpression(expType)) {
					castExps.put(exp, "");
				}
				castExps.putAll(identifyCastExpressions(exp));
			}
		} else if (Checker.isFieldDeclaration(astNodeType)) {
			// FIXME: we ignore this situation in the current version.
		} else if(Checker.isComplexExpression(astNodeType) || Checker.isSimpleName(astNodeType)) { // expressions
			for (ITree child : children) {
				int childType = child.getType();
				if (Checker.isComplexExpression(childType)) {
					if (Checker.isCastExpression(childType)) {
						castExps.put(child, "");
					}
					castExps.putAll(identifyCastExpressions(child));
				} else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
					castExps.putAll(identifyCastExpressions(child));
				} else if (Checker.isStatement(childType)) break;
			}
		}
		return castExps;
	}

}
