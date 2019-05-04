package edu.lu.uni.serval.jdt.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

/**
 * Extend JdtVisitor by visiting Expressions and adding expression type to tree label.
 * 
 * @author kui.liu
 *
 */
public class ExpJdtVisitor extends JdtVisitor {
	
	
	@Override
	public boolean visit(CompilationUnit node) {
		pushNode(node, "JavaFile");
		return true;
	}
	
	@Override
	public void endVisit(CompilationUnit node) {
		popNode();
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		pushNode(node, node.getName().toString());
        return true;
	}
	
	@Override
	public void endVisit(EnumConstantDeclaration node) {
		popNode();
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		String label = "";
    	int startPosition = 0;
    	int nodeStartPosition = node.getStartPosition();
    	int length = node.getLength();
    	List<?> modifiers = node.modifiers();
    	if (modifiers.size() > 0) {
    		label += modifiers.toString();
    		startPosition = ((ASTNode)  modifiers.get(0)).getStartPosition();
    	}
    	label += node.getName().toString() + ", ";
    	if (startPosition == 0) {
    		startPosition = node.getName().getStartPosition();
    	}
    	
    	List<?> interfaceTypes = node.superInterfaceTypes(); // type
    	if (interfaceTypes.size() > 0) {
    		label += interfaceTypes.toString();
    	}

    	push(node.getNodeType(), node.getClass().getSimpleName(), label, startPosition, nodeStartPosition + length - startPosition);
        return true;
	}
	
	@Override
	public void endVisit(EnumDeclaration node) {
		popNode();
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		pushNode(node, node.getName().toString());
		return true;
	}
	
	@Override
	public void endVisit(AnnotationTypeDeclaration node) {
		popNode();
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		pushNode(node, node.getName().toString());
		return true;
	}
	
	@Override
	public void endVisit(AnnotationTypeMemberDeclaration node) {
		popNode();
	}

	////---------------Expressions---------------
	//  ----------------Annotation---------------
	@Override
	public boolean visit(MarkerAnnotation node) {
		pushNode(node, node.toString());
		return false;
	}

	@Override
	public void endVisit(MarkerAnnotation node) {
		popNode();
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		pushNode(node, node.toString());
		return false;
	}

	@Override
	public void endVisit(NormalAnnotation node) {
		popNode();
	}

	@Override
	public boolean visit(SingleMemberAnnotation node) {
		pushNode(node, node.toString());
		return false;
	}

	@Override
	public void endVisit(SingleMemberAnnotation node) {
		popNode();
	}
	// ---------------Annotation---------------
	
    @Override
	public boolean visit(ArrayAccess node) {
		pushNode(node, node.toString());
    	Expression arrayExpression = node.getArray();
		Expression indexExpression = node.getIndex();
		arrayExpression.accept(this);
		indexExpression.accept(this);
		return false;
	}

	@Override
	public void endVisit(ArrayAccess node) {
		popNode();
	}

	@Override
	public boolean visit(ArrayCreation node) {
		pushNode(node, node.toString());
		ArrayType arrayType = node.getType();
		arrayType.accept(this);
		List<?> dimensions = node.dimensions();
		visitList(dimensions);
		ArrayInitializer initializer = node.getInitializer();
		if (initializer != null) {
			initializer.accept(this);
		}
		return false;
	}

	@Override
	public void endVisit(ArrayCreation node) {
		popNode();
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		pushNode(node, node.toString());
		List<?> expressions = node.expressions();
		visitList(expressions);
		return false;
	}

	@Override
	public void endVisit(ArrayInitializer node) {
		popNode();
	}

	@Override
	public boolean visit(Assignment node) {
		pushNode(node, node.toString());
		Expression leftHandExp = node.getLeftHandSide();
		leftHandExp.accept(this);
		String op = node.getOperator().toString();
		push(-1, "Operator", op, leftHandExp.getStartPosition() + leftHandExp.getLength(), op.length());
		popNode();
		Expression rightHandExp = node.getRightHandSide();
		rightHandExp.accept(this);
		return false;
	}

