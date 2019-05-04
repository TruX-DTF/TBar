package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * For a variable declaration statement, 
 * this schema replaces the type of the variable with a widened type – e.g., float to double.
 *
 * Context: variable declaration or cast expression.
 * 
 * @author kui.liu
 *
 */
public class DataTypeReplacer extends FixTemplate {
	
	/*
	 * ELIXIR	T1_WidenType
	 * SimFix   Replacement (TYPE, TYPE).
	 * SOFix	TypeReplacer
	 * 
	 * PAR Caster Mutator TODO https://sites.google.com/site/autofixhkust/home/fix-templates#TOC-Caster-Mutator
	 */
	
	@Override
	public void generatePatches() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		
		if (Checker.isVariableDeclarationStatement(suspCodeTree.getType())) {
			ITree type = null;
			List<ITree> children = suspCodeTree.getChildren();
			for (ITree child : children) {
				if (Checker.isModifier(child.getType())) continue;
				type = child;
				break;
			}
			String dataType = ContextReader.readType(type.getLabel());
			
			// we only focus on the primitive types.
			if ("char".equals(dataType) || "short".equals(dataType) || "byte".equals(dataType)
					|| "int".equals(dataType) || "long".equals(dataType) || "float".equals(dataType) || "double".equals(dataType)) {
				String[] primitiveTypes = {"double", "float", "long", "int", "short", "char", "byte"};
				
				int index = suspCodeTree.getChildPosition(type) + 1;
				List<String> variables = new ArrayList<>();
				for (int size = children.size(); index < size; index ++) {
					variables.add(children.get(index).getChildren().get(0).getLabel());
				}
				List<ITree> peerStmts = suspCodeTree.getParent().getChildren();
	    		ITree lastStmt = null;
	    		index = suspCodeTree.getParent().getChildPosition(suspCodeTree) + 1;
	    		List<Integer> positionsList1 = new ArrayList<>();
	    		for (int size = peerStmts.size(); index < size; index ++) {
	    			ITree stmt = peerStmts.get(index);
	    			List<Integer> posList = new ArrayList<>();
    				identifySameTypes(stmt, dataType, variables, (index == size - 1 ? null : peerStmts.subList(index + 1, size)), posList);
        			if (posList.size() > 0) {
        				lastStmt = stmt;
        				positionsList1.addAll(posList);
        			}
	    		}
				
				int typeStartPos = type.getPos();
				int typeEndPos = typeStartPos + type.getLength();
				String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, typeStartPos);
				String codePart2 = this.getSubSuspiciouCodeStr(typeEndPos, suspCodeEndPos);
				for (String primitiveType : primitiveTypes) {
					if (primitiveType.equals(dataType)) continue;
					String fixedCodeStr1 = codePart1 + primitiveType + codePart2;
					
					if (lastStmt != null) {
						String patch = fixedCodeStr1;
		    			int endPos = this.suspCodeEndPos;
//		    			positionsList = positionsList.stream().distinct().collect(Collectors.toList());// JDK 1.8
		    			List<Integer> positionsList = new ArrayList<>(new HashSet<>(positionsList1));
		    			Collections.sort(positionsList, new Comparator<Integer>() {
							@Override
							public int compare(Integer o1, Integer o2) {
								return o1.compareTo(o2);
							}
		    			});
		    			
		    			int s = positionsList.size();
		    			for (int i = 0; i < s; i ++) {
		    				int prevPos = i == 0 ? endPos : (positionsList.get(i - 1) + dataType.length());
		    				int currPos = positionsList.get(i);
		    				patch += this.suspJavaFileCode.substring(prevPos, currPos) + primitiveType;
						}
		    			int prevPos = positionsList.get(s - 1) + dataType.length();
		    			endPos = lastStmt.getPos() + lastStmt.getLength();
		    			patch += this.suspJavaFileCode.substring(prevPos, endPos);
		    			this.generatePatch(endPos, endPos, patch, "");
		    		}
					this.generatePatch(fixedCodeStr1);
				}
			} else {
				// TODO: other data types.
			}
		} else if (Checker.isCatchClause(suspCodeTree.getType())) {
			/*
			 * Fix pattern for REC_CATCH_EXCEPTION violations.
			 * 
			 * -  } catch (Exception ex) {
			 * +  } catch (RunTimeException ex) {
			 */
			String classPath = ContextReader.readClassPath(suspCodeTree);
			if (classPath == null || classPath.isEmpty()) return;
			List<String> exceptions = new ArrayList<>();
			List<String> importedDependencies = dic.getImportedDependencies().get(classPath);
			if (importedDependencies != null) {
				for (String importedDependency : importedDependencies) {
					if (importedDependency.endsWith("Execption")) {
						exceptions.add(importedDependency.substring(importedDependency.lastIndexOf(".") + 1));
					}
				}
			}
			
			ITree expDecl = suspCodeTree.getChild(0);
			ITree expTypeTree = null;
			List<ITree> children = expDecl.getChildren();
			for (ITree child : children) {
				if (!Checker.isModifier(child.getType())) {
					String expType = child.getLabel();
					if ("Exception".equals(expType)) {
						expTypeTree = child;
					}
					break;
				}
			}
			
			if (expTypeTree != null) {
				int startPos = expTypeTree.getPos();
				int endPos = startPos + expTypeTree.getLength();
				String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
				String codePart2 = this.getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
				this.generatePatch(codePart1 + "RunTimeException" + codePart2);
				
				for (String exception : exceptions) {
					this.generatePatch(codePart1 + exception + codePart2);
				}
			}
			
			return;
		}
		
		// PAR Caster Mutator
		// TODO How to select the data type, or what kinds of buggy types are targeted?
