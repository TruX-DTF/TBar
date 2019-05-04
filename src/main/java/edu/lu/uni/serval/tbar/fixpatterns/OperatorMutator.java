package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

public class OperatorMutator extends FixTemplate {
	/*
	 * 1. Arithmetic operators: +, -, *, /, % ==> priority change. SOFix	BinaryOpInversion
	 * 
	 * 2. Assignment Operators:  =, +=, -=, *=, /=, %=, &=, ^=, |=, <<=, >>=, >>>=.
	 * 
	 * 3. Equality and Relational Operators: ==, !=, >, >=, <, <=.
	 * ELIXIR	T5_InfixBooleanOperator
	 * FB	NPAlwaysNull
	 * FB	UCUselessCondition_
	 * PAR	ExpressionReplacer
	 * SimFix	RepInfixOp
	 * SketchFix	OperatorTransform
	 * SOFix	BinaryOpReplacer
	 * 4. Conditional Operators: &&, ||, ?:.
	 * ELIXIR	T5_InfixBooleanOperator
	 * 
	 * 5. Type Comparison Operator: instanceof.
	 * FB	BCVacuousInstanceof
	 * 
	 * 6. Bitwise and Bit Shift Operators: ~, <<, >>, >>>, &, ^, |.
	 * FB	BitIor
	 * FB	BitSignedCheck
	 * FB	BShiftWrongAddPriority
	 * FB	NSDangerousNonShortCircuit
	 * 
	 * 7. Unary Operators: ++expr, --expr, expr++, expr--, +expr, -expr, ~ !.
	 * No fix pattern.
	 * 
	 */
	private List<Pair<ITree, String>> arithmeticOpExps = new ArrayList<>();
	private List<Pair<ITree, String>> relationalOpExps = new ArrayList<>();
	private List<Pair<ITree, String>> conditionalOpExps = new ArrayList<>();
	private List<Pair<ITree, Integer>> arithmeticInfixExps = new ArrayList<>();
	private List<Pair<ITree, String>> bitIorExps = new ArrayList<>();
	private List<Pair<ITree, String>> bitAssignExps = new ArrayList<>();
	
	private static List<String> arithmeticOperators = new ArrayList<>();
	private static List<String> relationalOperators = new ArrayList<>();
	private static List<String> conditionalOperators = new ArrayList<>();
	
	static {
		arithmeticOperators.add("+");
		arithmeticOperators.add("-");
		arithmeticOperators.add("*");
		arithmeticOperators.add("/");
		arithmeticOperators.add("%");
		
		relationalOperators.add("==");
		relationalOperators.add("!=");
		relationalOperators.add("<");
		relationalOperators.add("<=");
		relationalOperators.add(">");
		relationalOperators.add(">=");
		
		conditionalOperators.add("&&");
		conditionalOperators.add("||");
//		conditionalOperators.add("!");
	}
	
	/*
	 * 1: Arithmetic operators.
	 * 2: Assignment Operators.
	 * 3: Equality and Relational Operators.
	 * 4: Conditional Operators.
	 * 5: Type Comparison Operator: instanceof.
	 * 6: Bitwise and Bit Shift Operators.
	 * 7: Unary Operators.
	 * 8: DLSDeadLocalStoreInReturn
	 */
	private int operatorType = 0;
	
	public OperatorMutator(int operatorType) {
		this.operatorType = operatorType;
	}

