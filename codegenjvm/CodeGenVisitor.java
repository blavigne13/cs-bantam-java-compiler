package codegenjvm;

import ast.*;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import util.ClassTreeNode;
import util.SymbolTable;
import visitor.Visitor;

public class CodeGenVisitor extends Visitor {
    PrintWriter j;
    ClassTreeNode ctn;
    String path, sig, classname;
    SymbolTable mst, vst;
    LinkedHashMap<String, Integer> locals;

    CodeGenVisitor(ClassTreeNode ctn, PrintWriter j) {
	System.out.println("new visitor");
	this.ctn = ctn;
	this.j = j;
	this.locals = new LinkedHashMap<String, Integer>();
	path = sig = "";
    }

    /**
     * Visit a program node
     * 
     * @param node
     *            the program node
     * @return result of the visit
     * */
    public Object visit(Program node) {
	node.getClassList().accept(this);
	return null;
    }

    /**
     * Visit a list node of classes
     * 
     * @param node
     *            the class list node
     * @return result of the visit
     * */
    public Object visit(ClassList node) {
	for (Iterator it = node.getIterator(); it.hasNext();)
	    ((Class_) it.next()).accept(this);
	return null;
    }

    /**
     * Visit a class node
     * 
     * @param node
     *            the class node
     * @return result of the visit
     * */
    public Object visit(Class_ node) {
	System.out.println(" visitor: " + ctn.getName());
	classname = node.getName();
	ClassTreeNode ptn = ctn.getParent();

	System.out.println(" parent: " + ptn.getName());

	j.println(".source " + node.getFilename());
	j.println(".class public " + classname);

	System.out.println(j.checkError());

	while (ptn != null) {
	    path = "/" + ptn.getName() + path;
	    ptn = ptn.getParent();
	}

	path = "java/lang" + path;

	j.println(".super " + path);
	j.println(".implements java/lang/Cloneable");
	j.println();
	// default constructor
	j.println(".method public <init>()V");
	j.println(".throws java/lang/CloneNotSupportedException");
	j.println("\t.limit stack 2");
	j.println("\t.limit locals 1");
	j.println("\taload_0");
	// j.println("\tdup");
	j.println("\tinvokespecial " + path + "/<init>()V");
	j.println("\treturn");
	j.println(".end method");
	j.println();

	System.out.println(j.checkError());
	mst = ctn.getMethodSymbolTable();
	vst = ctn.getVarSymbolTable();
	vst.enterScope();
	node.getMemberList().accept(this);

	j.flush();
	return null;
    }

    /**
     * Visit a field node
     * 
     * @param node
     *            the field node
     * @return result of the visit
     * */
    public Object visit(Field node) {
	System.out.println("field: " + node.getName());
	String t = node.getType();
	sig = classname + "/" + t + " ";
	switch (t) {
	case "int":
	    sig += "I";
	    break;
	case "boolean":
	    sig += "Z";
	    break;
	case "Object":
	case "String":
	    sig += "Ljava/lang/" + t + ";";
	    break;
	default:
	    sig += "L" + t + ";";
	}
	j.println(".field protected " + sig);

	if (node.getInit() != null) {
	    j.println("\taload_0");
	    j.println("\tputstatic " + sig);
	    node.getInit().accept(this);

	}
	return null;
    }

