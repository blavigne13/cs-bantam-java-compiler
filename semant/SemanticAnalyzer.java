/* Bantam Java Compiler and Language Toolset.

   Copyright (C) 2007 by Marc Corliss (corliss@hws.edu) and 
                         E Christopher Lewis (lewis@vmware.com).
   ALL RIGHTS RESERVED.

   The Bantam Java toolset is distributed under the following 
   conditions:

     You may make copies of the toolset for your own use and 
     modify those copies.

     All copies of the toolset must retain the author names and 
     copyright notice.

     You may not sell the toolset or distribute it in 
     conjunction with a commerical product or service without 
     the expressed written consent of the authors.

   THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS 
   OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE 
   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
   PARTICULAR PURPOSE. 
 */

package semant;

import ast.*;
import util.*;
import visitor.*;
import java.util.*;

/**
 * The <tt>SemanticAnalyzer</tt> class performs semantic analysis. In particular
 * this class is able to perform (via the <tt>analyze()</tt> method) the
 * following tests and analyses: (1) legal inheritence hierarchy (all classes
 * have existing parent, no cycles), (2) legal class member declaration, (3)
 * there is a correct Main class and main() method, and (4) each class member is
 * correctly typed.
 * 
 * This class is incomplete and will need to be implemented by the student.
 * */
public class SemanticAnalyzer {
    /** Root of the AST */
    private Program program;

    /** Root of the class hierarchy tree */
    private ClassTreeNode root;

    /** Maps class names to ClassTreeNode objects describing the class */
    private Hashtable<String, ClassTreeNode> classMap = 
    		new Hashtable<String, ClassTreeNode>();

    /** Ordered list of ClassTreeNode objects (breadth first) */
    private Vector<ClassTreeNode> orderedClassList = new Vector<ClassTreeNode>();

    /** Object for error handling */
    private ErrorHandler errorHandler = new ErrorHandler();

    /** Boolean indicating whether debugging is enabled */
    private boolean debug = true;

    /**
     * Maximum number of inherited and non-inherited fields that can be defined
     * for any one class
     */
    private final int MAX_NUM_FIELDS = 1500;

    /**
     * SemanticAnalyzer constructor
     * 
     * @param program
     *            root of the AST
     * @param debug
     *            boolean indicating whether debugging is enabled
     * */
    public SemanticAnalyzer(Program program, boolean debug) {
	this.program = program;
	this.debug = debug;
    }

    /**
     * Analyze the AST checking for semantic errors and annotating the tree Also
     * builds an auxiliary class hierarchy tree
     * 
     * @return root of the class hierarchy tree (needed for code generation)
     * 
     *         Must add code to do the following: 1 - build built-in class nodes
     *         in class hierarchy tree (already done) 2 - build and check the
     *         class hierarchy tree 3 - build the environment for each class
     *         (adding class members only) and check that members are declared
     *         properly 4 - check that the Main class and main method are
     *         declared properly 5 - type check each class member See the lab
     *         manual for more details on each of these steps.
     * */
    public ClassTreeNode analyze() {

	// list of class declarations
	ClassList classList = program.getClassList();

	// PART 1: class tree
	// build and check class hierarchy tree
	buildClassTree(classList);

	// PART 2: class symbol table
	// build class symbol table for members and check that members are
	// declared properly
	buildSymbolTable();

	// PART 3: Main class/main method
	// check that there is a Main class and main method
	checkMain();

	// PART 4: type checking
	// type check each member (fields and methods) of each user-defined
	// class
	typeCheck();

	errorHandler.checkErrors();
	return root;
    }