	@Override
	public void endVisit(Assignment node) {
		popNode();
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		pushNode(node, node.toString());
		return false;
	}

	@Override
	public void endVisit(BooleanLiteral node) {
		popNode();
	}

	@Override
	public boolean visit(CastExpression node) {
		pushNode(node, node.toString());
		Type castType = node.getType();
		castType.accept(this);
		Expression exp = node.getExpression();
		exp.accept(this);
		return false;
	}

	@Override
	public void endVisit(CastExpression node) {
		popNode();
	}

	@Override
	public boolean visit(CharacterLiteral node) {
		pushNode(node, node.getEscapedValue());
		return false;
	}

	@Override
	public void endVisit(CharacterLiteral node) {
		popNode();
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
//		pushNode(node, node.toString());
		String label = "";
		Expression exp = node.getExpression();
		if (exp != null) { 
			// Example: ClassInstanceCreation:trie
			//          node:trie.new TSTNode('\0',node)
			label = exp.toString();
		}
//		List<?> typeArguments = node.typeArguments();
//		for (Object obj : typeArguments) {
//			Type typeArgu = (Type) obj;// TypeArugment: Collections.<T>emptyList()
//			pushNode(typeArgu, "TypeArgument:" + typeArgu.getClass().getSimpleName() + ":" + typeArgu.toString());
//			popNode();
//		}
		Type type = node.getType();
		label += type.toString();
		List<?> arguments = node.arguments();
		label += arguments.toString();
		pushNode(node, label);
		
		if (exp != null) exp.accept(this);
		push(-2, "New", "new", type.getStartPosition() - 4, 3);
		popNode();
		type.accept(this);
		visitList(arguments);
		AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
		if (acd != null) {
			acd.accept(this);
		}
		return false;
	}

	@Override
	public void endVisit(ClassInstanceCreation node) {
		popNode();
	}
	
	@Override
	public boolean visit(ConditionalExpression node) {
		pushNode(node, node.toString());
		Expression conditionalExp = node.getExpression();
		Expression thenExp = node.getThenExpression(); 
		Expression elseExp = node.getElseExpression();
		conditionalExp.accept(this);
		thenExp.accept(this);
		elseExp.accept(this);
		return false;
	}

	@Override
	public void endVisit(ConditionalExpression node) {
		popNode();
	}

	@Override
	public boolean visit(FieldAccess node) {
		pushNode(node, node.toString());
		Expression exp = node.getExpression();
		exp.accept(this);
		SimpleName identifier = node.getName();
		identifier.accept(this);
		return false;
	}

	@Override
	public void endVisit(FieldAccess node) {
		popNode();
	}

	@Override
	public boolean visit(InfixExpression node) {
		pushNode(node, node.toString());
		
		Expression leftExp = node.getLeftOperand();
		leftExp.accept(this);
		
		String op = node.getOperator().toString();
		push(-1, "Operator", op, leftExp.getStartPosition() + leftExp.getLength(), op.length());
		popNode();
		
		Expression rightExp = node.getRightOperand();
		rightExp.accept(this);
		
		List<?> extendedOperands = node.extendedOperands();
		visitList(extendedOperands);
		
		return false;
	}

	@Override
	public void endVisit(InfixExpression node) {
		popNode();
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		pushNode(node, node.toString());
		Expression exp = node.getLeftOperand();
		exp.accept(this);
		push(-3, "Instanceof", "instanceof", exp.getStartPosition() + exp.getLength() + 1, 10);
		popNode();
		Type type = node.getRightOperand();
		type.accept(this);
		return false;
	}

	@Override
	public void endVisit(InstanceofExpression node) {
		popNode();
	}

	@Override
	public boolean visit(LambdaExpression node) {
		pushNode(node, node.toString());
		List<?> parameters = node.parameters();
		visitList(parameters);
		ASTNode body = node.getBody();
		if (body instanceof Block) {
			visitBody((Block) body);
		} else  {
			body.accept(this);
		}
		return false;
	}

	@Override
	public void endVisit(LambdaExpression node) {
		popNode();
	}

