package edu.lu.uni.serval.tbar.utils;

public class Checker {
	
	public static boolean withBlockStatement(String statementType) {
		if ("EnhancedForStatement".equals(statementType) || "ForStatement".equals(statementType) 
				|| "DoStatement".equals(statementType) || "WhileStatement".equals(statementType)
				|| "LabeledStatement".equals(statementType) || "SynchronizedStatement".equals(statementType)
				|| "IfStatement".equals(statementType) || "TryStatement".equals(statementType)  || "SwitchStatement".equals(statementType)) {
			return true;
		}
		return false;
	}
	
	public static boolean withBlockStatement(int type) {
		if (type == 19) return true; // DoStatement
		if (type == 24) return true; // ForStatement
		if (type == 25) return true; // IfStatement
		if (type == 30) return true; // LabeledStatement
		if (type == 50) return true; // SwitchStatement
		if (type == 51) return true; // SynchronizedStatement
		if (type == 54) return true; // TryStatement
		if (type == 61) return true; // WhileStatement
		if (type == 70) return true; // EnhancedForStatement
		return false;
	}
	
	public static boolean isStatement(int type) {
		if (type == 8)  return true; // block
		if (type == 6)  return true; // AssertStatement
		if (type == 10) return true; // BreakStatement
		if (type == 17) return true; // ConstructorInvocation
		if (type == 18) return true; // ContinueStatement
		if (type == 21) return true; // ExpressionStatement
		if (type == 41) return true; // ReturnStatement
		if (type == 46) return true; // SuperConstructorInvocation
		if (type == 49) return true; // SwitchCase
		if (type == 53) return true; // ThrowStatement
		if (type == 56) return true; // TypeDeclarationStatement
		if (type == 60) return true; // VariableDeclarationStatement
		return withBlockStatement(type);
	}
	
	public static boolean isStatement2(int type) {
		if (type == 8)  return true; // block
		if (type == 12) return true; // CatchClause
		return isStatement(type);
	}

	public static boolean isExpressionType(String astNode) {
		if (astNode.equals("ArrayAccess") || astNode.equals("ArrayCreation") ||
				astNode.equals("ArrayInitializer") || astNode.equals("Assignment") || astNode.equals("CastExpression") ||
				astNode.equals("ClassInstanceCreation") || astNode.equals("ConditionalExpression") || astNode.equals("CreationReference") ||
				astNode.equals("ExpressionMethodReference") || astNode.equals("FieldAccess") || astNode.equals("InfixExpression") ||
				astNode.equals("InstanceofExpression") || astNode.equals("LambdaExpression") || astNode.equals("MethodInvocation")  ||
				astNode.equals("MethodReference") || astNode.equals("ParenthesizedExpression") || astNode.equals("PostfixExpression")  ||
				astNode.equals("PrefixExpression") || astNode.equals("SuperFieldAccess") || astNode.equals("SuperMethodInvocation")  ||
				astNode.equals("SuperMethodReference") || astNode.equals("TypeLiteral") || astNode.equals("TypeMethodReference") 
				|| astNode.equals("VariableDeclarationExpression") ) {
			return true;
		}
		return false;
	}
	
	public static boolean isComplexExpression(int type) {
		if (type == 2)  return true; // ArrayAccess
		if (type == 3)  return true; // ArrayCreation
		if (type == 4)  return true; // ArrayInitializer
		if (type == 7)  return true; // Assignment
		if (type == 11) return true; // CastExpression
		if (type == 14) return true; // ClassInstanceCreation
		if (type == 16) return true; // ConditionalExpression
//		if (type == 17) return true; // ConstructorInvocation
		if (type == 22) return true; // FieldAccess
		if (type == 27) return true; // InfixExpression
		if (type == 32) return true; // MethodInvocation
		if (type == 36) return true; // ParenthesizedExpression
		if (type == 37) return true; // PostfixExpression
		if (type == 38) return true; // PrefixExpression
		if (type == 40) return true; // QualifiedName
		if (type == 47) return true; // SuperFieldAccess
		if (type == 48) return true; // SuperMethodInvocation
		if (type == 58) return true; // VariableDeclarationExpression
		if (type == 59) return true; // VariableDeclarationFragment FIXME: this node is not an expression node.
		if (type == 62) return true; // InstanceofExpression
		if (type == 86) return true; // LambdaExpression
		return false;
	}