	@Override
	public void generatePatches() {
		ITree suspCodeTree = getSuspiciousCodeTree();
		if (operatorType == 2) {
			new AssignmentOperatorMuataor().generatePatches(suspCodeTree);
		} else if (operatorType == 5) {
//			new InstanceofMutator().generatePatches(suspCodeTree);
		} else {
			identifySuspiciousOperators(suspCodeTree);
			if (operatorType == 0 || operatorType == 1 || operatorType == 3 || operatorType == 6 || operatorType == 8) {
				new ConditionalExpressionOperatorMutator().generatePatches();
				new OperatorPriorityMutator().generatePatches();
				new BitIorOperatorMutator().generatePatches();
				new ArithmeticOperatorMutator().generatePatches();
				new DLSDeadLocalStoreInReturn().generatePatches(suspCodeTree);
			} else if (operatorType == 4) {
				// !, && or ||
				for (Pair<ITree, String> relationalOpExp : conditionalOpExps) {
					ITree opExpTree = relationalOpExp.getFirst();
					String op = relationalOpExp.getSecond();
//					if ("!".equals(op)) {
//						int pos1 = opExpTree.getPos();
//						int pos2 = opExpTree.getChild(1).getPos();
//						String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
//						String codePart2 = this.getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
//						this.generatePatch(codePart1 + codePart2);
//					} else {
						int pos1 = opExpTree.getChild(0).getPos() + opExpTree.getChild(0).getLength();
						int pos2 = opExpTree.getChild(2).getPos();
						String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
						String codePart2 = this.getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
						if ("&&".equals(op)) op = " || ";
						else op = " && ";
						this.generatePatch(codePart1 + op + codePart2);
//					}
				}
			}
		}
	}
	