	@Override
	public boolean visit(MethodInvocation node) {
		Expression exp = node.getExpression();
//		List<?> typeArguments = node.typeArguments();
		SimpleName methodName = node.getName();
		List<?> arguments = node.arguments();
		if (exp == null && arguments.size() == 0) {
			pushNode(node, "MethodName:" + methodName.getFullyQualifiedName() + ":" + arguments.toString());
		} else {
			pushNode(node, node.toString());
			List<MethodInvocation> methods = new ArrayList<>();
			while (exp != null) {
				if (exp instanceof MethodInvocation) {
					MethodInvocation method = (MethodInvocation) exp;
					methods.add(0, method);
					exp = method.getExpression();
				} else {
					if (exp instanceof Name) {
						pushNode(exp, "Name:" + exp.toString());
						popNode();
					} else {
						exp.accept(this);	
					}
					exp = null;
				}
			}
			for (MethodInvocation method : methods) {
				List<?> argumentsList = method.arguments();
				pushNode(method, "MethodName:" + method.getName().getFullyQualifiedName() + ":" + argumentsList.toString());
				visitList(argumentsList);
				popNode();
			}
//			for (Object obj : typeArguments) {
//				Type typeArgu = (Type) obj; // TypeArugment: Collections.<T>emptyList()
//				pushNode(typeArgu, "TypeArgument:" + typeArgu.getClass().getSimpleName() + ":" + typeArgu.toString());
//				popNode();
//			}
			push(42, "SimpleName", "MethodName:" + methodName.getFullyQualifiedName() + ":" + arguments.toString(), 
					methodName.getStartPosition(), node.getStartPosition() + node.getLength() - methodName.getStartPosition());
	    	visitList(arguments);
	    	popNode();
		}
		return false;
	}

	@Override
	public void endVisit(MethodInvocation node) {
		popNode();
	}
	
	// ----------------MethodReference----------------
	@Override
	public boolean visit(CreationReference node) {
//		pushNode(node, node.toString());
//		Type type = node.getType();
//		type.accept(this);
//		List<?> typeArguments =  node.typeArguments();
//		for (Object obj : typeArguments) {
//			Type typeArgument = (Type) obj;
//			typeArgument.accept(this);
//		}
		return false;
	}

	@Override
	public void endVisit(CreationReference node) {
//		popNode();
	}

	@Override
	public boolean visit(ExpressionMethodReference node) {
//		pushNode(node, node.getName().getFullyQualifiedName());
//		Expression exp = node.getExpression();
//		exp.accept(this);
//		List<?> typeArguments = node.typeArguments();
//		for (Object obj : typeArguments) {
//			Type typeArgument = (Type) obj;
//			typeArgument.accept(this);
//		}
		return false;
	}

	@Override
	public void endVisit(ExpressionMethodReference node) {
//		popNode();
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		// TODO Auto-generated method stub
		return false;//super.visit(node);
	}

	@Override
	public void endVisit(SuperMethodReference node) {
		// TODO Auto-generated method stub
//		super.endVisit(node);
	}
	
	@Override
	public boolean visit(TypeMethodReference node) {
		// TODO Auto-generated method stub
		return false;//super.visit(node);
	}

	@Override
	public void endVisit(TypeMethodReference node) {
		// TODO Auto-generated method stub
//		super.endVisit(node);
	}
	// ----------------MethodReference----------------
	
	// ----------------Name----------------
	@Override
	public boolean visit(QualifiedName node) {
		pushNode(node, node.toString());
		Name name = node.getQualifier();
		SimpleName simpleName = node.getName();
		name.accept(this);
		simpleName.accept(this);
		return false;
	}

	@Override
	public void endVisit(QualifiedName node) {
		popNode();
	}

	@Override
	public boolean visit(SimpleName node) {
		pushNode(node, node.getFullyQualifiedName());
		return false;
	}

	@Override
	public void endVisit(SimpleName node) {
		popNode();
	}
	// ----------------Name----------------

	@Override
	public boolean visit(NullLiteral node) {
		pushNode(node, "null");
		return false;
	}