//		ITree suspStmtTree = this.getSuspiciousCodeTree();
//		Map<ITree, String> castExps = new ClassCastChecker().identifyCastExpressions(suspStmtTree);
//		if (castExps.isEmpty())
//			return;
//
//		for (Map.Entry<ITree, String> entity : castExps.entrySet()) {
//			// Generate Patches with CastExpression.
//			ITree castExp = entity.getKey();
//			ITree castingType = castExp.getChild(0);
//			int castTypeStartPos = castingType.getPos();
//			int castTypeEndPos = castTypeStartPos + castingType.getLength();
//			String castTypeStr = ContextReader
//					.readType(this.getSubSuspiciouCodeStr(castTypeStartPos, castTypeEndPos));
//			String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, castTypeStartPos);
//			String codePart2 = this.getSubSuspiciouCodeStr(castTypeEndPos, suspCodeEndPos);
//
//			if ("char".equals(castTypeStr) || "short".equals(castTypeStr) || "byte".equals(castTypeStr)
//					|| "int".equals(castTypeStr) || "long".equals(castTypeStr) || "float".equals(castTypeStr) || "double".equals(castTypeStr)) {
//				continue;
//			} else if ("Charater".equals(castTypeStr) || "Short".equals(castTypeStr) || "Byte".equals(castTypeStr)
//					|| "Integer".equals(castTypeStr) || "Long".equals(castTypeStr) || "Float".equals(castTypeStr) || "Double".equals(castTypeStr)) {
//				continue;
//			} else {
//				String classPath = ContextReader.readClassPath(suspCodeTree);
//				if (classPath == null || classPath.isEmpty()) return;
//				List<String> importedDependencies = dic.getImportedDependencies().get(classPath);
//				if (importedDependencies != null) {
//					for (String importedDependency : importedDependencies) {
//						String typeStr = importedDependency.substring(importedDependency.lastIndexOf(".") + 1);
//						if (typeStr.equals(castTypeStr))
//							continue;
//						generatePatch(codePart1 + typeStr + codePart2);
//					}
//				}
//			}
//		}

	}
	
	private void identifySameTypes(ITree stmt, String oldType, List<String> variables, List<ITree> peerStmts, List<Integer> posList) {
		if (Checker.isVariableDeclarationStatement(stmt.getType())) {
			ITree dataType = null;
			String variable = null;
			List<ITree> children = stmt.getChildren();
			for (ITree child : children) {
				int type = child.getType();
				if (Checker.isModifier(type)) continue;
				if (Checker.isVariableDeclarationFragment(type)) {//VariableDeclarationFragment
					variable = child.getChildren().get(0).getLabel();
					break;
				} else {
					dataType = child;
				}
			}
			if (dataType != null && dataType.getLabel().equals(oldType)) {
				if (isCorelatedStmt(stmt, variables, 60, peerStmts, posList, oldType)) {
					posList.add(dataType.getPos());
					variables.add(variable);
				}
			}
		} else if (Checker.withBlockStatement(stmt.getType())) {
			List<ITree> children = stmt.getChildren();
			for (int index = 0, size = children.size(); index < size; index ++) {
				ITree child = children.get(index);
				if (Checker.isStatement(child.getType())) {
					identifySameTypes(child, oldType, variables, (index == size - 1 ? null : children.subList(index + 1, size)), posList);
				}
			}
		}
	}
	
	private boolean isCorelatedStmt(ITree stmt, List<String> variables, int stmtType, List<ITree> peerStmts, List<Integer> posList, String oldType) {
		List<ITree> children = stmt.getChildren();
		boolean isCorelatedStmt = false;
		for (int index = 0, size = children.size(); index < size; index ++) {
			ITree child = children.get(index);
			// variables in stmt are int variable list.
			int type = child.getType();
			if (Checker.isSimpleName(type)) {
				String variable = child.getLabel();
				if (variables.contains(variable)) {
					isCorelatedStmt = true;
				} else if (Checker.isVariableDeclarationStatement(stmtType)) {// VariableDeclarationStatement
					variables.add(variable);
				}
			} else if (Checker.isComplexExpression(type)) {
				isCorelatedStmt = isCorelatedStmt(child, variables, stmtType, null, posList, oldType);
				if (isCorelatedStmt) return isCorelatedStmt;
			} else if (Checker.isStatement(type)) {
				identifySameTypes(child, oldType, variables, (index == size - 1 ? null : children.subList(index + 1, size)), posList);
			}
		}
		if (peerStmts != null) {
			for (ITree peerStmt : peerStmts) {
				isCorelatedStmt = isCorelatedStmt(peerStmt, variables, stmtType, null, posList, oldType);
				if (isCorelatedStmt) return isCorelatedStmt;
			}
		}
		return isCorelatedStmt;
	}

}
