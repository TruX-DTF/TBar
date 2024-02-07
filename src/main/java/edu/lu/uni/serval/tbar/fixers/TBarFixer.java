package edu.lu.uni.serval.tbar.fixers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.faultloc.SuspCodeNode;
import edu.lu.uni.serval.tbar.fixpatterns.CNIdiomNoSuperCall;
import edu.lu.uni.serval.tbar.fixpatterns.ClassCastChecker;
import edu.lu.uni.serval.tbar.fixpatterns.ConditionalExpressionMutator;
import edu.lu.uni.serval.tbar.fixpatterns.DataTypeReplacer;
import edu.lu.uni.serval.tbar.fixpatterns.ICASTIdivCastToDouble;
import edu.lu.uni.serval.tbar.fixpatterns.LiteralExpressionMutator;
import edu.lu.uni.serval.tbar.fixpatterns.MethodInvocationMutator;
import edu.lu.uni.serval.tbar.fixpatterns.NPEqualsShouldHandleNullArgument;
import edu.lu.uni.serval.tbar.fixpatterns.NullPointerChecker;
import edu.lu.uni.serval.tbar.fixpatterns.OperatorMutator;
import edu.lu.uni.serval.tbar.fixpatterns.RangeChecker;
import edu.lu.uni.serval.tbar.fixpatterns.ReturnStatementMutator;
import edu.lu.uni.serval.tbar.fixpatterns.StatementInserter;
import edu.lu.uni.serval.tbar.fixpatterns.StatementMover;
import edu.lu.uni.serval.tbar.fixpatterns.StatementRemover;
import edu.lu.uni.serval.tbar.fixpatterns.VariableReplacer;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.info.Patch;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;

/**
 * 
 * @author kui.liu
 *
 */
@SuppressWarnings("unused")
public class TBarFixer extends AbstractFixer {

	private static Logger log = LoggerFactory.getLogger(TBarFixer.class);
	
	public TBarFixer(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	@Override
	public void fixProcess() {
		// Read paths of the buggy project.
		if (!dp.validPaths) return;
		
		// Read suspicious positions.
		List<SuspiciousPosition> suspiciousCodeList = faultloc.getSuspiciousCodeList();

		if (suspiciousCodeList == null) return;
		
		List<SuspCodeNode> triedSuspNode = new ArrayList<>();
		log.info("=======TBar: Start to fix suspicious code======");
		for (SuspiciousPosition suspiciousCode : suspiciousCodeList) {
			List<SuspCodeNode> scns = faultloc.getSuspiciousCode(suspiciousCode);
			if (scns == null) continue;

			for (SuspCodeNode scn : scns) {
//				log.debug(scn.suspCodeStr);
				if (triedSuspNode.contains(scn)) continue;
				triedSuspNode.add(scn);
				
				// Parse context information of the suspicious code.
				List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
				List<Integer> distinctContextInfo = new ArrayList<>();
				for (Integer contInfo : contextInfoList) {
					if (!distinctContextInfo.contains(contInfo) && !Checker.isBlock(contInfo)) {
						distinctContextInfo.add(contInfo);
					}
				}
//				List<Integer> distinctContextInfo = contextInfoList.stream().distinct().collect(Collectors.toList());
				
		        // Match fix templates for this suspicious code with its context information.
				fixWithMatchedFixTemplates(scn, distinctContextInfo);
		        
				if (!isTestFixPatterns && minErrorTest == 0) break;
				if (this.patchId >= 10000) break;
			}
			if (!isTestFixPatterns && minErrorTest == 0) break;
			if (this.patchId >= 10000) break;
        }
		log.info("=======TBar: Finish off fixing======");
		
		FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + this.dataType + "/" + this.buggyProject);
	}