	@Override
	public void endVisit(NullLiteral node) {
		popNode();
	}

	@Override
	public boolean visit(NumberLiteral node) {
		String num = node.getToken();
//		if (num.endsWith("L") || num.endsWith("l")) {
//			num = "longNumber";
//		}
		pushNode(node, num);
		return false;
	}

	@Override
	public void endVisit(NumberLiteral node) {
		popNode();
	}

	@Override
	public boolean visit(ParenthesizedExpression node) {
		pushNode(node, node.toString());
		Expression exp = node.getExpression();
		exp.accept(this);
		return false;
	}

	@Override
	public void endVisit(ParenthesizedExpression node) {
		popNode();
	}

	@Override
	public boolean visit(PostfixExpression node) {
		pushNode(node, node.toString());
		Expression exp = node.getOperand();
		exp.accept(this);
		String op = node.getOperator().toString();
		push(-1, "Operator", op, exp.getStartPosition() + exp.getLength() + 1, op.length());
		popNode();
		return false;
	}

	@Override
	public void endVisit(PostfixExpression node) {
		popNode();
	}

	@Override
	public boolean visit(PrefixExpression node) {
		pushNode(node, node.toString());
		String op = node.getOperator().toString();
		push(-1, "Operator", op, node.getStartPosition(), op.length());
		popNode();
		Expression exp = node.getOperand();
		exp.accept(this);
		return false;
	}

	@Override
	public void endVisit(PrefixExpression node) {
		popNode();
	}
	
	@Override
	public boolean visit(StringLiteral node) {
		pushNode(node, node.getEscapedValue());
		return false;
	}

	@Override
	public void endVisit(StringLiteral node) {
		popNode();
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		pushNode(node, node.toString());
		Name className = node.getQualifier();
		SimpleName identifier = node.getName();
		if (className != null) {
			className.accept(this);
		}
		identifier.accept(this);
		return false;
	}

	@Override
	public void endVisit(SuperFieldAccess node) {
		popNode();
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		pushNode(node, node.toString());
		Name className = node.getQualifier();
		if (className != null) {
			className.accept(this);
		}
		SimpleName methodName = node.getName();
		List<?> arguments = node.arguments();
		pushNode(methodName, "MethodName:" + methodName.getFullyQualifiedName() + ":" + arguments.toString());
		visitList(arguments);
		popNode();
		return false;
	}

	@Override
	public void endVisit(SuperMethodInvocation node) {
		popNode();
	}

	@Override
	public boolean visit(ThisExpression node) {
		pushNode(node, "this");
		return false;
	}

	@Override
	public void endVisit(ThisExpression node) {
		popNode();
	}

    @Override
    public boolean visit(TypeLiteral node) {
        pushNode(node, node.toString());
        return false;
    }

    @Override
    public void endVisit(TypeLiteral node) {
        popNode();
    }
	
    @Override
    public boolean visit(VariableDeclarationExpression node) {
        pushNode(node, node.toString());
        List<?> modifiers = node.modifiers();
        for (Object obj : modifiers) {
        	IExtendedModifier modifier = (IExtendedModifier) obj;
        	if (modifier.isModifier()) {
        		((Modifier)modifier).accept(this);
        	}
        }
        node.getType().accept(this);
        visitList(node.fragments());
        return false;
    }

    @Override
    public void endVisit(VariableDeclarationExpression node) {
        popNode();
    }
    ////---------------End of Expressions---------------
    
    ////////
    @Override
   	public boolean visit(ArrayType node) {
   		pushNode(node, node.toString());
   		Type type = node.getElementType();
   		type.accept(this);
//   		List<?> dimensions = node.dimensions();
   		return false;
   	}

   	@Override
   	public void endVisit(ArrayType node) {
   		popNode();
   	}
   	
	@Override
	public boolean visit(Dimension node) {
		// TODO
		return false;
	}

	@Override
	public void endVisit(Dimension node) {
	}