    /**
     * Visit a method node
     * 
     * @param node
     *            the method node
     * @return result of the visit
     * */
    public Object visit(Method node) {
	System.out.println("method: " + node.getName());
	String t = node.getReturnType();
	sig = "";

	if (node.getName().equals("main")) {
	    System.out.println("main: " + node.getName());
	    // bantam main
	    j.println(".method static public main([Ljava/lang/String;)V");
	    j.println(".throws java/lang/CloneNotSupportedException");
	    j.println("\t.limit stack 100");
	    j.println("\t.limit locals 100");
	    j.println("\tnew Main");
	    j.println("\tdup");
	    j.println("\tinvokespecial Main/<init>()V");
	    j.println("\tinvokevirtual Main/original_main()V");
	    j.println("\treturn");
	    j.println(".end method");
	    j.println();
	    sig += ".method public original_main(";

	    System.out.println("-main: " + node.getName());
	} else {
	    System.out.println("not main: " + node.getName());

	    sig += ".method public " + node.getName() + "(";

	}
	vst.enterScope();
	locals.put("this", locals.size());

	System.out.println("formals: ");
	node.getFormalList().accept(this);
	System.out.println("/t-end formals: ");

	sig += ")";
	switch (t) {
	case "void":
	    sig += "V";
	    break;
	case "int":
	    sig += "I";
	    break;
	case "boolean":
	    sig += "Z";
	    break;
	case "Object":
	case "String":
	    sig += "Ljava/lang/" + t + ";";
	    break;
	case "TextIO":
	case "Sys":
	    sig += "Ljava/lib/" + t + ";";
	    break;
	default:
	    sig += "L" + classname + "/" + t + ";";
	}
	j.println(sig);
	j.println(".throws java/lang/CloneNotSupportedException");
	j.println("\t.limit stack 100");
	j.println("\t.limit locals 100");

	node.getStmtList().accept(this);

	// j.println("\treturn");

	j.println(".end method");
	j.println();

	return null;
    }

    /**
     * Visit a list node of formals
     * 
     * @param node
     *            the formal list node
     * @return result of the visit
     * */
    public Object visit(FormalList node) {
	for (Iterator it = node.getIterator(); it.hasNext();)
	    ((Formal) it.next()).accept(this);
	return null;
    }

    /**
     * Visit a formal node
     * 
     * @param node
     *            the formal node
     * @return result of the visit
     * */
    public Object visit(Formal node) {
	System.out.println("\tformal: " + node.getName());

	locals.put(node.getName(), locals.size());

	String t = node.getType();

	if (t.indexOf("[]") > -1) {
	    sig += "[";
	    t.replaceFirst("[]", "");
	}

	switch (t) {
	case "int":
	    sig += "I";
	    break;
	case "boolean":
	    sig += "Z";
	    break;
	case "Object":
	case "String":
	    sig += "Ljava/lang/" + t + ";";
	    break;
	default:
	    sig += "L" + t + ";";
	}
	return null;
    }

    /**
     * Visit a list node of statements
     * 
     * @param node
     *            the statement list node
     * @return result of the visit
     * */
    public Object visit(StmtList node) {
	for (Iterator it = node.getIterator(); it.hasNext();)
	    ((Stmt) it.next()).accept(this);
	return null;
    }

    /**
     * Visit a declaration statement node
     * 
     * @param node
     *            the declaration statement node
     * @return result of the visit
     * */
    public Object visit(DeclStmt node) {
	System.out.println("decl: " + node.getName());

	String t = node.getType();
	locals.put(node.getName(), locals.size());

	node.getInit().accept(this);

	int a = locals.get(node.getName());

	if (a < 4) {
	    switch (t) {
	    case "int":
	    case "boolean":
		j.println("\tistore_" + locals.get(node.getName()));
		break;
	    default:
		j.println("\tastore_" + locals.get(node.getName()));
	    }
	} else {
	    switch (t) {
	    case "int":
	    case "boolean":
		j.println("\tistore " + locals.get(node.getName()));
		break;
	    default:
		j.println("\tastore " + locals.get(node.getName()));
	    }
	}
	return null;
    }

    /**
     * Visit an expression statement node
     * 
     * @param node
     *            the expression statement node
     * @return result of the visit
     * */
    public Object visit(ExprStmt node) {
	node.getExpr().accept(this);
	return null;
    }

    /**
     * Visit an if statement node
     * 
     * @param node
     *            the if statement node
     * @return result of the visit
     * */
    public Object visit(IfStmt node) {
	node.getPredExpr().accept(this);
	node.getThenStmt().accept(this);
	node.getElseStmt().accept(this);
	return null;
    }

    /**
     * Visit a while statement node
     * 
     * @param node
     *            the while statement node
     * @return result of the visit
     * */
    public Object visit(WhileStmt node) {
	node.getPredExpr().accept(this);
	node.getBodyStmt().accept(this);
	return null;
    }