	private void identifySuspiciousOperators(ITree suspCodeTree) {
		List<ITree> children = suspCodeTree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isComplexExpression(childType)) {
				identifySuspiciousOperators(child);
				if (Checker.isInfixExpression(childType)) {
					String op = child.getChild(1).getLabel();
					if (operatorType == 4) {
						if (conditionalOperators.contains(op)) conditionalOpExps.add(new Pair<ITree, String>(child, op));
					} else {
						if (relationalOperators.contains(op)) {
							relationalOpExps.add(new Pair<ITree, String>(child, op));
						} else if (arithmeticOperators.contains(op)) {
							arithmeticOpExps.add(new Pair<ITree, String>(child, op));
						}
						ITree leftHandExp = child.getChild(0);
						ITree rightHandExp = child.getChild(2);
						if (Checker.isParenthesizedExpression(leftHandExp.getType()) && !Checker.isInfixExpression(rightHandExp.getType())) {
							if (Checker.isInfixExpression(leftHandExp.getChild(0).getType())) {
								String subOp = leftHandExp.getChild(0).getChild(1).getLabel();
								if (arePotentialBuggyOperators(op, subOp)) {
									arithmeticInfixExps.add(new Pair<ITree, Integer>(child, 0));
								}
							}
						} else if (Checker.isParenthesizedExpression(rightHandExp.getType()) && !Checker.isInfixExpression(leftHandExp.getType())) {
							if (Checker.isInfixExpression(rightHandExp.getChild(0).getType())) {
								String subOp = rightHandExp.getChild(0).getChild(1).getLabel();
								if (arePotentialBuggyOperators(op, subOp)) {
									arithmeticInfixExps.add(new Pair<ITree, Integer>(child, 2));
								}
							}
						} else if (Checker.isInfixExpression(leftHandExp.getType())) {
							String subOp = leftHandExp.getChild(1).getLabel();
							if (arePotentialBuggyOperators(op, subOp)) {
								arithmeticInfixExps.add(new Pair<ITree, Integer>(child, 1));
							}
						} else if (Checker.isInfixExpression(rightHandExp.getType())) {
							String subOp = rightHandExp.getChild(1).getLabel();
							if (arePotentialBuggyOperators(op, subOp)) {
								arithmeticInfixExps.add(new Pair<ITree, Integer>(child, 3));
							}
						}
						
						if ("&".equals(op) || "|".equals(op)) bitIorExps.add(new Pair<ITree, String>(child, op));
						if ("&=".equals(op) || "|=".equals(op)) bitAssignExps.add(new Pair<ITree, String>(child, op));
					}
//				} else if (Checker.isPrefixExpression(childType)) {
//					String op = child.getChild(0).getLabel();
//					if ("!".equals(op) && condOp != null) conditionalOpExps.add(new Pair<ITree, String>(child, op));
				}
			} else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
				identifySuspiciousOperators(child);
			} else if (Checker.isStatement(childType)) break;
		}
	}
	
	private class DLSDeadLocalStoreInReturn {
		/*
		 * -	return v &=/|= exp ;
	     * +	return v &/| exp;
		 */
		protected void generatePatches(ITree suspCodeTree) {
			if (!Checker.isReturnStatement(suspCodeEndPos)) return;
			for (Pair<ITree, String> bitAssignExp : bitAssignExps) {
				ITree buggyExp = bitAssignExp.getFirst();
				String op = bitAssignExp.getSecond();
				int pos1 = buggyExp.getChild(0).getPos() + buggyExp.getChild(0).getLength();
				int pos2 = buggyExp.getChild(2).getPos();
				String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
				op = op.substring(0, 1);
				generatePatch(codePart1 + op + getSubSuspiciouCodeStr(pos2, suspCodeEndPos));
			}
		}
	}

	private boolean arePotentialBuggyOperators(String op1, String op2) {
		if (op1.equals(op2)) return false;
		if (("+".equals(op1) || "-".equals(op1)) && ("+".equals(op2) || "-".equals(op2))) return false;
		if (("&&".equals(op1) || "||".equals(op1)) && ("&&".equals(op2) || "||".equals(op2))) return true;
		if (("+".equals(op1) || "-".equals(op1) || "*".equals(op1) || "/".equals(op1) || "%".equals(op1) || ">>".equals(op1) || "<<".equals(op1))
				&& ("+".equals(op2) || "-".equals(op2) || "*".equals(op2) || "/".equals(op2) || "%".equals(op2) || ">>".equals(op2) || "<<".equals(op2))) return true;
		return false;
	}
	
	private class ConditionalExpressionOperatorMutator {
		/*
		 * a || b -> a && b;
		 */
		private boolean isConditionalOperator = false;
		private List<ITree> triedInfixExps = new ArrayList<>();
		
		public ConditionalExpressionOperatorMutator() {};
		@SuppressWarnings("unused")
		public ConditionalExpressionOperatorMutator(boolean isConditionalOperator) {
			this();
			this.isConditionalOperator = isConditionalOperator;
		}
		
		protected void generatePatches() {
			if (isConditionalOperator) {
				for (Pair<ITree, String> relationalOperatorPair : relationalOpExps) {
					ITree suspExpTree = relationalOperatorPair.getFirst();
					int startPos = suspExpTree.getChild(0).getPos() + suspExpTree.getChild(0).getLength();
					int endPos = suspExpTree.getChild(2).getPos();
					String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
					String codePart2 = getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
					String op = relationalOperatorPair.getSecond();
					if ("&&".equals(op)) {
						String fixedCodeStr1 = codePart1 + " || " + codePart2;
						generatePatch(fixedCodeStr1);
					} else if ("||".equals(op)) {
						String fixedCodeStr1 = codePart1 + " && " + codePart2;
						generatePatch(fixedCodeStr1);
					}
				}
			} else {
				for (Pair<ITree, String> relationalOperatorPair : relationalOpExps) {
					ITree suspExpTree = relationalOperatorPair.getFirst();
					int startPos = suspExpTree.getChild(0).getPos() + suspExpTree.getChild(0).getLength();
					int endPos = suspExpTree.getChild(2).getPos();
					String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
					String codePart2 = getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
					String op = relationalOperatorPair.getSecond();
					if (codePart2.startsWith("null") || codePart1.endsWith("null")) {
						String mutatedOp;
						if ("==".equals(op)) mutatedOp = " != ";
						else mutatedOp = " == ";
						String fixedCodeStr1 = codePart1 + mutatedOp + codePart2;
						generatePatch(fixedCodeStr1);
					} else {
						for (String relationalOperator : relationalOperators) {
							if (relationalOperator.equals(op)) continue;
							generatePatch(codePart1 + relationalOperator + codePart2);
						}
						furtherMutation(suspExpTree, op);
					}
				}
			}
		}
		
		private void furtherMutation(ITree suspExpTree, String buggyOp) {
			ITree parentTree = suspExpTree.getParent();
			int index = 0;
			while (true) {
				int parentTreeType = parentTree.getType();
				if (Checker.isStatement(parentTreeType)) return;
				if (Checker.isInfixExpression(parentTreeType)) break;
				if (!Checker.isParenthesizedExpression(parentTreeType)) return;
				parentTree = parentTree.getParent();
				index ++;
				if (index > 1) return;
			}
			
			String op = parentTree.getChild(1).getLabel();
			if (!"||".equals(op) && !"&&".equals(op)) return;
			// (a op b) ||/&& (c op d)
			// a op b ||/&& c op d
			if (triedInfixExps.contains(parentTree)) return;
			triedInfixExps.add(parentTree);
			
			ITree anotherSuspExpTree = null;
			if (index == 1) {
				anotherSuspExpTree = parentTree.getChild(0);
				if (anotherSuspExpTree.getChildren().isEmpty()) return;
				anotherSuspExpTree = anotherSuspExpTree.getChild(0);
				if (anotherSuspExpTree.equals(suspExpTree)) {
					anotherSuspExpTree = parentTree.getChild(2);
					if (anotherSuspExpTree.getChildren().isEmpty()) return;
					anotherSuspExpTree = anotherSuspExpTree.getChild(0);
				}
			} else {
				anotherSuspExpTree = parentTree.getChild(0);
				if (Checker.isParenthesizedExpression(anotherSuspExpTree.getType())) 
					anotherSuspExpTree = anotherSuspExpTree.getChild(0);
				if (anotherSuspExpTree.equals(suspExpTree)) {
					anotherSuspExpTree = parentTree.getChild(2);
					if (Checker.isParenthesizedExpression(anotherSuspExpTree.getType())) 
						anotherSuspExpTree = anotherSuspExpTree.getChild(0);
				}
			}
			if (Checker.isInfixExpression(anotherSuspExpTree.getType()) && buggyOp.equals(anotherSuspExpTree.getChild(1).getLabel())) {
				int startPos1 = suspExpTree.getPos();
				int startPos2 = anotherSuspExpTree.getPos();
				if (startPos1 > startPos2) {
					ITree _suspExpTree = suspExpTree;
					suspExpTree = anotherSuspExpTree;
					anotherSuspExpTree = _suspExpTree;
					int _startPos = startPos1;
					startPos1 = startPos2;
					startPos2 = _startPos;
				}
				int pos1 = suspExpTree.getChild(0).getPos() + suspExpTree.getChild(0).getLength();
				int pos2 = suspExpTree.getChild(2).getPos();
				int pos3 = anotherSuspExpTree.getChild(0).getPos() + anotherSuspExpTree.getChild(0).getLength();
				int pos4 = anotherSuspExpTree.getChild(2).getPos();
				
				String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
				String codePart2 = getSubSuspiciouCodeStr(pos2, pos3);
				String codePart3 = getSubSuspiciouCodeStr(pos4, suspCodeEndPos);
				for (String relationalOperator : relationalOperators) {
					if (relationalOperator.equals(buggyOp)) continue;
					generatePatch(codePart1 + " " + relationalOperator + " " + codePart2 + " " + relationalOperator + " " + codePart3);
				}
			}
		}
	}
	
	/**
	 * SOFix: BinaryOpInversion, change the priority of the original operators.
	 * FB : BShiftWrongAddPriority
	 * 
	 * @author kui.liu
	 */
	private class OperatorPriorityMutator {
		protected void generatePatches() {
			for (Pair<ITree, Integer> pair : arithmeticInfixExps) {
				ITree suspInfixExp = pair.getFirst();
				int index = pair.getSecond();
				if (index % 2 == 0) {
					ITree subExp = suspInfixExp.getChild(index);
					
					int startPos1 = suspInfixExp.getPos();
					int endPos1 = startPos1 + suspInfixExp.getLength();
					String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, startPos1);
					String codePart2 = getSubSuspiciouCodeStr(endPos1, suspCodeEndPos);
					
					if (index == 0) {
						ITree subInfixExp= subExp.getChild(0);
						int pos1 = subInfixExp.getChild(0).getPos();
						int pos2 = subInfixExp.getChild(2).getPos();
						String codePart3 = getSubSuspiciouCodeStr(pos1, pos2);
						int pos3 = pos2 + subInfixExp.getChild(2).getLength();
						String codePart4 = getSubSuspiciouCodeStr(pos2, pos3);
						int pos4 = subExp.getPos() + subExp.getLength();
						String codePart5 = getSubSuspiciouCodeStr(pos4, endPos1);
						generatePatch(codePart1 + codePart3 + "(" + codePart4 + codePart5 + ")" + codePart2);
					} else {
						String codePart3 = getSubSuspiciouCodeStr(startPos1, subExp.getPos());
						ITree subInfixExp= subExp.getChild(0);
						int pos1 = subInfixExp.getChild(0).getPos();
						int pos2 = pos1 + subInfixExp.getChild(0).getLength();
						String codePart4 = getSubSuspiciouCodeStr(pos1, pos2);
						int pos3 = subInfixExp.getChild(2).getPos() + subInfixExp.getChild(2).getLength();
						String codePart5 = getSubSuspiciouCodeStr(pos2, pos3);
						generatePatch(codePart1 + "(" + codePart3 + codePart4 + ")" + codePart5 + codePart2);
					}
				} else {
					index --;
					ITree infixExp = suspInfixExp.getChild(index);
					
					int startPos = suspInfixExp.getPos();
					int endPos = startPos + suspInfixExp.getLength();
					int pos;
					if (index == 0) { // a * b - c ==> a * (b - c)
						pos = infixExp.getChild(2).getPos();
					} else { // a - b * c ==> (a - b) * c
						pos = startPos;
						endPos = infixExp.getChild(0).getPos() + infixExp.getChild(0).getLength();
					}
					String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos);
					String codePart2 = getSubSuspiciouCodeStr(pos, endPos);
					String codePart3 = getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
					generatePatch(codePart1 + "(" + codePart2 + ")" + codePart3);
				}
			}
		}
	}
	
	/**
	 * Fix pattern for BC_VACUOUS_INSTANCEOF violations.
	 * - if ( exp instanceof String ) {
	 * + if ( exp != null) {
	 * 
	 * @author kui.liu
	 */
	@SuppressWarnings("unused")
	private class InstanceofMutator {
		protected void generatePatches(ITree suspCodeTree) {
			if (Checker.isIfStatement(suspCodeTree.getType())) {
				ITree exp = suspCodeTree.getChild(0);
				if (!Checker.isInstanceofExpression(exp.getType())) return;
				
				int index = exp.getPos() + exp.getLength();
				String nonBuggyCode = getSubSuspiciouCodeStr(index, suspCodeEndPos);
				ITree subExp = exp.getChild(0);
				int startPos = subExp.getPos();
				int endPos = startPos + subExp.getLength();
				String subExpCode = getSubSuspiciouCodeStr(startPos, endPos);
				generatePatch("if (" + subExpCode + " != null" + nonBuggyCode);
			}
		}
	}
	
	private class AssignmentOperatorMuataor {
		protected void generatePatches(ITree suspCodeTree) {
			if (!Checker.isExpressionStatement(suspCodeTree.getType())) return;
			ITree assignmentExp = suspCodeTree.getChild(0);
			if (!Checker.isAssignment(assignmentExp.getType())) return;
			ITree var = assignmentExp.getChild(0);
			ITree op = assignmentExp.getChild(1);
			if ("=".equals(op.getLabel())) {
				String varName = var.getLabel();
				String varType = ContextReader.readVariableType(suspCodeTree, varName);
				if (varType == null) return;
				if ("int".equals(varType) || "Integer".equals(varType) || "byte".equalsIgnoreCase(varType)
						 || "short".equalsIgnoreCase(varType) || "long".equalsIgnoreCase(varType)
						 || "float".equalsIgnoreCase(varType) || "double".equalsIgnoreCase(varType)) {
					ITree exp = assignmentExp.getChild(2);
					int pos1 = var.getPos() + var.getLength();
					int pos2 = exp.getPos();
					String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
					String codePart2 = getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
					String[] operators = {" += ", " -= ", " *= ", " /= ", " %= ", " &= ", " ^= ", " |= ", " <<= ", " >>= ", " >>>= "};
					for (String operator : operators) {
						generatePatch(codePart1 + operator + codePart2);
					}
				}
			} else {
				if (assignmentExp.getChildren().size() < 3) return;
				ITree exp = assignmentExp.getChild(2);
				int pos1 = var.getPos() + var.getLength();
				int pos2 = exp.getPos();
				String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
				String codePart2 = getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
				generatePatch(codePart1 + " = " + codePart2);
			}
		}
	}

	/**
	 * Fix pattern for NS_DANGEROUS_NON_SHORT_CIRCUIT, BIT_IOR, BIT_SIGNED_CHECK violations.
	 * 
	 * @author kui.liu
	 *
	 */
	private class BitIorOperatorMutator {
		/*
		 * NS_DANGEROUS_NON_SHORT_CIRCUIT:
		 * -   leftExp  &/|  rightExp
		 * +   leftExp &&/|| rightExp
		 * 
		 * BIT_IOR:
		 * -  exp1 | exp2;
		 * +  exp1 & exp2;
		 * 
		 * BIT_SIGNED_CHECK:
		 * - (exp1 & exp2) > 0;
		 * + (exp1 & exp2) == exp2;
		 */
		protected void generatePatches() {
			if (bitIorExps.isEmpty()) return;
			for (Pair<ITree, String> bitIorExpPair : bitIorExps) {
				// NS_DANGEROUS_NON_SHORT_CIRCUIT
				ITree buggyExp = bitIorExpPair.getFirst();
				String op =  bitIorExpPair.getSecond();
				int pos1 = buggyExp.getChild(0).getPos() + buggyExp.getChild(0).getLength();
				int pos2 = buggyExp.getChild(2).getPos();
				generatePatch(getSubSuspiciouCodeStr(suspCodeStartPos, pos1) + op + op + getSubSuspiciouCodeStr(pos2, suspCodeEndPos));
				
				if ("|".equals(bitIorExpPair.getSecond())) {
					// BIT_IOR
					generatePatch(getSubSuspiciouCodeStr(suspCodeStartPos, pos1) + " & " + getSubSuspiciouCodeStr(pos2, suspCodeEndPos));
				} else {
					// BIT_SIGNED_CHECK
					ITree parentTree = buggyExp.getParent();
					if (!Checker.isParenthesizedExpression(parentTree.getType())) continue;
					parentTree = parentTree.getParent();
					if (Checker.isInfixExpression(parentTree.getType())) {
						String operator = parentTree.getChild(1).getLabel();
						if (">".equals(operator) && "0".equals(parentTree.getChild(2).getLabel())) {
							ITree leffHandExp = parentTree.getChild(0);
							pos1 = leffHandExp.getPos() + leffHandExp.getLength();
							ITree rightHandExp = parentTree.getChild(2);
							int pos3 = pos2 + buggyExp.getChild(2).getLength();
							int pos4 = rightHandExp.getPos() + rightHandExp.getLength();
							generatePatch(getSubSuspiciouCodeStr(suspCodeStartPos, pos1) + " == "
									+ getSubSuspiciouCodeStr(pos2, pos3) + getSubSuspiciouCodeStr(pos4, suspCodeEndPos));
						}
					}
				}
			}
		}
	}

	/**
	 * Fix pattern for IM_AVERAGE_COMPUTATION_COULD_OVERFLOW, IM_BAD_CHECK_FOR_ODD violations.
	 * 
	 * @author kui.liu
	 *
	 */
	private class ArithmeticOperatorMutator {
		/*
		 * IM_AVERAGE_COMPUTATION_COULD_OVERFLOW:
		 * - numExp / 2;
		 * + numExp >>> 1;
		 * 
		 * IM_BAD_CHECK_FOR_ODD:
		 * Fix pattern 1:
		 * - (numExp % 2) == 1
		 * + (numExp & 1) == 1
		 * Fix pattern2:
		 * -  numExp % 2 == 1;
		 * +  numExp % 2 != 0;
		 */
		protected void generatePatches() {
			for (Pair<ITree, String> arithmeticOpExpPair : arithmeticOpExps) {
				String op = arithmeticOpExpPair.getSecond();
				if ("/".equals(op)) {
					ITree arithmeticOpExp = arithmeticOpExpPair.getFirst();
					ITree rightHandExp = arithmeticOpExp.getChild(2);
					if (Checker.isNullLiteral(rightHandExp.getType()) && "2".equals(rightHandExp.getLabel())) {
						ITree leftHandExpTree = arithmeticOpExp.getChild(0);
						int startPos = leftHandExpTree.getPos() + leftHandExpTree.getLength();
						int endPos = rightHandExp.getPos() + rightHandExp.getLength();
						generatePatch(getSubSuspiciouCodeStr(suspCodeStartPos, startPos) + " >>> 1 " + getSubSuspiciouCodeStr(endPos, suspCodeEndPos));
					}
				} else if ("%".equals(op)) {
					// IM_BAD_CHECK_FOR_ODD
					ITree arithmeticOpExp = arithmeticOpExpPair.getFirst();
					if ("2".equals(arithmeticOpExp.getChild(2).getLabel())) {
						ITree parentTree = arithmeticOpExp.getParent();
						if (Checker.isInfixExpression(parentTree.getType())) {
						} else if (Checker.isParameterizedType(parentTree.getType())) {
							parentTree = parentTree.getParent();
							if (!Checker.isInfixExpression(parentTree.getType())) parentTree = null;
						} else parentTree = null;
						if (parentTree != null) {
							if ("==".equals(parentTree.getChild(1).getLabel()) && "1".equals(parentTree.getChild(2).getLabel())) {
								int pos1 = arithmeticOpExp.getChild(0).getPos() + arithmeticOpExp.getChild(0).getLength();
								int pos2 = arithmeticOpExp.getChild(2).getPos();
								generatePatch(getSubSuspiciouCodeStr(suspCodeStartPos, pos1) + " & " + getSubSuspiciouCodeStr(pos2, suspCodeEndPos));
								
								pos1 = arithmeticOpExp.getPos() + arithmeticOpExp.getLength();
								pos2 = parentTree.getPos() + parentTree.getLength();
								generatePatch(getSubSuspiciouCodeStr(suspCodeStartPos, pos1) + " != 0 " + getSubSuspiciouCodeStr(pos2, suspCodeEndPos));
							}
						}
					}
				}

				ITree opExpTree = arithmeticOpExpPair.getFirst();
				int pos1 = opExpTree.getChild(0).getPos() + opExpTree.getChild(0).getLength();
				int pos2 = opExpTree.getChild(2).getPos();
				String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
				String codePart2 = getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
//				for (String operator : arithmeticOperators) {
//					if (operator.equals(op)) continue;
//					generatePatch(codePart1 + operator + codePart2);
//				}
				if ("*".equals(op)) {
					generatePatch(codePart1 + " / " + codePart2);
				} else if ("/".equals(op)) {
					generatePatch(codePart1 + " * " + codePart2);
					generatePatch(codePart1 + " % " + codePart2);
				} else if ("+".equals(op)) {
					generatePatch(codePart1 + " - " + codePart2);
				} else if ("-".equals(op)) {
					generatePatch(codePart1 + " + " + codePart2);
				} else if ("%".equals(op)) {
					generatePatch(codePart1 + " / " + codePart2);
				}
				
				int size = opExpTree.getChildren().size();
				for (int i = 3; i < size; i ++) {
					pos1 = opExpTree.getChild(i - 1).getPos() + opExpTree.getChild(i - 1).getLength();
					pos2 = opExpTree.getChild(i).getPos();
					codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
					codePart2 = getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
					if ("*".equals(op)) {
						generatePatch(codePart1 + " / " + codePart2);
					} else if ("/".equals(op)) {
						generatePatch(codePart1 + " * " + codePart2);
						generatePatch(codePart1 + " % " + codePart2);
					} else if ("+".equals(op)) {
						generatePatch(codePart1 + " - " + codePart2);
					} else if ("-".equals(op)) {
						generatePatch(codePart1 + " + " + codePart2);
					} else if ("%".equals(op)) {
						generatePatch(codePart1 + " / " + codePart2);
					}
				}
			}
		}
	}
	
}