	///////////// 
	@Override
	public boolean visit(PackageDeclaration node) {
		String nodeStr = node.toString();
    	nodeStr = nodeStr.substring(8, nodeStr.length() - 2);
    	pushNode(node, nodeStr);
    	return false;
	}

	@Override
	public void endVisit(PackageDeclaration node) {
		popNode();
	}

	
    @Override
	public boolean visit(ImportDeclaration node) {
    	String nodeStr = node.toString();
    	nodeStr = nodeStr.substring(7, nodeStr.length() - 2);
    	pushNode(node, nodeStr);
    	return false;
    }
    
    @Override
    public void endVisit(ImportDeclaration node) {
    	popNode();
    }
    
    @Override
    public boolean visit(Javadoc node) {
        return false;
    }

    @Override
    public void endVisit(Javadoc node) {
    }

    @Override
    public boolean visit(TypeDeclaration node) {
    	String label = "";
    	int startPosition = 0;
    	int nodeStartPosition = node.getStartPosition();
    	int length = node.getLength();
    	List<?> modifiers = node.modifiers();
    	if (modifiers.size() > 0) {
    		label += modifiers.toString();
    		startPosition = ((ASTNode)  modifiers.get(0)).getStartPosition();
    	}
    	label += "ClassName:" + node.getName().toString() + ", ";
    	if (startPosition == 0) {
    		startPosition = node.getName().getStartPosition();
    	}
    	
    	Type superClass = node.getSuperclassType();
    	if (superClass != null) {
    		label += "@@SuperClass:" + superClass.toString() + ", ";
    	}
    	List<?> interfaceTypes = node.superInterfaceTypes(); // type
    	if (interfaceTypes.size() > 0) {
    		label += "@@Interface:" + interfaceTypes.toString();
    	}
    	
    	//push(int type, String typeName, String label, int startPosition, int length)
    	push(node.getNodeType(), node.getClass().getSimpleName(), label, startPosition, nodeStartPosition + length - startPosition);
//    	pushNode(node, label);
    	visitList(modifiers);
    	pushNode(node.getName(), "ClassName:" + node.getName().toString());
    	popNode();
    	if (superClass != null) {
    		superClass.accept(this);
    	}
//    	List<?> typeParameters = node.typeParameters(); // TypeParameters
    	visitList(interfaceTypes);
    	
    	List<?> body = node.bodyDeclarations();
    	visitList(body);
        return false;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
    	String nodeStr = "";
    	int startPosition = 0;
    	int nodeStartPosition = node.getStartPosition();
    	int length = node.getLength();
    	
    	List<?> modifiers = node.modifiers();
    	List<Modifier> realModifiers = new ArrayList<>();
        for (Object obj : modifiers) {
        	IExtendedModifier modifier = (IExtendedModifier) obj;
        	if (modifier.isModifier()) {
        		nodeStr += modifier.toString() + ", ";
        		realModifiers.add((Modifier)modifier);
        		if (startPosition == 0) {
        			startPosition = ((Modifier) modifier).getStartPosition();
        		}
        	}
        }
        Type type = node.getType();
        if (startPosition == 0) {
        	startPosition = type.getStartPosition();
        }
        nodeStr += type.toString() + ", ";
        List<?> fragments = node.fragments();
        nodeStr += fragments.toString();
//        pushNode(node, nodeStr);
    	push(node.getNodeType(), node.getClass().getSimpleName(), nodeStr, startPosition, nodeStartPosition + length - startPosition);
    	
        visitList(realModifiers);
        type.accept(this);
        visitList(fragments);
        return false;
    }

    @Override
	public boolean visit(Initializer node) {
    	pushNode(node, "Initializer");
		Block body = node.getBody();
		if (body != null) {
			List<?> statements = body.statements();
			visitList(statements);
		}
		return false;
	}
    
    @Override
    public void endVisit(Initializer node) {
    	popNode();
    }
    