    /**
     * Visit a for statement node
     * 
     * @param node
     *            the for statement node
     * @return result of the visit
     * */
    public Object visit(ForStmt node) {
	if (node.getInitExpr() != null)
	    node.getInitExpr().accept(this);
	if (node.getPredExpr() != null)
	    node.getPredExpr().accept(this);
	if (node.getUpdateExpr() != null)
	    node.getUpdateExpr().accept(this);
	node.getBodyStmt().accept(this);
	return null;
    }

    /**
     * Visit a break statement node
     * 
     * @param node
     *            the break statement node
     * @return result of the visit
     * */
    public Object visit(BreakStmt node) {
	return null;
    }

    /**
     * Visit a block statement node
     * 
     * @param node
     *            the block statement node
     * @return result of the visit
     * */
    public Object visit(BlockStmt node) {
	node.getStmtList().accept(this);
	return null;
    }

    /**
     * Visit a return statement node
     * 
     * @param node
     *            the return statement node
     * @return result of the visit
     * */
    public Object visit(ReturnStmt node) {
	System.out.println("ret");
	if (node.getExpr() != null) {
	    node.getExpr().accept(this);

	    String t = node.getExpr().getExprType();

	    switch (t) {
	    case "int":
	    case "boolean":
		j.println("\tireturn");
		break;
	    default:
		j.println("\tareturn");
	    }
	} else {
	    j.println("\treturn");
	}

	return null;
    }

    /**
     * Visit a list node of expressions
     * 
     * @param node
     *            the expression list node
     * @return result of the visit
     * */
    public Object visit(ExprList node) {
	for (Iterator it = node.getIterator(); it.hasNext();)
	    ((Expr) it.next()).accept(this);
	return null;
    }

    /**
     * Visit a dispatch expression node
     * 
     * @param node
     *            the dispatch expression node
     * @return result of the visit
     * */
    public Object visit(DispatchExpr node) {
	System.out.println("disp: " + node.getMethodName());

	node.getRefExpr().accept(this);
	String t, ref = node.getRefExpr().getExprType();
	System.out.println("ref: " + ref);
	sig = "";
	switch (ref) {
	case "this":
	case "super":
	default:
	    node.getRefExpr();
	}

	node.getActualList().accept(this);
	for (Iterator it = node.getActualList().getIterator(); it.hasNext();) {
	    t = (((Expr) it.next()).getExprType());
	    System.out.println("actual: " + t);
	    if (t.indexOf("[]") > -1) {
		sig += "[";
		t.replaceFirst("[]", "");
	    }

	    switch (t) {
	    case "int":
		sig += "I";
		break;
	    case "boolean":
		sig += "Z";
		break;
	    case "Object":
	    case "String":
		sig += "Ljava/lang/" + t + ";";
		break;
	    default:
		sig += "L" + t + ";";
	    }
	}
	sig += ")";
	t = node.getExprType();
	switch (t) {
	case "void":
	    sig += "V";
	case "int":
	    sig += "I";
	    break;
	case "boolean":
	    sig += "Z";
	    break;
	case "Object":
	case "String":
	    sig += "Ljava/lang/" + t + ";";
	    break;
	default:
	    sig += "L" + t + ";";
	}

	j.println("\tinvokevirtual " + ref + "/" + node.getMethodName() + "("
		+ sig);

	return null;
    }

    /**
     * Visit a new expression node
     * 
     * @param node
     *            the new expression node
     * @return result of the visit
     * */
    public Object visit(NewExpr node) {
	System.out.println("new: " + node.getExprType());
	String t = node.getExprType();

	switch (t) {
	case "String":
	case "Object":
	    t = "java/lang/" + t;
	default:

	}
	j.println("\tnew " + t);
	j.println("\tdup");
	j.println("\tinvokespecial " + t + "/<init>()V");

	return null;
    }

    /**
     * Visit a new array expression node
     * 
     * @param node
     *            the new array expression node
     * @return result of the visit
     * */
    public Object visit(NewArrayExpr node) {
	node.getSize().accept(this);
	return null;
    }

    /**
     * Visit an instanceof expression node
     * 
     * @param node
     *            the instanceof expression node
     * @return result of the visit
     * */
    public Object visit(InstanceofExpr node) {
	node.getExpr().accept(this);
	return null;
    }

