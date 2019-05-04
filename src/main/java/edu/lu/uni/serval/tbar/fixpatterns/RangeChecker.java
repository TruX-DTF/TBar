package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

public class RangeChecker extends FixTemplate {
	
	/*
	 * PAR	      RangeChecker
	 * ELIXIR	  T4_ArrayRangeAndCollectionSize
	 * PAR	      CollectionSizeChecker
	 * 
	 * Fuzzy fix patterns:
	 * SOFix	  IfChecker
	 * SketchFix  ConditionTransform
	 * SimFix	  InsIfStmt
	 *
	 */
	
	private boolean isArrayAccess = false;
	
	public RangeChecker(boolean isArrayAccess) {
		this.isArrayAccess = isArrayAccess;
	}

	@Override
	public void generatePatches() {
		ITree suspCodeTree = getSuspiciousCodeTree();
		if (isArrayAccess)
			new ArrayRangeChecker().generatePatches(suspCodeTree);
		else new CollectionRangeChecker().generatePatches(suspCodeTree);
	}
	
	private class ArrayRangeChecker {
		protected void generatePatches(ITree suspCodeTree) {
			List<Pair<ITree, ITree>> allSuspiciousArrayVars = identifyAllSuspiciousArrayAccesses(suspCodeTree);
			
			if (allSuspiciousArrayVars.isEmpty()) return;
			
			StringBuilder conditionalExp = new StringBuilder();
			Pair<ITree, ITree> firstPair = allSuspiciousArrayVars.get(0);
			ITree suspArrayExp = firstPair.getFirst();
			ITree indexExp = firstPair.getSecond();
			int suspArrayExpStartPos = suspArrayExp.getPos();
			int suspArrayExpEndPos = suspArrayExpStartPos + suspArrayExp.getLength();
			int indexExpStartPos = indexExp.getPos();
			int indexExpEndPos = indexExpStartPos + indexExp.getLength();
			
			String suspArrayExpStr = getSubSuspiciouCodeStr(suspArrayExpStartPos, suspArrayExpEndPos);
			String indexExpStr = getSubSuspiciouCodeStr(indexExpStartPos, indexExpEndPos);
			
			if (!"0".equals(indexExpStr)) {
				conditionalExp.append("(").append(indexExpStr).append(" < ").append(suspArrayExpStr).append(".length())");
			}
			
			for (int index = 1, size = allSuspiciousArrayVars.size(); index < size; index ++) {
				Pair<ITree, ITree> pair = allSuspiciousArrayVars.get(index);
				suspArrayExp = pair.getFirst();
				indexExp = pair.getSecond();
				suspArrayExpStartPos = suspArrayExp.getPos();
				suspArrayExpEndPos = suspArrayExpStartPos + suspArrayExp.getLength();
				indexExpStartPos = indexExp.getPos();
				indexExpEndPos = indexExpStartPos + indexExp.getLength();
				
				suspArrayExpStr = getSubSuspiciouCodeStr(suspArrayExpStartPos, suspArrayExpEndPos);
				indexExpStr = getSubSuspiciouCodeStr(indexExpStartPos, indexExpEndPos);
				if ("0".equals(indexExpStr)) continue;
				if (conditionalExp.length() == 0) {
					conditionalExp.append("(");
				} else {
					conditionalExp.append(" && (");
				}
				conditionalExp.append(indexExpStr).append(" < ").append(suspArrayExpStr).append(".length())");
			}
			
			if (conditionalExp.length() == 0) return;
			if (Checker.isReturnStatement(suspCodeTree.getType())) {
				String fixedCodeStr1 = "if (!(" + conditionalExp.toString() + ")) return ";
				String returnType = ContextReader.readMethodReturnType(suspCodeTree);
				if (returnType != null && !returnType.isEmpty()) {
					if ("void".equals(returnType)) {
						fixedCodeStr1 += ";\n";
					} else if ("float".equals(returnType) || "double".equals(returnType)) {
						fixedCodeStr1 += " 0.0;\n";
					} else if ("int".equals(returnType) || "long".equals(returnType)) {
						fixedCodeStr1 += " 0;\n";
					} else if ("boolean".equalsIgnoreCase(returnType)) {
						fixedCodeStr1 += " false;\n";
					} else {
						fixedCodeStr1 += " null;\n";
					}
					generatePatch(suspCodeStartPos, fixedCodeStr1);
				}
			} else {
				String varName = null;
				int codeEndPos = suspCodeEndPos;
				if (Checker.isVariableDeclarationStatement(suspCodeTree.getType()) ||
						(Checker.isExpressionStatement(suspCodeTree.getType()) && Checker.isAssignment(suspCodeTree.getChild(0).getType()))) {
					varName = ContextReader.identifyVariableName(suspCodeTree);
				}
				if (varName != null) {
					codeEndPos = ContextReader.identifyRelatedStatements(suspCodeTree, varName, codeEndPos);
				}
				
				String fixedCodeStr1 = "if (" + conditionalExp.toString() + ") {\n\t";
				String fixedCodeStr2 = "\n}\n";
				
				generatePatch(suspCodeStartPos, codeEndPos, fixedCodeStr1, fixedCodeStr2);
			}
		}
		