	@Override
	public boolean visit(MethodDeclaration node) {

    	int startPosition = 0;
    	String returnTypeStr = "";
    	Type returnType = null;
    	if (node.isConstructor()) {
    		returnTypeStr = "=CONSTRUCTOR=";
    	} else {
    		returnType = node.getReturnType2();
    		returnTypeStr = ((returnType == null) ? "void" : returnType.toString());
    	}
		List<?> modifiers = node.modifiers();
		List<?> typeParameters = node.typeParameters();
		SimpleName methodName = node.getName();
		List<?> parameters = node.parameters();
		List<?> exceptionTypes = node.thrownExceptionTypes();
		List<Modifier> realModifiers = new ArrayList<>();
		
		String methodLabel = "";
		for (Object obj : modifiers) {
			IExtendedModifier modifier = (IExtendedModifier) obj;
			if (modifier.isModifier()) {
				methodLabel += obj.toString() + ", ";
				realModifiers.add((Modifier) modifier);
				if (startPosition == 0) {
					startPosition = ((Modifier) modifier).getStartPosition();
				}
			}
		}
		methodLabel += "@@" + returnTypeStr + ", ";
		if (typeParameters != null && typeParameters.size() > 0) {
			methodLabel += "@@tp:";
			for (Object obj : typeParameters) {
				methodLabel += obj.toString() + ", ";
			}
		}
		methodLabel += "MethodName:" + methodName + ", ";
		if (startPosition == 0) {
			startPosition = methodName.getStartPosition();
		}
		if (parameters == null || parameters.size() == 0) {
			methodLabel += "@@Argus:null";
		} else {
			methodLabel += "@@Argus:";
			for (Object obj : parameters) {
				SingleVariableDeclaration svd = (SingleVariableDeclaration) obj;
				String arguType = svd.getType().toString();
				String arguName = svd.getName().getFullyQualifiedName();
				methodLabel += arguType + "+" + arguName + "+";
			}
		}
		if (exceptionTypes != null && exceptionTypes.size() > 0) {
			methodLabel += "@@Exp:";
			for (Object obj : exceptionTypes) {
				methodLabel += obj.toString() + "+";
			}
		}
		
		pushNode(node, methodLabel);
//		push(node.getNodeType(), node.getClass().getSimpleName(), methodLabel, startPosition, nodeStartPosition + length - startPosition);
		
        /*
		 *  The visiting of the below elements (except modifiers and body) can be removed, 
		 *  because there is no any fix pattern can be mined from these elements.
		 *  Even though some fix patterns can be mined, they are not what we want.
		 */
        visitList(realModifiers);
		if (returnType != null) {
			returnType.accept(this);
		}
//		visitList(typeParameters);
		pushNode(methodName, "MethodName:" + methodName.getFullyQualifiedName());
		popNode();
		visitList(parameters);
//		visitList(exceptionTypes);

		// The body can be null when the method declaration is from a interface
		Block methodBody = node.getBody();
		if (methodBody != null) {
//			push(8, "Block", "MethodBody", methodBody.getStartPosition(), methodBody.getLength());
//			methodBody.accept(this);
			List<?> stmts = methodBody.statements();
			visitList(stmts);
//			popNode();
		}
		return false;

	}

    //-----------------Types-----------------
    @Override
    public boolean visit(NameQualifiedType node) {
    	// Name <b>.</b> { Annotation } SimpleName
    	pushNode(node, node.toString());
    	return false;
    }
    
    @Override
    public void endVisit(NameQualifiedType node) {
    	popNode();
    }
    
    @Override
    public boolean visit(ParameterizedType node) {
        pushNode(node, node.toString());
        node.getType().accept(this);
        visitList(node.typeArguments());
        return false;
    }

    @Override
    public boolean visit(PrimitiveType node) {
        pushNode(node, node.getPrimitiveTypeCode().toString());
        return false;
    }

    @Override
    public boolean visit(QualifiedType node) {
        pushNode(node, node.toString());
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        pushNode(node, node.getName().getFullyQualifiedName());
        return false;
    }
    
    @Override
	public boolean visit(WildcardType node) {
    	pushNode(node, "?");
		return false;
	}
    
    @Override
    public void endVisit(WildcardType node) {
    	popNode();
    }