    /**
     * Visit a cast expression node
     * 
     * @param node
     *            the cast expression node
     * @return result of the visit
     * */
    public Object visit(CastExpr node) {
	node.getExpr().accept(this);
	return null;
    }

    /**
     * Visit an assignment expression node
     * 
     * @param node
     *            the assignment expression node
     * @return result of the visit
     * */
    public Object visit(AssignExpr node) {
	System.out.println("ass: " + node.getName());

	node.getExpr().accept(this);
	String t = node.getExprType();

	int a = locals.get(node.getName());

	if (a < 4) {
	    switch (t) {
	    case "int":
	    case "boolean":
		j.println("\tistore_" + locals.get(node.getName()));
		break;
	    default:
		j.println("\tastore_" + locals.get(node.getName()));
	    }
	} else {
	    switch (t) {
	    case "int":
	    case "boolean":
		j.println("\tistore " + locals.get(node.getName()));
		break;
	    default:
		j.println("\tastore " + locals.get(node.getName()));
	    }
	}
	return null;
    }

    /**
     * Visit an array assignment expression node
     * 
     * @param node
     *            the array assignment expression node
     * @return result of the visit
     * */
    public Object visit(ArrayAssignExpr node) {
	node.getIndex().accept(this);
	node.getExpr().accept(this);
	return null;
    }

