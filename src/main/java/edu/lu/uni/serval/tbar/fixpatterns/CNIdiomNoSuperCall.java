package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * Fix patterns for CN_IDIOM_NO_SUPER_CALL violations.
 * 
 * Context: overwritten clone method and class instance creation expression.
 * 
 * @author kui.liu
 *
 */
public class CNIdiomNoSuperCall extends FixTemplate {

	/*
	 * clone() method.
	 * 
	 * Fix pattern 1:
	 * -    ... new T(args);
	 * +    (T)super.clone();
	 * 
	 * Fix pattern 2:
	 * -    return new T(this);
	 * +    try {
	 * +      return (T) super.clone();
	 * +    } catch (CloneNotSupportedException cnse) {
	 * +      throw new RuntimeException("...");
	 * +    }
	 */
	
	@Override
	public void generatePatches() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		ITree cloneMethod = findCloneMethodDeclaration(suspCodeTree);
		if (cloneMethod != null) {
			ITree classTree = cloneMethod.getParent();
			if (classTree == null) return;
			if (Checker.isTypeDeclaration(classTree.getType())) return;
			
			String classLabel = classTree.getLabel();
			String className = classLabel.substring(classLabel.indexOf("ClassName:") + 10);
			className = className.substring(0, className.indexOf(", "));
			
			List<Pair<ITree, ITree>> buggyExpPairs = identifyBuggyClassInstanceCreations(suspCodeTree, className);
		
			for (Pair<ITree, ITree> buggyExpPair : buggyExpPairs) {
				ITree buggyExp = buggyExpPair.getFirst();
				ITree parentTree = buggyExpPair.getSecond();
				String fixedCodeStr;
				if (Checker.isReturnStatement(parentTree.getType())) {
					fixedCodeStr = "try {\n        return (" + className+ ") super.clone();\n";
					fixedCodeStr += "} catch (CloneNotSupportedException cnse) {\n";
					fixedCodeStr += "        throw new RuntimeException(\"...\");\n}\n";
				} else {
					int startPos = buggyExp.getPos();
					int endPos = startPos + buggyExp.getLength();
					String codePart1 = this.getSubSuspiciouCodeStr(this.suspCodeStartPos, startPos);
					String codePart2 = this.getSubSuspiciouCodeStr(endPos, this.suspCodeEndPos);
					fixedCodeStr = codePart1 + "(" + className+ ") super.clone()" + codePart2;
				}
				this.generatePatch(fixedCodeStr);
			}
		}
	}

	private ITree findCloneMethodDeclaration(ITree suspCodeTree) {
		ITree parent = suspCodeTree.getParent();
		while (true) {
			if (parent == null) return null;
			if (Checker.isMethodDeclaration(parent.getType())) {
				break;
			} else if (Checker.isTypeDeclaration(parent.getType())) {
				return null;
			}
			parent = parent.getParent();
		}
		String methodLabel = parent.getLabel();
		methodLabel = methodLabel.substring(methodLabel.indexOf("@@") + 2);
		int index = methodLabel.indexOf("MethodName:");
		String returnType = methodLabel.substring(0, index - 2);
		if ("boolean".equals(returnType) || "void".equals(returnType)) return null;
		methodLabel = methodLabel.substring(index + 11);
		if (!methodLabel.startsWith("clone, ")) return null;
		
		if (methodLabel.endsWith("@@Argus:null") || methodLabel.contains("@@Argus:null@@Exp:")) {
			return parent;
		}
		return null;
	}
	
	private List<Pair<ITree, ITree>> identifyBuggyClassInstanceCreations(ITree tree, String className) {
		List<Pair<ITree, ITree>> buggyExpPairs = new ArrayList<>();
		List<ITree> children = tree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isComplexExpression(childType)) {
				if (Checker.isClassInstanceCreation(childType)) {
					List<ITree> subChildren = child.getChildren();
					boolean isType = false;
					for (ITree subChild : subChildren) {
						if (isType) {
							String typeLabel = subChild.getLabel();
							if (typeLabel.equals(className)) {
								Pair<ITree, ITree> pair = new Pair<>(child, tree);
								buggyExpPairs.add(pair);
							}
							break;
						} else if (Checker.isNewKeyword(subChild.getType())) {
							isType = true;
						}
					}
				}
				buggyExpPairs.addAll(identifyBuggyClassInstanceCreations(child, className));
			} else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
				buggyExpPairs.addAll(identifyBuggyClassInstanceCreations(child, className));
			} else if (Checker.isStatement(childType)) {
				break;
			}
		}
		
		return buggyExpPairs;
	}

}