	@Override
	public boolean visit(IntersectionType node) {
        pushNode(node, node.toString());
		return super.visit(node);
	}
    
    @Override
    public void endVisit(IntersectionType node) {
    	popNode();
    }

	@Override
	public boolean visit(UnionType node) {
        pushNode(node, node.toString());
		return super.visit(node);
	}
    
    @Override
    public void endVisit(UnionType node) {
    	popNode();
    }
    //-----------------Types-----------------
    
	@Override
    public boolean visit(SingleVariableDeclaration node) {
        pushNode(node, node.toString());
        List<?> modifiers = node.modifiers();
        for (Object obj : modifiers) {
        	IExtendedModifier modifier = (IExtendedModifier) obj;
        	if (modifier.isModifier()) {
        		((Modifier) modifier).accept(this);
        	}
        }
    	Type type = node.getType();
    	SimpleName variableName = node.getName();
    	Expression exp = node.getInitializer();
        type.accept(this);
        variableName.accept(this);
        if (exp != null) {
        	exp.accept(this);
        }
        return false;
    }
    
    @Override
    public boolean visit(VariableDeclarationFragment node) {
    	pushNode(node, node.toString());
    	SimpleName variableName = node.getName();
    	variableName.accept(this);
        Expression exp = node.getInitializer();
        if (exp != null) {
        	exp.accept(this);
        }
        return false;
    }

    /////////////////////
    private void visitList(List<?> list) {
        for (Object obj : list) {
        	ASTNode node = (ASTNode) obj;
            (node).accept(this);
        }
    }
    ///////////////////
    
    ////***************BODY VISITOR*************************
    private static final String COLON = ":";
    
    @Override
    public boolean visit(CatchClause node) {
        pushNode(node, node.getException().toString());
        SingleVariableDeclaration exc = node.getException();
        exc.accept(this);
        Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "CatchBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
//        visitBody(node.getBody());
        return false;
    }

    @Override
    public void endVisit(CatchClause node) {
        popNode();
    }

    ////-------------------Statements-------------------
    @Override
    public boolean visit(AssertStatement node) {
    	Expression exp = node.getExpression();
    	Expression msg = node.getMessage();
        String value = exp.getClass().getSimpleName() + COLON + exp.toString();
        if (msg != null) {
            value += ", Msg-" + msg.getClass().getSimpleName() + COLON + msg.toString();
            pushNode(node, value);
            exp.accept(this);
            msg.accept(this);
        } else {
        	pushNode(node, value);
            exp.accept(this);
        }
        return false;
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        String nodeStr = node.toString();
        nodeStr = nodeStr.substring(0, nodeStr.length() - 1);
        pushNode(node, nodeStr);
//        List<?> typeArguments = node.typeArguments();
        List<?> arguments = node.arguments();
//        visitList(typeArguments);
        visitList(arguments);
        return false;
    }

    @Override
    public boolean visit(DoStatement node) {
    	Expression exp = node.getExpression();
        pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
        Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "DoBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
        exp.accept(this);
        return false;
    }

    @Override
    public boolean visit(EmptyStatement node) {
        return false;
    }

    @Override
    public void endVisit(EmptyStatement node) {
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
    	SingleVariableDeclaration parameter = node.getParameter();
    	Expression exp = node.getExpression();
        pushNode(node, parameter.toString() + ", " + exp.getClass().getSimpleName() + COLON + exp.toString());
        parameter.accept(this);
        exp.accept(this);
        Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "EnForBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
//        visitBody(node.getBody());
        return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
    	Expression exp = node.getExpression();
        pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
        exp.accept(this);
        return false;
    }

    @Override
    public boolean visit(ForStatement node) {
        String value = "";
        List<?> init = node.initializers();
		Expression exp = node.getExpression();
		List<?> update = node.updaters();
        value += init.toString() + ";";
		if (exp != null) {
			value += exp.toString() + ";";
		}
		value += update.toString();
        
        pushNode(node, value);
		visitList(init);
		if (exp != null) {
			exp.accept(this);
		}
		visitList(update);
		
		Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "ForBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
//        visitBody(node.getBody());
        return false;
    }