    /**
     * Visit a binary comparison equals expression node
     * 
     * @param node
     *            the binary comparison equals expression node
     * @return result of the visit
     * */
    public Object visit(BinaryCompEqExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);
	return null;
    }

    /**
     * Visit a binary comparison not equals expression node
     * 
     * @param node
     *            the binary comparison not equals expression node
     * @return result of the visit
     * */
    public Object visit(BinaryCompNeExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);
	return null;
    }

    /**
     * Visit a binary comparison less than expression node
     * 
     * @param node
     *            the binary comparison less than expression node
     * @return result of the visit
     * */
    public Object visit(BinaryCompLtExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);
	return null;
    }

    /**
     * Visit a binary comparison less than or equal to expression node
     * 
     * @param node
     *            the binary comparison less than or equal to expression node
     * @return result of the visit
     * */
    public Object visit(BinaryCompLeqExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);
	return null;
    }

    /**
     * Visit a binary comparison greater than expression node
     * 
     * @param node
     *            the binary comparison greater than expression node
     * @return result of the visit
     * */
    public Object visit(BinaryCompGtExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);
	return null;
    }

    /**
     * Visit a binary comparison greater than or equal to expression node
     * 
     * @param node
     *            the binary comparison greater to or equal to expression node
     * @return result of the visit
     * */
    public Object visit(BinaryCompGeqExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);
	return null;
    }

    /**
     * Visit a binary arithmetic plus expression node
     * 
     * @param node
     *            the binary arithmetic plus expression node
     * @return result of the visit
     * */
    public Object visit(BinaryArithPlusExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);

	j.println("\tiadd");
	return null;
    }

    /**
     * Visit a binary arithmetic minus expression node
     * 
     * @param node
     *            the binary arithmetic minus expression node
     * @return result of the visit
     * */
    public Object visit(BinaryArithMinusExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);

	j.println("\tisub");
	return null;
    }

    /**
     * Visit a binary arithmetic times expression node
     * 
     * @param node
     *            the binary arithmetic times expression node
     * @return result of the visit
     * */
    public Object visit(BinaryArithTimesExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);

	j.println("\timul");
	return null;
    }

    /**
     * Visit a binary arithmetic divide expression node
     * 
     * @param node
     *            the binary arithmetic divide expression node
     * @return result of the visit
     * */
    public Object visit(BinaryArithDivideExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);

	j.println("\tidiv");
	return null;
    }

    /**
     * Visit a binary arithmetic modulus expression node
     * 
     * @param node
     *            the binary arithmetic modulus expression node
     * @return result of the visit
     * */
    public Object visit(BinaryArithModulusExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);

	j.println("\tirem");
	return null;
    }

    /**
     * Visit a binary logical AND expression node
     * 
     * @param node
     *            the binary logical AND expression node
     * @return result of the visit
     * */
    public Object visit(BinaryLogicAndExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);

	j.println("\tiand");
	return null;
    }

    /**
     * Visit a binary logical OR expression node
     * 
     * @param node
     *            the binary logical OR expression node
     * @return result of the visit
     * */
    public Object visit(BinaryLogicOrExpr node) {
	node.getLeftExpr().accept(this);
	node.getRightExpr().accept(this);

	j.println("\tior");
	return null;
    }

    /**
     * Visit a unary negation expression node
     * 
     * @param node
     *            the unary negation expression node
     * @return result of the visit
     * */
    public Object visit(UnaryNegExpr node) {
	node.getExpr().accept(this);

	j.println("\tineg");
	return null;
    }

    /**
     * Visit a unary NOT expression node
     * 
     * @param node
     *            the unary NOT expression node
     * @return result of the visit
     * */
    public Object visit(UnaryNotExpr node) {
	node.getExpr().accept(this);
	j.println("\ticonst_m1");
	j.println("\tixor");
	return null;
    }

    /**
     * Visit a unary increment expression node
     * 
     * @param node
     *            the unary increment expression node
     * @return result of the visit
     * */
    public Object visit(UnaryIncrExpr node) {
	node.getExpr().accept(this);
	return null;
    }

    /**
     * Visit a unary decrement expression node
     * 
     * @param node
     *            the unary decrement expression node
     * @return result of the visit
     * */
    public Object visit(UnaryDecrExpr node) {
	node.getExpr().accept(this);
	return null;
    }

    /**
     * Visit a variable expression node
     * 
     * @param node
     *            the variable expression node
     * @return result of the visit
     * */
    public Object visit(VarExpr node) {
	System.out.println("var: " + node.getName());
	if (node.getRef() != null) {
	    node.getRef().accept(this);
	}
	switch (node.getExprType()) {
	case "int":
	case "boolean":
	    j.println("\tiload_" + locals.get(node.getName()));
	    break;
	default:
	    j.println("\taload_" + locals.get(node.getName()));
	}
	return null;
    }

    /**
     * Visit an array expression node
     * 
     * @param node
     *            the array expression node
     * @return result of the visit
     * */
    public Object visit(ArrayExpr node) {
	System.out.println("arr: " + node.getName());
	if (node.getRef() != null)
	    node.getRef().accept(this);
	node.getIndex().accept(this);
	return null;
    }

    /**
     * Visit an int constant expression node
     * 
     * @param node
     *            the int constant expression node
     * @return result of the visit
     * */
    public Object visit(ConstIntExpr node) {
	System.out.println("intc: " + node.getConstant());
	int ic = node.getIntConstant();

	switch (ic) {
	case -1:
	    j.println("iconst_m1");
	    break;
	case 0:
	    j.println("iconst_0");
	    break;
	case 1:
	    j.println("iconst_1");
	    break;
	case 2:
	    j.println("iconst_2");
	    break;
	case 3:
	    j.println("iconst_3");
	    break;
	case 4:
	    j.println("iconst_4");
	    break;
	case 5:
	    j.println("iconst_5");
	    break;
	default:
	    if (ic <= 127 || ic >= -128)
		j.println("\tbipush " + ic);
	    else if (ic <= 32767 || ic >= -32768)
		j.println("\tsipush " + ic);
	    else
		j.println("\tfupush " + ic);
	    break;
	}
	return null;
    }

    /**
     * Visit a boolean constant expression node
     * 
     * @param node
     *            the boolean constant expression node
     * @return result of the visit
     * */
    public Object visit(ConstBooleanExpr node) {
	System.out.println("boolc: " + node.getConstant());
	return null;
    }

    /**
     * Visit a string constant expression node
     * 
     * @param node
     *            the string constant expression node
     * @return result of the visit
     * */
    public Object visit(ConstStringExpr node) {
	System.out.println("strc: " + node.getConstant());

	String s = node.getConstant();
	j.println("\tldc \"" + s + "\"");
	return null;
    }

}