		private List<Pair<ITree, ITree>> identifyAllSuspiciousArrayAccesses(ITree suspCodeTree) {
			List<Pair<ITree, ITree>> allSuspiciousArrayVars= new ArrayList<>();
			List<ITree> children = suspCodeTree.getChildren();
			for (ITree child : children) {
				int type = child.getType();
				if (Checker.isStatement(type)) break;
				else if (Checker.isArrayAccess(type)) {
					ITree arrayExp = child.getChild(0);
					ITree indexExp = child.getChild(1);
					if (Checker.isComplexExpression(arrayExp.getType())) {
						allSuspiciousArrayVars.addAll(identifyAllSuspiciousArrayAccesses(arrayExp));
					}
					allSuspiciousArrayVars.add(new Pair<ITree, ITree>(arrayExp, indexExp));
				} else if (Checker.isComplexExpression(type)) {
					allSuspiciousArrayVars.addAll(identifyAllSuspiciousArrayAccesses(child));
				} else if (Checker.isSimpleName(type) && child.getLabel().startsWith("MethodName:")) {
					allSuspiciousArrayVars.addAll(identifyAllSuspiciousArrayAccesses(child));
				}
			}
			return allSuspiciousArrayVars;
		}
	}
	
	private class CollectionRangeChecker {
		protected void generatePatches(ITree suspStmtTree) {
			List<Pair<ITree, ITree>> methodInvocations = identifyMethodInvocations(suspStmtTree);
			if (methodInvocations.isEmpty()) return;
			String varName = null;
			String returnType = null;
			int codeEndPos = suspCodeEndPos;
			
			if (Checker.isReturnStatement(suspStmtTree.getType())) {
				returnType = ContextReader.readMethodReturnType(suspStmtTree);
			} else {
				varName = identifyVarName(suspStmtTree);
				if (varName != null) {
					codeEndPos = ContextReader.identifyRelatedStatements(suspStmtTree, varName, codeEndPos);
				}
			}
			
			for (Pair<ITree, ITree> pair : methodInvocations) {
				ITree collectionExp = pair.getFirst();
				ITree indexExp = pair.getSecond();
				
				int collectionExpStartPos = collectionExp.getPos();
				int collectionExpEndPos = collectionExpStartPos + collectionExp.getLength();
				String collectionExpStr = getSubSuspiciouCodeStr(collectionExpStartPos, collectionExpEndPos);
				int indexExpStartPos = indexExp.getPos();
				int indexExpEndPos = indexExpStartPos + indexExp.getLength();
				String parameterExpStr = getSubSuspiciouCodeStr(indexExpStartPos, indexExpEndPos);
				
				if (returnType != null) {
					String fixedCodeStr1 = "if (" + parameterExpStr + " >= " + collectionExpStr + ".size()) return ";
					if (returnType != null && !returnType.isEmpty()) {
						if ("void".equals(returnType)) {
							fixedCodeStr1 += ";\n";
						} else if ("float".equals(returnType) || "double".equals(returnType)) {
							fixedCodeStr1 += " 0.0;\n";
						} else if ("int".equals(returnType) || "long".equals(returnType)) {
							fixedCodeStr1 += " 0;\n";
						} else if ("boolean".equalsIgnoreCase(returnType)) {
							fixedCodeStr1 += " false;\n";
						} else {
							fixedCodeStr1 += " null;\n";
						}
						generatePatch(suspCodeStartPos, fixedCodeStr1);
					}
				} else {
					String fixedCodeStr1 = "if (" + parameterExpStr + " < " + collectionExpStr + ".size()) {\n\t";
					String fixedCodeStr2 = "\n} else {\n\tthrow new IndexOutOfBoundsException(\"too big index: " + parameterExpStr + "\");\n}";
					generatePatch(suspCodeStartPos, codeEndPos, fixedCodeStr1, fixedCodeStr2);
				}
			}
		}
		
		private List<Pair<ITree, ITree>> identifyMethodInvocations(ITree codeAst) {
			List<Pair<ITree, ITree>> methodInvocations = new ArrayList<>();
			
			List<ITree> children = codeAst.getChildren();
			if (children == null || children.isEmpty()) return methodInvocations;
			
			for (ITree child : children) {
				int childNodeType = child.getType();
				if (Checker.isMethodInvocation(childNodeType)) {
					List<ITree> subChildren = child.getChildren();
					for (int index = 0, size = subChildren.size(); index < size; index ++) {
						ITree subChild = subChildren.get(index);
						int subChildType = subChild.getType();
						String label = subChild.getLabel();
						if (Checker.isSimpleName(subChildType) && label.startsWith("MethodName:get:")) {
							// only one parameter.
							if (index + 2 == size) {
								ITree parameterAst = subChildren.get(index + 1);
								methodInvocations.add(new Pair<ITree, ITree>(subChild, parameterAst));
							}
						}
					}
				}
				if (Checker.isComplexExpression(childNodeType)) {
					methodInvocations.addAll(identifyMethodInvocations(child));
				} else if (Checker.isStatement(childNodeType)) break;
			}
			
			return methodInvocations;
		}
		
		private String identifyVarName(ITree suspStmtTree) {
			List<ITree> children = suspStmtTree.getChildren();
			if (children == null || children.isEmpty()) return null;
			
			int suspStmtType = suspStmtTree.getType();
			if (Checker.isVariableDeclarationStatement(suspStmtType)) {
				boolean isType = true; // Identity data type
				for (ITree child : children) {
					int childNodeType = child.getType();
					if (Checker.isModifier(childNodeType)) {
						continue;
					}
					if (isType) { // Type Node.
						isType = false;
					} else { //VariableDeclarationFragment(s)
						return child.getChild(0).getLabel();
					}
				}
			} else if (Checker.isExpressionStatement(suspStmtType)) {
				ITree expAst = children.get(0);
				int expAstType = expAst.getType();
				if (Checker.isAssignment(expAstType)) {
					return expAst.getChild(0).getLabel();
				}
			}
			return null;
		}
	}
	
}