	public static boolean isValidExpression(int type) {
		if (type == 9)  return true; // BooleanLiteral
		if (type == 13) return true; // CharacterLiteral
		if (type == 33) return true; // NullLiteral
		if (type == 34) return true; // NumberLiteral
		if (type == 45) return true; // StringLiteral
		if (type == 42) return true; // SimpleName
		if (type == 52) return true; // ThisExpression
		return isComplexExpression(type);
	}
	
	public static boolean isTrivalExpression(int type) {
		if (type == 57) return true; // TypeLiteral
		if (type == 77) return true; // NormalAnnotation
		if (type == 78) return true; // MarkerAnnotation
		if (type == 79) return true; // SingleMemberAnnotation
		if (type == 89) return true; // CreationReference
		if (type == 90) return true; // ExpressionMethodReference
		if (type == 91) return true; // SuperMethodReference
		if (type == 92) return true; // TypeMethodReference
		return false;
	}
	
	public static boolean isInstanceofOperator(int type) {
		return type == -3;
	}
	
	public static boolean isNewKeyword(int type) {
		return type == -2;
	}
	
	public static boolean isOperator(int type) {
		return type == -1;
	}
	
	public static boolean isASTNode(int type) {
		return type == 0;
	}
	
	public static boolean isAnonymousClassDeclaration(int type) {
		return type == 1;
	}
	
	public static boolean isArrayAccess(int type) {
		return type == 2;
	}
	
	public static boolean isArrayCreation(int type) {
		return type == 3;
	}
	
	public static boolean isArrayInitializer(int type) {
		return type == 4;
	}
	
	public static boolean isArrayType(int type) {
		return type == 5;
	}
	
	public static boolean isAssertStatement(int type) {
		return type == 6;
	}
	
	public static boolean isAssignment(int type) {
		return type == 7;
	}
	
	public static boolean isBlock(int type) {
		return type == 8;
	}
	
	public static boolean isBooleanLiteral(int type) {
		return type == 9;
	}
	
	public static boolean isBreakStatement(int type) {
		return type == 10;
	}
	
	public static boolean isCastExpression(int type) {
		return type == 11;
	}
	
	public static boolean isCatchClause(int type) {
		return type ==12;
	}
	
	public static boolean isCharacterLiteral(int type) {
		return type == 13;
	}
	
	public static boolean isClassInstanceCreation(int type) {
		return type == 14;
	}
	
	public static boolean isCompilationUnit(int type) {
		return type == 15;
	}
	
	public static boolean isConditionalExpression(int type) {
		return type == 16;
	}
	
	public static boolean isConstructorInvocation(int type) {
		return type == 17;
	}
	
	public static boolean isContinueStatement(int type) {
		return type == 18;
	}
	
	public static boolean isDoStatement(int type) {
		return type == 19;
	}
	
	public static boolean isEmptyStatement(int type) {
		return type == 20;
	}
	
	public static boolean isExpressionStatement(int type) {
		return type == 21;
	}

	public static boolean isFieldAccess(int type) {
		return type == 22;
	}
	
	public static boolean isFieldDeclaration(int type) {
		return type == 23;
	}
	
	public static boolean isForStatement(int type) {
		return type == 24;
	}
	
	public static boolean isIfStatement(int type) {
		return type == 25;
	}
	
	public static boolean isImportDeclaration(int type) {
		return type == 26;
	}
	
	public static boolean isInfixExpression(int type) {
		return type == 27;
	}
	
	public static boolean isInitializer(int type) {
		return type == 28;
	}
	
	public static boolean isJavaDoc(int type) {
		return type == 29;
	}
	
	public static boolean isLabeledStatement(int type) {
		return type == 30;
	}
	
	public static boolean isMethodDeclaration(int type) {
		return type == 31;
	}
	
	public static boolean isMethodInvocation(int type) {
		return type == 32;
	}
	
	public static boolean isNullLiteral(int type) {
		return type == 33;
	}
	
	public static boolean isNumberLiteral(int type) {
		return type == 34;
	}
	
	public static boolean isPackageDeclaration(int type) {
		return type == 35;
	}
	
	public static boolean isParenthesizedExpression(int type) {
		return type == 36;
	}
	
	public static boolean isPostfixExpression(int type) {
		return type == 37;
	}
	
	public static boolean isPrefixExpression(int type) {
		return type == 38;
	}
	
	public static boolean isPrimitiveType(int type) {
		return type == 39;
	}
	