    @Override
    public boolean visit(IfStatement node) {
        Expression exp = node.getExpression();
        pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
        exp.accept(this);
        Statement stmt = node.getThenStatement();
        if (stmt != null) {
        	push(8, "Block", "ThenBody", stmt.getStartPosition(), stmt.getLength());
            visitBody(stmt);
            popNode();
        }

        stmt = node.getElseStatement();
        if (stmt != null) {
        	push(8, "Block", "ElseBody", stmt.getStartPosition(), stmt.getLength());
            visitBody(stmt);
            popNode();
        }
        return false;
    }
    
    @Override
    public boolean visit(LabeledStatement node) {
        pushNode(node, node.getLabel().getFullyQualifiedName());
        Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "LabelBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
//        visitBody(node.getBody());
        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
    	Expression exp = node.getExpression();
    	if (exp != null) {
    		pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
    		exp.accept(this);
    	} else {
            pushNode(node, "");
    	}
        return false;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
    	String nodeStr = node.toString();
    	nodeStr = nodeStr.substring(0, nodeStr.length() - 1);
        pushNode(node, node.toString());
        visitList(node.arguments());
        return false;
    }

    @Override
    public boolean visit(SwitchCase node) {
    	Expression exp = node.getExpression();
    	if (exp != null) {
    		pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
    		exp.accept(this);
    	} else {
    		pushNode(node, "default");
    	}
        return false;
    }

    @Override
    public boolean visit(SwitchStatement node) {
    	Expression exp = node.getExpression();
        pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
        exp.accept(this);
//        int startPosition = exp.getStartPosition();
//        int length1 = exp.getLength();
//        int length2 = node.getLength();
//        push(8, "Block", "SwitchBody", startPosition + length1 + 1, node.getStartPosition() + length2 - startPosition - length1 - 1);
        visitList(node.statements());
//		popNode();
        return false;
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
    	Expression exp = node.getExpression();
        pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
        exp.accept(this);
        Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "SyncBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
//        visitBody(node.getBody());
        return false;
    }

    @Override
    public boolean visit(ThrowStatement node) {
    	Expression exp = node.getExpression();
        pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
        exp.accept(this);
        return false;
    }

    @Override
    public boolean visit(TryStatement node) {
    	List<?> resources = node.resources();
    	pushNode(node, "try:" + resources.toString());
		visitList(resources);

		Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "TryBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
//        visitBody(node.getBody());

        visitList(node.catchClauses());

        Statement stmt = node.getFinally();
        if (stmt != null) {
        	push(8, "Block", "FinallyBody", stmt.getStartPosition(), stmt.getLength());
            visitBody(stmt);
            popNode();
        }
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
    	String nodeStr = node.toString();
    	nodeStr = nodeStr.substring(0, nodeStr.length() - 1);
        pushNode(node, nodeStr);
        List<?> modifiers = node.modifiers();
        for (Object obj : modifiers) {
        	IExtendedModifier modifier = (IExtendedModifier) obj;
        	if (modifier.isModifier()) {
        		((Modifier)modifier).accept(this);
        	}
        }
        
    	Type type = node.getType();
    	type.accept(this);
    	List<?> fragments = node.fragments();
    	visitList(fragments);
        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
    	Expression exp = node.getExpression();
        pushNode(node, exp.getClass().getSimpleName() + COLON + exp.toString());
        exp.accept(this);
        
        Statement body = node.getBody();
        if (body != null) {
//        	push(8, "Block", "WhileBody", body.getStartPosition(), body.getLength());
        	visitBody(body);
//        	popNode();
        }
        return false;
    }

	private void visitBody(Statement body) {
		if (body instanceof Block) {
			List<?> statements = ((Block) body).statements();
			visitList(statements);
		} else {
			body.accept(this);
		}
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		pushNode(node, "AnonymousClass");
		List<?> bodyDeclarations = node.bodyDeclarations();
		visitList(bodyDeclarations);
		return false;
	}

	@Override
	public void endVisit(AnonymousClassDeclaration node) {
		popNode();
	}
}