	public void fixWithMatchedFixTemplates(SuspCodeNode scn, List<Integer> distinctContextInfo) {
		// generate patches with fix templates of TBar.
		FixTemplate ft = null;
		
		if (!Checker.isMethodDeclaration(scn.suspCodeAstNode.getType())) {
			boolean nullChecked = false;
			boolean typeChanged = false;
			boolean methodChanged = false;
			boolean operator = false;
				
			for (Integer contextInfo : distinctContextInfo) {
				if (Checker.isCastExpression(contextInfo)) {
					ft = new ClassCastChecker();
					if (isTestFixPatterns) dataType = readDirectory() + "/ClassCastChecker";
	
					if (!typeChanged) {
						generateAndValidatePatches(ft, scn);
						if (!isTestFixPatterns && this.minErrorTest == 0) return;
						if (this.fixedStatus == 2) {
							fixedStatus = 0;
							return;
						}
						typeChanged = true;
						ft = new DataTypeReplacer();
						if (isTestFixPatterns) dataType = readDirectory() + "/DataTypeReplacer";
					}
				} else if (Checker.isClassInstanceCreation(contextInfo)) {
//					ft = new CNIdiomNoSuperCall();
//					if (isTestFixPatterns) dataType = readDirectory() + "/CNIdiomNoSuperCall";
					if (!methodChanged) {
//						generateAndValidatePatches(ft, scn);
//						if (!isTestFixPatterns && this.minErrorTest == 0) return;
						methodChanged = true;
						ft = new MethodInvocationMutator();
						if (isTestFixPatterns) dataType = readDirectory() + "/MethodInvocationMutator";
					}
				} else if (Checker.isIfStatement(contextInfo) || Checker.isDoStatement(contextInfo) || Checker.isWhileStatement(contextInfo)) {
					if (Checker.isInfixExpression(scn.suspCodeAstNode.getChild(0).getType()) && !operator) {
						operator = true;
						ft = new OperatorMutator(0);
						if (isTestFixPatterns) dataType = readDirectory() + "/OperatorMutator";
						generateAndValidatePatches(ft, scn);
						if (!isTestFixPatterns && this.minErrorTest == 0) return;
						if (this.fixedStatus == 2) {
							fixedStatus = 0;
							return;
						}
					}
					ft = new ConditionalExpressionMutator(2);
					if (isTestFixPatterns) dataType = readDirectory() + "/ConditionalExpressionMutator";
				} else if (Checker.isConditionalExpression(contextInfo)) {
					ft = new ConditionalExpressionMutator(0);
					if (isTestFixPatterns) dataType = readDirectory() + "/ConditionalExpressionMutator";
				} else if (Checker.isCatchClause(contextInfo) || Checker.isVariableDeclarationStatement(contextInfo)) {
					if (!typeChanged) {
						ft = new DataTypeReplacer();
						if (isTestFixPatterns) dataType = readDirectory() + "/DataTypeReplacer";
						typeChanged = true;
					}
				} else if (Checker.isInfixExpression(contextInfo)) {
					ft = new ICASTIdivCastToDouble();
					if (isTestFixPatterns) dataType = readDirectory() + "/ICASTIdivCastToDouble";
					generateAndValidatePatches(ft, scn);
					if (!isTestFixPatterns && this.minErrorTest == 0) return;
					if (this.fixedStatus == 2) {
						fixedStatus = 0;
						return;
					}
					
					if (!operator) {
						operator = true;
						ft = new OperatorMutator(0);
						if (isTestFixPatterns) dataType = readDirectory() + "/OperatorMutator";
						generateAndValidatePatches(ft, scn);
						if (!isTestFixPatterns && this.minErrorTest == 0) return;
						if (this.fixedStatus == 2) {
							fixedStatus = 0;
							return;
						}
					}
					
					ft = new ConditionalExpressionMutator(1);
					if (isTestFixPatterns) dataType = readDirectory() + "/ConditionalExpressionMutator";
					generateAndValidatePatches(ft, scn);
					if (!isTestFixPatterns && this.minErrorTest == 0) return;
					if (this.fixedStatus == 2) {
						fixedStatus = 0;
						return;
					}
					
					ft = new OperatorMutator(4);
					if (isTestFixPatterns) dataType = readDirectory() + "/OperatorMutator";
				} else if (Checker.isBooleanLiteral(contextInfo) || Checker.isNumberLiteral(contextInfo) || Checker.isCharacterLiteral(contextInfo)|| Checker.isStringLiteral(contextInfo)) {
					ft = new LiteralExpressionMutator();
					if (isTestFixPatterns) dataType = readDirectory() + "/LiteralExpressionMutator";
				} else if (Checker.isMethodInvocation(contextInfo) || Checker.isConstructorInvocation(contextInfo) || Checker.isSuperConstructorInvocation(contextInfo)) {
					if (!methodChanged) {
						ft = new MethodInvocationMutator();
						if (isTestFixPatterns) dataType = readDirectory() + "/MethodInvocationMutator";
						methodChanged = true;
					}
					
					if (Checker.isMethodInvocation(contextInfo)) {
						if (ft != null) {
							generateAndValidatePatches(ft, scn);
							if (!isTestFixPatterns && this.minErrorTest == 0) return;
							if (this.fixedStatus == 2) {
								fixedStatus = 0;
								return;
							}
						}
						ft = new NPEqualsShouldHandleNullArgument();
						if (isTestFixPatterns) dataType = readDirectory() + "/NPEqualsShouldHandleNullArgument";
						generateAndValidatePatches(ft, scn);
						if (!isTestFixPatterns && this.minErrorTest == 0) return;
						if (this.fixedStatus == 2) {
							fixedStatus = 0;
							return;
						}
						
						ft = new RangeChecker(false);
						if (isTestFixPatterns) dataType = readDirectory() + "/RangeChecker";
					}
				} else if (Checker.isAssignment(contextInfo)) {
					ft = new OperatorMutator(2);
					if (isTestFixPatterns) dataType = readDirectory() + "/OperatorMutator";
				} else if (Checker.isInstanceofExpression(contextInfo)) {
					ft = new OperatorMutator(5);
					if (isTestFixPatterns) dataType = readDirectory() + "/OperatorMutator";
				} else if (Checker.isArrayAccess(contextInfo)) {
					ft = new RangeChecker(true);
					if (isTestFixPatterns) dataType = readDirectory() + "/RangeChecker";
				} else if (Checker.isReturnStatement(contextInfo)) {
					String returnType = ContextReader.readMethodReturnType(scn.suspCodeAstNode);
					if ("boolean".equalsIgnoreCase(returnType)) {
						ft = new ConditionalExpressionMutator(2);
						if (isTestFixPatterns) dataType = readDirectory() + "/ConditionalExpressionMutator";
					} else {
						ft = new ReturnStatementMutator(returnType);
						if (isTestFixPatterns) dataType = readDirectory() + "/ReturnStatementMutator";
					}
				} else if (Checker.isSimpleName(contextInfo) || Checker.isQualifiedName(contextInfo)) {
					ft = new VariableReplacer();
					if (isTestFixPatterns) dataType = readDirectory() + "/VariableReplacer";
					
					if (!nullChecked) {
						generateAndValidatePatches(ft, scn);
						if (!isTestFixPatterns && this.minErrorTest == 0) return;
						if (this.fixedStatus == 2) {
							fixedStatus = 0;
							return;
						}
						nullChecked = true;
						ft = new NullPointerChecker();
						if (isTestFixPatterns) dataType = readDirectory() + "/NullPointerChecker";
					}
				} 
				if (ft != null) {
					generateAndValidatePatches(ft, scn);
					if (!isTestFixPatterns && this.minErrorTest == 0) return;
					if (this.fixedStatus == 2) {
						fixedStatus = 0;
						return;
					}
				}
				ft = null;
				if (this.patchId >= 10000) break;
			}
			
			if (!nullChecked) {
				nullChecked = true;
				ft = new NullPointerChecker();
				if (isTestFixPatterns) dataType = readDirectory() + "/NullPointerChecker";
				generateAndValidatePatches(ft, scn);
				if (!isTestFixPatterns && this.minErrorTest == 0) return;
				if (this.fixedStatus == 2) {
					fixedStatus = 0;
					return;
				}
			}

			ft = new StatementMover();
			if (isTestFixPatterns) dataType = readDirectory() + "/StatementMover";
			generateAndValidatePatches(ft, scn);
			if (!isTestFixPatterns && this.minErrorTest == 0) return;
			if (this.fixedStatus == 2) {
				fixedStatus = 0;
				return;
			}
			
			ft = new StatementRemover();
			if (isTestFixPatterns) dataType = readDirectory() + "/StatementRemover";
			generateAndValidatePatches(ft, scn);
			if (!isTestFixPatterns && this.minErrorTest == 0) return;
			if (this.fixedStatus == 2) {
				fixedStatus = 0;
				return;
			}
			
			ft = new StatementInserter();
			if (isTestFixPatterns) dataType = readDirectory() + "/StatementInserter";
			generateAndValidatePatches(ft, scn);
			if (!isTestFixPatterns && this.minErrorTest == 0) return;
			if (this.fixedStatus == 2) {
				fixedStatus = 0;
				return;
			}
		} else {
			ft = new StatementRemover();
			if (isTestFixPatterns) dataType = readDirectory() + "/StatementRemover";
			generateAndValidatePatches(ft, scn);
			if (!isTestFixPatterns && this.minErrorTest == 0) return;
			if (this.fixedStatus == 2) {
				fixedStatus = 0;
				return;
			}
		}
	}
	