	public static boolean isQualifiedName(int type) {
		return type == 40;
	}
	
	public static boolean isReturnStatement(int type) {
		return type == 41;
	}
	
	public static boolean isSimpleName(int type) {
		return type == 42;
	}
	
	public static boolean isSimpleType(int type) {
		return type == 43;
	}
	
	public static boolean isSingleVariableDeclaration(int type) {
		return type == 44;
	}
	
	public static boolean isStringLiteral(int type) {
		return type == 45;
	}
	
	public static boolean isSuperConstructorInvocation(int type) {
		return type == 46;
	}
	
	public static boolean isSuperFieldAccess(int type) {
		return type == 47;
	}
	
	public static boolean isSuperMethodInvocation(int type) {
		return type == 48;
	}
	
	public static boolean isSwitchCase(int type) {
		return type == 49;
	}
	
	public static boolean isSwitchStatement(int type) {
		return type == 50;
	}
	
	public static boolean isSynchronizedStatement(int type) {
		return type == 51;
	}
	
	public static boolean isThisExpression(int type) {
		return type == 52;
	}
	
	public static boolean isThrowStatement(int type) {
		return type == 53;
	}
	
	public static boolean isTryStatement(int type) {
		return type == 54;
	}
	
	public static boolean isTypeDeclaration(int type) {
		return type == 55;
	}
	
	public static boolean isTypeDeclarationStatement(int type) {
		return type == 56;
	}
	
	public static boolean isTypeLiteral(int type) {
		return type == 57;
	}
	
	public static boolean isVariableDeclarationExpression(int type) {
		return type == 58;
	}
	
	public static boolean isVariableDeclarationFragment(int type) {
		return type == 59;
	}
	
	public static boolean isVariableDeclarationStatement(int type) {
		return type == 60;
	}
	
	public static boolean isWhileStatement(int type) {
		return type == 61;
	}
	
	public static boolean isInstanceofExpression(int type) {
		return type == 62;
	}
	
	public static boolean isLineComment(int type) {
		return type == 63;
	}
	
	public static boolean isBlockComment(int type) {
		return type == 64;
	}
	
	public static boolean isTagElement(int type) {
		return type == 65;
	}
	
	public static boolean isTextElement(int type) {
		return type == 66;
	}
	
	public static boolean isMemberRef(int type) {
		return type == 67;
	}
	
	public static boolean isMethodRef(int type) {
		return type == 68;
	}
	
	public static boolean isMethodRefParameter(int type) {
		return type == 69;
	}
	
	public static boolean isEnhancedForStatement(int type) {
		return type == 70;
	}
	
	public static boolean isEnumDeclaration(int type) {
		return type == 71;
	}
	
	public static boolean isEnumConstantDeclaration(int type) {
		return type == 72;
	}
	
	public static boolean isTypeParameter(int type) {
		return type == 73;
	}
	
	public static boolean isParameterizedType(int type) {
		return type == 74;
	}
	
	public static boolean isQualifiedType(int type) {
		return type == 75;
	}
	
	public static boolean isWildcardType(int type) {
		return type == 76;
	}
	
	public static boolean isNormalAnnotation(int type) {
		return type == 77;
	}
	
	public static boolean isMarkerAnnotation(int type) {
		return type == 78;
	}
	
	public static boolean isSingleMemberAnnotation(int type) {
		return type == 79;
	}
	
	public static boolean isMemberValuePair(int type) {
		return type == 80;
	}
	
	public static boolean isAnnotationTypeDeclaration(int type) {
		return type == 81;
	}
	
	public static boolean isAnnotationTypeMemberDeclaration(int type) {
		return type == 82;
	}
	
	public static boolean isModifier(int type) {
		return type == 83;
	}
	
	public static boolean isUnionType(int type) {
		return type == 84;
	}
	
	public static boolean isDimension(int type) {
		return type == 85;
	}
	
	public static boolean isLambdaExpression(int type) {
		return type == 86;
	}
	
	public static boolean isIntersectionType(int type) {
		return type == 87;
	}
	
	public static boolean isNameQualifiedType(int type) {
		return type == 88;
	}
	
	public static boolean isCreationReference(int type) {
		return type == 89;
	}
	
	public static boolean isExpressionMethodReference(int type) {
		return type == 90;
	}
	
	public static boolean isSuperMethodReference(int type) {
		return type == 91;
	}
	
	public static boolean isTypeMethodReference(int type) {
		return type == 92;
	}
}