    /**
     * Add built in classes to the class tree
     * */
    private void updateBuiltins() {
	// create AST node for object
	Class_ astNode = new Class_(-1, "<built-in class>", "Object", null,
		(MemberList) (new MemberList(-1)).addElement(new Method(-1,
			"Object", "clone", new FormalList(-1),
			(StmtList) (new StmtList(-1))
				.addElement(new ReturnStmt(-1, null)))));
	// create a class tree node for object, save in variable root
	root = new ClassTreeNode(astNode, true, true, classMap);
	// add object class tree node to the mapping
	classMap.put("Object", root);

	// note: String, TextIO, and Sys all have fields that are not shown
	// below. Because
	// these classes cannot be extended and fields are protected, they
	// cannot be accessed by
	// other classes, so they do not have to be included in the AST.

	// create AST node for String
	astNode = new Class_(-1, "<built-in class>", "String", "Object",
		(MemberList) (new MemberList(-1))
			.addElement(
				new Method(-1, "int", "length", new FormalList(
					-1), (StmtList) (new StmtList(-1))
					.addElement(new ReturnStmt(-1, null))))
			.addElement(
				new Method(-1, "boolean", "equals",
					(FormalList) (new FormalList(-1))
						.addElement(new Formal(-1,
							"Object", "str")),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "String", "substring",
					(FormalList) (new FormalList(-1))
						.addElement(
							new Formal(-1, "int",
								"beginIndex"))
						.addElement(
							new Formal(-1, "int",
								"endIndex")),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "String", "concat",
					(FormalList) (new FormalList(-1))
						.addElement(new Formal(-1,
							"String", "str")),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null)))));
	// create class tree node for String, add it to the mapping
	classMap.put("String",
		new ClassTreeNode(astNode, true, false, classMap));

	// create AST node for TextIO
	astNode = new Class_(
		-1,
		"<built-in class>",
		"TextIO",
		"Object",
		(MemberList) (new MemberList(-1))
			.addElement(
				new Method(-1, "void", "readStdin",
					new FormalList(-1),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "void", "readFile",
					(FormalList) (new FormalList(-1))
						.addElement(new Formal(-1,
							"String", "readFile")),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "void", "writeStdout",
					new FormalList(-1),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "void", "writeStderr",
					new FormalList(-1),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(
					-1,
					"void",
					"writeFile",
					(FormalList) (new FormalList(-1))
						.addElement(new Formal(-1,
							"String", "writeFile")),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "String", "getString",
					new FormalList(-1),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "int", "getInt", new FormalList(
					-1), (StmtList) (new StmtList(-1))
					.addElement(new ReturnStmt(-1, null))))
			.addElement(
				new Method(-1, "TextIO", "putString",
					(FormalList) (new FormalList(-1))
						.addElement(new Formal(-1,
							"String", "str")),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null))))
			.addElement(
				new Method(-1, "TextIO", "putInt",
					(FormalList) (new FormalList(-1))
						.addElement(new Formal(-1,
							"int", "n")),
					(StmtList) (new StmtList(-1))
						.addElement(new ReturnStmt(-1,
							null)))));
	// create class tree node for TextIO, add it to the mapping
	classMap.put("TextIO",
		new ClassTreeNode(astNode, true, false, classMap));

	// create AST node for Sys
	astNode = new Class_(-1, "<built-in class>", "Sys", "Object",
		(MemberList) (new MemberList(-1)).addElement(new Method(-1,
			"void", "exit", (FormalList) (new FormalList(-1))
				.addElement(new Formal(-1, "int", "status")),
			(StmtList) (new StmtList(-1))
				.addElement(new ReturnStmt(-1, null))))
	/*
	 * MC: Adding time() requires modifying Spim, which I don't want to do
	 * yet (although I do have this working with Spim in a branched
	 * version).
	 */
	/*
	 * .addElement(new Method(-1, "int", "time", new FormalList(-1),
	 * (StmtList)(new StmtList(-1)) .addElement(new ReturnStmt(-1, null))))
	 */
	);
	// create class tree node for Sys, add it to the mapping
	classMap.put("Sys", new ClassTreeNode(astNode, true, false, classMap));
    }

    /*************************************************************************
     * You should not have to modify the code above this point *
     *************************************************************************/

    /**
     * Build class hierarchy tree, checking to make sure it is well-formed
     * Broken up into three parts: (1) build class tree nodes, add nodes to the
     * mapping, and check for duplicate class names; (2) set parent links of the
     * nodes, and check if parent exists; (3) check that there are no cycles in
     * the graph (i.e., that it's a tree)
     * 
     * @param classList
     *            list of AST class nodes
     * */
    private void buildClassTree(ClassList classList) {
	HashSet<String> primitives = new HashSet<String>();
	HashSet<String> reserved = new HashSet<String>();
	specials(primitives, reserved);

	boolean debug = false;
	if (debug)
	    System.out.println("_enter: buildClassTree_");

	updateBuiltins();
	new ClassHeirarchyVisitor(classMap, errorHandler, primitives, reserved)
		.visit(program);

	if (debug) {
	    String inh;
	    System.out.println("_class heirarchy:");
	    for (ClassTreeNode c : classMap.values()) {
		inh = c.getName();
		for (ClassTreeNode ctn = c.getParent(); ctn != null; ctn = ctn
			.getParent())
		    inh = ctn.getName() + "->" + inh;
		System.out.println("\t_" + inh);
	    }
	    System.out.println("_exit: buildClassTree_");
	}
    }

    /**
     * Build symbol table for each class Note: builds symbol table only for
     * class members not for locals. Must be done before any type checking can
     * be done since classes may contain code that refers to members in other
     * classes. Note also: cannot build symbol table for a subclass before its
     * parent class (since child may use symbols in superclass).
     * */
    private void buildSymbolTable() {
	boolean debug = false;
	if (debug)
	    System.out.println("\n_enter buildSymbolTable_");

	HashSet<String> primitives = new HashSet<String>();
	HashSet<String> reserved = new HashSet<String>();
	specials(primitives, reserved);

	new ClassEnvVisitor(classMap, errorHandler, primitives, reserved)
		.visit(program);

	if (debug)
	    System.out.println("_exit buildSymbolTable_");
    }

    /**
     * Check that Main class and main() method are defined correctly
     * */
    private void checkMain() {
	boolean debug = false;
	if (debug)
	    System.out.println("\n_enter: checkMain_");

	ClassTreeNode ctn = classMap.get("Main");
	Iterator<ASTNode> itAST;
	ASTNode m;

	if (ctn == null) {
	    errorHandler.register(errorHandler.SEMANT_ERROR,
		    "no class 'Main' defined.");
	} else if (ctn.getMethodSymbolTable().peek("main") == null) {
	    errorHandler.register(errorHandler.SEMANT_ERROR, ctn.getASTNode()
		    .getFilename(), ctn.getASTNode().getLineNum(),
		    "no 'main' method defined in the 'Main' class.");
	} else {
	    itAST = ctn.getASTNode().getMemberList().getIterator();
	    while (itAST.hasNext()) {
		m = itAST.next();
		if (m instanceof Method
			&& ((Method) m).getName().equals("main")) {
		    if (((Method) m).getFormalList().getSize() != 0) {
			errorHandler.register(errorHandler.SEMANT_ERROR, ctn
				.getASTNode().getFilename(), m.getLineNum(),
				"'main' method in class 'Main'"
					+ " cannot take arguments");
		    }
		    if (!((Method) m).getReturnType().equals("void")) {
			errorHandler.register(errorHandler.SEMANT_ERROR, ctn
				.getASTNode().getFilename(), m.getLineNum(),
				"'main' method in class 'Main' must be void");
		    }
		    break;
		}
	    }
	}

	if (debug)
	    System.out.println("_exit: checkMain_");
    }

    /**
     * Type check each class member
     * */
    private void typeCheck() {
	HashSet<String> primitives = new HashSet<String>();
	HashSet<String> reserved = new HashSet<String>();
	specials(primitives, reserved);

	boolean debug = false;
	if (debug)
	    System.out.println("\n_enter: typeCheck_");

	new TypeCheckVisitor(classMap, errorHandler, primitives, reserved)
		.visit(program);

	if (debug)
	    System.out.println("_exit: typeCheck_");
    }

    /**
     * rsv.contains(blah) is a lot cleaner than blah.equals("this") ||
     * blah.equals("super") || blah.equals("null") || ...
     * 
     * @param prim
     * @param rsv
     */
    private void specials(HashSet<String> prim, HashSet<String> rsv) {
	prim.add("int");
	prim.add("boolean");

	rsv.add("this");
	rsv.add("super");
	rsv.add("null");
    }
}