	private String readDirectory() {
		int index = dataType.indexOf("/");
		if (index > -1) dataType = dataType.substring(0, index);
		return dataType;
	}
	
	protected void generateAndValidatePatches(FixTemplate ft, SuspCodeNode scn) {
		ft.setSuspiciousCodeStr(scn.suspCodeStr);
		ft.setSuspiciousCodeTree(scn.suspCodeAstNode);
		if (scn.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn.javaBackup);
		ft.setDictionary(dic);
		ft.generatePatches();
		List<Patch> patchCandidates = ft.getPatches();
//		System.out.println(dataType + " ====== " + patchCandidates.size());
		
		// Test generated patches.
		if (patchCandidates.isEmpty()) return;
		testGeneratedPatches(patchCandidates, scn);
	}
	
	public List<Integer> readAllNodeTypes(ITree suspCodeAstNode) {
		List<Integer> nodeTypes = new ArrayList<>();
		nodeTypes.add(suspCodeAstNode.getType());
		List<ITree> children = suspCodeAstNode.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isFieldDeclaration(childType) || 
					Checker.isMethodDeclaration(childType) ||
					Checker.isTypeDeclaration(childType) ||
					Checker.isStatement(childType)) break;
			nodeTypes.addAll(readAllNodeTypes(child));
		}
		return nodeTypes;
	}

}
