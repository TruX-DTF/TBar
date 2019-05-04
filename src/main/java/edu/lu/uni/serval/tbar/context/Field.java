package edu.lu.uni.serval.tbar.context;

public class Field {
	
	private String packageName;
	private String className;
	private ModifierType modifier;
	private String dataType;
	private String varName;
	private boolean isStatic = false;
	
	public Field(String className, ModifierType modifier, String dataType, String varName,
			boolean isStatic) {
//		this.packageName = packageName;
		this.className = className;
		this.modifier = modifier;
		this.dataType = dataType;
		this.varName = varName;
		this.isStatic = isStatic;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}

	public ModifierType getModifier() {
		return modifier;
	}

	public String getDataType() {
		return dataType;
	}

	public String getVarName() {
		return varName;
	}

	public boolean isStatic() {
		return isStatic;
	}

}
