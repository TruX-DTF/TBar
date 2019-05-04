package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;

public class MethodInvocationMutator extends AlterMethodInvocation {

	/*
	 * ELIXIR	T7_ChangeMethodInvocation => TODO: What is the first schema of changing method invocations ????
	 * SimFix	  RepMI_MI
	 * SketchFix  OverloadingTransform
	 * Fix pattern for DM_INVALID_MIN_MAX violations.
	 * 
	 * 1. Replace at least one parameter.
	 * PAR	  ParameterReplacer
	 * SOFix  ArgChanger
	 * SOFix  ArgMover
	 * 
	 * 2. Add at least one new parameter.
	 * PAR	  ParameterAdder
	 * SOFix  ArgAdder
	 * 
	 * 3. Delete at least one parameter.
	 * PAR	  ParameterRemover
	 * SOFix  ArgRemover
	 * 
	 * 4. Replace the method name with another one.
	 * PAR	  MethodReplacer
	 * SOFix  InvoReplacer
	 */
	
	@Override
	public void generatePatches() {
		if (Checker.isThrowStatement(this.getSuspiciousCodeTree().getType())) return;
		List<MethodInvocationExpression> suspMethodInvocations = this.identifySuspiciousMethodInvocations();

		for (MethodInvocationExpression suspMI : suspMethodInvocations) {
			ITree methodInvocationNode = suspMI.getCodeAst();
			
			/*
			 * Fix pattern for DM_INVALID_MIN_MAX violations.
			 * -   Math.max(Math.min(exp1, numberLiteral), exp2);
			 * +   Math.min(Math.max(exp1, numberLiteral), exp2);
			 */
			DMInvalidMinMax(methodInvocationNode);
			
			// Mutate method name.
			Map<String, List<String>> couldBeReplacedMethods = suspMI.getCouldBeReplacedMethods();
			if (couldBeReplacedMethods != null && !couldBeReplacedMethods.isEmpty()) {
				String methodName = suspMI.getMethodName();
				int startPos = methodInvocationNode.getPos();
				int endPos = startPos + methodName.length();
				String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
				String codePart2 = this.getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
				
				List<String> triedMethodNames = new ArrayList<>();
				for (Map.Entry<String, List<String>> entry : couldBeReplacedMethods.entrySet()) {
					List<String> possibleNames = entry.getValue();
					for (String possibleName : possibleNames) {
						if (triedMethodNames.contains(possibleName)) continue;
						triedMethodNames.add(possibleName);
						String fixedCodeStr1 = codePart1 + possibleName + codePart2;
						this.generatePatch(fixedCodeStr1);
					}
				}
			}
			
			
			
			// Add parameter
			List<String> returnTypes = suspMI.getPossibleReturnTypes();
			List<List<String>> parameterTypesList = suspMI.getParameterTypes();
			ITree methodNameNode = suspMI.getCodeAst();
			List<ITree> paraAsts = methodNameNode.getChildren();
			int paraNum = paraAsts.size();
			boolean isMutated = false;
			List<ITree> differentParaMethods = suspMI.getDifferentParaMethods();
			if (differentParaMethods != null && !differentParaMethods.isEmpty()) {
				List<ITree> parameters = new ArrayList<>();
				List<String> paraStrList = new ArrayList<>();
				if (Checker.isClassInstanceCreation(methodNameNode.getType())) {
					boolean isParameter = false;
					for (ITree paraAst : paraAsts) {
						if (isParameter) {
							parameters.add(paraAst);
							paraStrList.add(paraAst.getLabel());
						} else if (Checker.isSimpleType(paraAst.getType())) {
							isParameter = true;
						}
					}
				} else {
					for (ITree paraAst : paraAsts) {
						parameters.add(paraAst);
						paraStrList.add(paraAst.getLabel());
					}
				}
				for (ITree method : differentParaMethods) {
					String label = method.getLabel();
					int indexOfMethodName = label.indexOf("MethodName:");
					int indexOrPara = label.indexOf("@@Argus:");
					
					// Match parameter data types.
					String paraStr = label.substring(indexOrPara + 8);
					if (paraStr.startsWith("null")) {
						continue;
					} else {
						int indexExp = paraStr.indexOf("@@Exp:");
						if (indexExp > 0) paraStr = paraStr.substring(0, indexExp);
					}
					
					// Read return type.
					String returnType = label.substring(label.indexOf("@@") + 2, indexOfMethodName - 2);
					int index = returnType.indexOf("@@tp:");
					if (index > 0) returnType = returnType.substring(0, index - 2);
					returnType = ContextReader.readType(returnType);
					
					index = returnTypes.indexOf(returnType);
					if (index >= 0) {
						List<String> paraList = ContextReader.parseMethodParameterTypes(paraStr, "\\+");
						int paraListSize = paraList.size();
						List<String> buggyParaList = parameterTypesList.get(index);
						int remainParaNum = paraListSize - paraNum;
						if (remainParaNum <= 0) continue;
						if (remainParaNum != 1) continue;// FIXME: other conditions.
//						int lcsValue = paraListSize + paraNum - 2 * lcs(paraList, buggyParaList);
//						if (lcsValue != remainParaNum) continue;
						for (int i = 0; i < paraListSize; i ++) {
							List<String> subParaList = new ArrayList<>();
							subParaList.addAll(paraList.subList(0, i));
							subParaList.addAll(paraList.subList(i + 1, paraListSize));
							if (sameParaList(subParaList, buggyParaList)) {
								String paraType = paraList.get(i);
								List<String> varNames = allVarNamesMap.get(paraType);
								
								if (varNames != null && !varNames.isEmpty()) {
									String codePart1, codePart2, methodName;
									if (i == paraNum) {
										if (i == 0) {
											methodName = methodNameNode.getLabel().substring(11);
											methodName = methodName.substring(0, methodName.indexOf(":"));
											int subEndPos1 = methodNameNode.getPos() + methodName.length();
											int subEndPos2 = methodNameNode.getPos() + methodNameNode.getLength();
											codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, subEndPos1) + "(";
											codePart2 = ")" + this.getSubSuspiciouCodeStr(subEndPos2, suspCodeEndPos);
										} else {
											ITree paraTree = parameters.get(i - 1);
											int subStartPos = paraTree.getPos();
											int subEndPos = subStartPos + paraTree.getLength();
											codePart1 = this.getSubSuspiciouCodeStr(this.suspCodeStartPos, subEndPos) + ", ";
											codePart2 = this.getSubSuspiciouCodeStr(subEndPos, this.suspCodeEndPos);
										}
									} else {
										ITree paraTree = parameters.get(i);
										int subStartPos = paraTree.getPos();
										codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, subStartPos);
										codePart2 = ", " + this.getSubSuspiciouCodeStr(subStartPos, suspCodeEndPos);
									}
									for (String varName : varNames) {
										if (paraStrList.contains(varName)) continue;
										if (Character.isUpperCase(varName.charAt(0))) continue;
										String fixedCodeStr1 = codePart1 + varName + codePart2;
										this.generatePatch(fixedCodeStr1);
									}
									isMutated = true;
								}
							}
						}
						
//						// some default values. FIXME: it could be removed.
//						for (int i = 0; i < paraListSize; i++) {
//							List<String> subParaList = new ArrayList<>();
//							subParaList.addAll(paraList.subList(0, i));
//							subParaList.addAll(paraList.subList(i + 1, paraListSize));
//							if (sameParaList(subParaList, buggyParaList)) {
//								String paraType = paraList.get(i);
//								String codePart1, codePart2, methodName;
//								if (i == paraNum) {
//									if (i == 0) {
//										methodName = methodNameNode.getLabel().substring(11);
//										methodName = methodName.substring(0, methodName.indexOf(":"));
//										int subEndPos1 = methodNameNode.getPos() + methodName.length();
//										int subEndPos2 = methodNameNode.getPos() + methodNameNode.getLength();
//										codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, subEndPos1) + "(";
//										codePart2 = ")" + this.getSubSuspiciouCodeStr(subEndPos2, suspCodeEndPos);
//									} else {
//										ITree paraTree = paraAsts.get(i - 1);
//										int subStartPos = paraTree.getPos();
//										int subEndPos = subStartPos + paraTree.getLength();
//										codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, subEndPos) + ", ";
//										codePart2 = this.getSubSuspiciouCodeStr(subEndPos, suspCodeEndPos);
//									}
//								} else {
//									ITree paraTree = paraAsts.get(i);
//									int subStartPos = paraTree.getPos();
//									codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, subStartPos);
//									codePart2 = ", " + this.getSubSuspiciouCodeStr(subStartPos, suspCodeEndPos);
//								}
//
//								String fixedCodeStr1 = codePart1;
//								if (paraType.equals("char")) {
//									fixedCodeStr1 += "' '";
//								} else if (paraType.equals("Character")) {
//									fixedCodeStr1 += "' '" + codePart2;
//									this.generatePatch(fixedCodeStr1);
//									fixedCodeStr1 = codePart1 + "null";
//								} else if (paraType.equals("byte") || paraType.equals("short") || paraType.equals("int")
//										|| paraType.equals("long") || paraType.equals("double")
//										|| paraType.equals("float")) {
//									fixedCodeStr1 += "0" + codePart2;
//									this.generatePatch(fixedCodeStr1);
//									fixedCodeStr1 = codePart1 + "1";
//								} else if (paraType.equals("Byte") || paraType.equals("Short") || paraType.equals("Integer")
//										|| paraType.equals("Long") || paraType.equals("Double")
//										|| paraType.equals("Float")) {
//									fixedCodeStr1 += "0" + codePart2;
//									this.generatePatch(fixedCodeStr1);
//									fixedCodeStr1 = codePart1 + "1" + codePart2;
//									this.generatePatch(fixedCodeStr1);
//									fixedCodeStr1 = codePart1 + "null";
//								} else if (paraType.equals("String")) {
//									fixedCodeStr1 += "\"\"" + codePart2;
//									this.generatePatch(fixedCodeStr1);
//									fixedCodeStr1 = codePart1 + "null";
//								} else if (paraType.equalsIgnoreCase("boolean")) {
//									fixedCodeStr1 += "true" + codePart2;
//									this.generatePatch(fixedCodeStr1);
//									fixedCodeStr1 = codePart1 + "false";
//								} else {
//									fixedCodeStr1 += "null";
//								}
//								fixedCodeStr1 += codePart2;
//								this.generatePatch(fixedCodeStr1);
//								isMutated = true;
//							}
//						}
					}
				}
			}
			if (!isMutated && Checker.isClassInstanceCreation(methodNameNode.getType())) {
				List<ITree> parameters = new ArrayList<>();
				List<String> paraList = new ArrayList<>();
				if (Checker.isClassInstanceCreation(methodNameNode.getType())) {
					boolean isParameter = false;
					for (ITree paraAst : paraAsts) {
						if (isParameter) {
							parameters.add(paraAst);
							paraList.add(paraAst.getLabel());
						} else if (Checker.isSimpleType(paraAst.getType())) {
							isParameter = true;
						}
					}
				} else {
					for (ITree paraAst : paraAsts) {
						parameters.add(paraAst);
						paraList.add(paraAst.getLabel());
					}
				}
				
				int parametersSize = parameters.size() - 1;
				for (int i = 0; i <= parametersSize; i ++) {
					ITree para = parameters.get(i);
					int paraStartPos = para.getPos();
					String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, paraStartPos);
					String codePart2 = this.getSubSuspiciouCodeStr(paraStartPos, suspCodeEndPos);
					for (String var : this.allVarNamesList) {
						if (paraList.contains(var)) continue;
						if (Character.isUpperCase(var.charAt(0))) continue;
						this.generatePatch(codePart1 + var + ", " + codePart2);
					}
					
					if (i == parametersSize) {
						paraStartPos = paraStartPos + para.getLength();
						codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, paraStartPos);
						codePart2 = this.getSubSuspiciouCodeStr(paraStartPos, suspCodeEndPos);
						for (String var : this.allVarNamesList) {
							if (paraList.contains(var)) continue;
							if (Character.isUpperCase(var.charAt(0))) continue;
							this.generatePatch(codePart1 + ", " + var + codePart2);
						}
					}
				}
			}
			
			
			// Remove parameter: only remove one parameter.
			int startPos, endPos;
			if (!paraAsts.isEmpty()) {
				int size = paraAsts.size();
				if (Checker.isClassInstanceCreation(methodNameNode.getType())) {
					int index = 0;
					if (Checker.isNewKeyword(paraAsts.get(index).getType())) {
						index = 2;
					} else index = 3;
					int toIndex = size;
					if (Checker.isAnonymousClassDeclaration(paraAsts.get(toIndex - 1).getType())) {
						toIndex = size - 1;
					}
					paraAsts = paraAsts.subList(index, toIndex);
					if (paraAsts.isEmpty()) continue;
					size = paraAsts.size();
				}
				if (size == 1) {
					ITree paraAst = paraAsts.get(0);
					startPos = paraAst.getPos();
					endPos = startPos + paraAst.getLength();
					String fixedCodeStr1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos) + 
							this.getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
					this.generatePatch(fixedCodeStr1);
				} else {
					size = size - 1;
					for (int index = 0; index < size; index ++) {
						startPos = paraAsts.get(index).getPos(); 
						endPos = paraAsts.get(index + 1).getPos();
						String fixedCodeStr1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos) + 
								this.getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
						this.generatePatch(fixedCodeStr1);
					}
					ITree paraAst = paraAsts.get(size - 1);
					startPos = paraAst.getPos() + paraAst.getLength();
					paraAst = paraAsts.get(size);
					endPos = paraAst.getPos() + paraAst.getLength();
					String fixedCodeStr1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos) + 
							this.getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
					this.generatePatch(fixedCodeStr1);
				}
			}
		}
		
		
		// update parameter.
		Map<ITree, Integer> suspMethodInvocationsMap = this.suspMethodInvocations;

		for (Map.Entry<ITree, Integer> entry : suspMethodInvocationsMap.entrySet()) {
			ITree methodInvocationNode = entry.getKey();
			int methodInvType = entry.getValue();
			List<ITree> paraList;// = new ArrayList<>();
			if (methodInvType == 1) {
				int type = methodInvocationNode.getType();
				if (Checker.isClassInstanceCreation(type)) {
					List<ITree> children = methodInvocationNode.getChildren();
					int index = 2;
					if (!Checker.isNewKeyword(children.get(0).getType())) {
						index = 3;
					}
					if (children.size() <= index) continue; // no parameter.
					int toIndex = children.size();
					if (Checker.isAnonymousClassDeclaration(children.get(toIndex - 1).getType())) {
						paraList = children.subList(index, toIndex - 1);
					} else {
						paraList = children.subList(index, toIndex);
					}
				} else if (Checker.isConstructorInvocation(type) || Checker.isSuperConstructorInvocation(type)) {
					paraList = methodInvocationNode.getChildren();
				} else continue; // no parameters.
			} else if (methodInvType == 3) 
				paraList = methodInvocationNode.getChildren();
			else {//if (methodInvType == 2)
				int suspCodeNodeType = this.getSuspiciousCodeTree().getType();
				if (Checker.isConstructorInvocation(suspCodeNodeType) || Checker.isSuperConstructorInvocation(suspCodeNodeType)) {
					paraList = methodInvocationNode.getChildren();
				} else {
//					List<ITree> children = methodInvocationNode.getChildren();
					paraList = methodInvocationNode.getChildren();//children.get(children.size() - 1).getChildren();
				}
			}
			int paraNum = paraList.size();
			if (paraNum == 0 || paraNum == 1) continue;
			
			List<List<String>> paraVars = new ArrayList<>();
			for (ITree paraTree : paraList) {
				List<String> varNames = new ArrayList<>();
				int paraTreeType = paraTree.getType();
				if (Checker.isSimpleName(paraTreeType)) {
					String varName = ContextReader.readVariableName(paraTree);
					String dataType = varTypesMap.get(varName);
					
					if (dataType == null) {
						varName = "this." + varName;
						dataType = this.varTypesMap.get(varName);
					}
					if (dataType != null) {
						if (isNumberType(dataType)) {
							// FIXME: how about number literal values?
							List<String> varL = allVarNamesMap.get("int");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("Integer");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("long");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("Long");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("double");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("Double");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("float");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("Float");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("short");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("Short");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("byte");
							if (varL != null) varNames.addAll(varL);
							varL = allVarNamesMap.get("Byte");
							if (varL != null) varNames.addAll(varL);
						} else {
							varNames = allVarNamesMap.get(dataType);
						}
						varNames.remove(varName);
					} else {
						varNames = allVarNamesMap.get("Object");
					}
				} else if (Checker.isQualifiedName(paraTreeType)) {
					String varName = paraTree.getLabel();
					String dataType = varTypesMap.get(varName);
					if (dataType == null) varNames = allVarNamesMap.get("Object");
					else {
						varNames = allVarNamesMap.get(dataType);
						varNames.remove(varName);
					}
				} else { // NullLiteral and others.
					paraVars = null;
					break;
				}
				paraVars.add(varNames);
			}
			if (paraVars == null) continue;
			
			String codePart1, codePart2;
			// Replace two parameters.
			for (int index1 = 0, size = paraVars.size() - 1; index1 < size; index1 ++) {
				List<String> vars1 = paraVars.get(index1);
				if (vars1 == null) continue;
				ITree paraTree1 = paraList.get(index1);
				vars1.remove(paraTree1.getLabel());
				if (vars1.isEmpty()) continue;
				
				int paraStartPos1 = paraTree1.getPos();
				int paraEndPos1 = paraStartPos1 + paraTree1.getLength();
				codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, paraStartPos1);
				
				for (int index2 = index1 + 1; index2 <= size; index2 ++) {
					List<String> vars2 = paraVars.get(index2);
					if (vars2 == null) continue;
					ITree paraTree2 = paraList.get(index2);
					vars2.remove(paraTree2.getLabel());
					if (vars2.isEmpty()) continue;
					
					int paraStartPos2 = paraTree2.getPos();
					int paraEndPos2 = paraStartPos2 + paraTree2.getLength();
					codePart2 = this.getSubSuspiciouCodeStr(paraEndPos1, paraStartPos2);
					String codePart3= this.getSubSuspiciouCodeStr(paraEndPos2, suspCodeEndPos);
					
					for (String var1 : vars1) {
						if (Character.isUpperCase(var1.charAt(0))) continue;
						for (String var2 : vars2) {
							if (Character.isUpperCase(var2.charAt(0))) continue;
							String fixedCodeStr1 = codePart1 + var1 + codePart2 + var2 + codePart3;
							generatePatch(fixedCodeStr1);
						}
					}
				}
			}
			
			if (paraNum > 2) {
				// Replace three parameters.
				for (int index1 = 0, size = paraVars.size() - 1; index1 < size; index1 ++) {
					List<String> vars1 = paraVars.get(index1);
					if (vars1 == null) continue;
					ITree paraTree1 = paraList.get(index1);
					vars1.remove(paraTree1.getLabel());
					if (vars1.isEmpty()) continue;
					int paraStartPos1 = paraTree1.getPos();
					int paraEndPos1 = paraStartPos1 + paraTree1.getLength();
					codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, paraStartPos1);
					
					for (int index2 = index1 + 1; index2 <= size; index2 ++) {
						List<String> vars2 = paraVars.get(index2);
						if (vars2 == null) continue;
						ITree paraTree2 = paraList.get(index2);
						vars2.remove(paraTree2.getLabel());
						if (vars2.isEmpty()) continue;
						int paraStartPos2 = paraTree2.getPos();
						int paraEndPos2 = paraStartPos2 + paraTree2.getLength();
						codePart2 = this.getSubSuspiciouCodeStr(paraEndPos1, paraStartPos2);
						
						for (int index3 = index2 + 1; index3 <= size; index3 ++) {
							List<String> vars3 = paraVars.get(index3);
							if (vars3 == null) continue;
							ITree paraTree3 = paraList.get(index3);
							vars3.remove(paraTree3.getLabel());
							if (vars3.isEmpty()) continue;
							int paraStartPos3 = paraTree3.getPos();
							int paraEndPos3 = paraStartPos3 + paraTree3.getLength();
							String codePart3 = this.getSubSuspiciouCodeStr(paraEndPos2, paraStartPos3);
							String codePart4 = this.getSubSuspiciouCodeStr(paraEndPos3, suspCodeEndPos);
							
							for (String var1 : vars1) {
								if (Character.isUpperCase(var1.charAt(0))) continue;
								for (String var2 : vars2) {
									if (var2.equals(var1)) continue;
									if (Character.isUpperCase(var2.charAt(0))) continue;
									for (String var3 : vars3) {
										if (var3.equals(var1) || var3.equals(var2)) continue;
										if (Character.isUpperCase(var3.charAt(0))) continue;
										String fixedCodeStr1 = codePart1 + var1 + codePart2 + var2 + codePart3 + var3 + codePart4;
										generatePatch(fixedCodeStr1);
									}
								}
							}
						}
					}
					// TODO replace more parameters.
				}
			}
		}
	}
	
	private void DMInvalidMinMax(ITree methodInvocationNode) {
		/*
		 * -   Math.max(Math.min(exp1, numberLiteral), exp2);
	     * +   Math.min(Math.max(exp1, numberLiteral), exp2);
		 */
		String label = methodInvocationNode.getLabel();
		Boolean isMathMax = null;
		if (label.startsWith("Math.max(")) {
			isMathMax = true;
		} else if (label.startsWith("Math.min(")) {
			isMathMax = false;
		}
		if (isMathMax == null) return;
		
		List<ITree> subChildren = methodInvocationNode.getChildren();
		if (subChildren.size() != 2) return;
		ITree methodInvoc = subChildren.get(1);
		if (methodInvoc.getChildren().size() != 2) return;
		
		int pos1 = methodInvocationNode.getPos();
		int pos2 = 0;
		ITree para = null;
		String methodName1;
		String methodName2;
		if (isMathMax) {
			methodName1 = "Math.max(";
			methodName2 = "Math.min(";
			if (methodInvoc.getChild(0).getLabel().startsWith("Math.min(")) {
				para = methodInvoc.getChild(0);
				pos2 = para.getPos();
			} else if (methodInvoc.getChild(1).getLabel().startsWith("Math.min(")) {
				para = methodInvoc.getChild(1);
				pos2 = para.getPos();
			}
		} else {
			methodName1 = "Math.min(";
			methodName2 = "Math.max(";
			if (methodInvoc.getChild(0).getLabel().startsWith("Math.max(")) {
				para = methodInvoc.getChild(0);
				pos2 = para.getPos();
			} else if (methodInvoc.getChild(1).getLabel().startsWith("Math.max(")) {
				para = methodInvoc.getChild(1);
				pos2 = para.getPos();
			}
		}
		if (para == null) return;
		int pos3 = methodInvocationNode.getChild(1).getChild(0).getPos();
		int pos4 = para.getChild(1).getChild(0).getPos();
		String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
		String codePart2 = this.getSubSuspiciouCodeStr(pos3, pos2);
		String codePart3 = this.getSubSuspiciouCodeStr(pos4, suspCodeEndPos);
		String fixedCodeStr1 = codePart1 + methodName2 + codePart2 + methodName1 + codePart3;
		this.generatePatch(fixedCodeStr1);
	}
	
	private boolean sameParaList(List<String> subParaList, List<String> buggyParaList) {
		if (buggyParaList.size() != subParaList.size()) return false;
		for (int i = 0, size = subParaList.size(); i < size; i ++) {
			if (!subParaList.get(i).equals(buggyParaList.get(i))) return false;
		}
		return true;
	}

	private boolean isNumberType(String dataType) {
		if ("int".equals(dataType)) return true;
		if ("Integer".equals(dataType)) return true;
		if ("long".equalsIgnoreCase(dataType)) return true;
		if ("float".equalsIgnoreCase(dataType)) return true;
		if ("double".equalsIgnoreCase(dataType)) return true;
		if ("short".equalsIgnoreCase(dataType)) return true;
		if ("byte".equalsIgnoreCase(dataType)) return true;
		return false;
	}
	
}
