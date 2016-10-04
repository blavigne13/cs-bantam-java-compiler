package codegenjvm;

import java.io.*;
import java.util.Iterator;

import util.*;
import ast.*;

public class JVMCodeGenerator {
    private ClassTreeNode root;
    private boolean debug;

    public JVMCodeGenerator(ClassTreeNode root, boolean debug) throws FileNotFoundException {	
	this.root = root;
	this.debug = debug;
    }

    public void generate() throws FileNotFoundException {
	generate_r(root);
    }

    public void generate_r(ClassTreeNode ctn) throws FileNotFoundException {
	if (!ctn.isBuiltIn()) {
	    System.out.println(ctn.getName()+" != built-in");
	    
	    PrintWriter pw = new PrintWriter(new File(ctn.getName() + ".j"));
	    
	    CodeGenVisitor visitor = new CodeGenVisitor(ctn, pw);
	    visitor.visit(ctn.getASTNode());
	}
	
	Iterator<ClassTreeNode> it = ctn.getChildrenList();
	
	System.out.println(ctn.getName()+" hasNext: "+ it.hasNext());
	
	while (it.hasNext())
	    generate_r(it.next());
    }
}
