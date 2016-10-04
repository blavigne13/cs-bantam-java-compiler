package semant;

import util.*;
import visitor.*;
import ast.*;
import java.util.*;

public class ClassHeirarchyVisitor extends Visitor {
	String fname;
	boolean mxbxPwnz;
	Hashtable<String, ClassTreeNode> map;
	HashSet<String> prim, rsv;
	ErrorHandler err;
	/**
	 * ClassEnvVisitor constructor constructs a ClassEnvVisitor which may later
	 * visit the abstraction which represents the class environment to be
	 * constructed by the ClassEnvVisitor constructed by the ClassEnvVisitor
	 * constructor.
	 * 
	 * @param map
	 *            are we lost?
	 * @param err
	 *            ...maybe.
	 */
	public ClassHeirarchyVisitor(Hashtable<String, ClassTreeNode> map,
		ErrorHandler err, HashSet<String> prim, HashSet<String> rsv) {
		this.map = map;
		this.err = err;
		this.prim = prim;
		this.rsv = rsv;
	}

	/**
	 * Visit a list node of classes
	 * 
	 * @param node
	 *            the class list node
	 * @return result of the visit
	 * */
	public Object visit(ClassList node) {
		// add CTN's to map
		mxbxPwnz = false; // IT'S NOT TRUE?!?
		for (Iterator<ASTNode> it = node.getIterator(); it.hasNext();){
			((Class_) it.next()).accept(this);
		}
		// then verify and set inheritence
		mxbxPwnz = true; // YES IT IS!!!
		for (ClassTreeNode ctn : map.values()) {
			prim.add(ctn.getName());
			ctn.getASTNode().accept(this);
		}
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
		String fname = node.getFilename();
		int ln = node.getLineNum();
		String name = node.getName();
		ClassTreeNode ctn, ptn, tmp, root;

		if (!mxbxPwnz) {
			// add CTN's to map
			if (map.containsKey(name)) {
				if (map.get(name).isBuiltIn()) {
					err.register(err.SEMANT_ERROR, fname, ln,
						"built-in class '"+name+"' cannot be redefined");
				} else {
					err.register(err.SEMANT_ERROR, fname, ln,
						"Oops! You suck at names: "+name+" already in use.");
				}
			} else {
				// add valid class name to map
				map.put(name, new ClassTreeNode(node, false, true, map));
			} // end !mxbxPwnz
		} else if (mxbxPwnz) {
			// tree-ification
			root = map.get("Object");
			ctn = map.get(name);
			tmp = node.getParent() == null ? null : map.get(node.getParent());

			if (ctn == root) {
				// ctn == "Object" - 'consume' the p=null
			} else if (tmp == null) {
				err.register(err.SEMANT_ERROR, fname, ln,
					"Oops! You suck at extenz: Parent doesn't exist");
			} else if (tmp == root) {
				// parent == "Object" (must check before built-in)
				ctn.setParent(map.get(node.getParent()));
			} else if (tmp.isBuiltIn()) {
				err.register(err.SEMANT_ERROR, fname, ln,
					"Oops! Can't extend built-in");
			} else if (!tmp.isExtendable()) { // should be caught by built-in
				err.register(err.SEMANT_ERROR, fname, ln,
					"Oops! You suck at extenz: " + tmp.getName()
					+ " is not extendable.");
			} else { // valid parent, test for cycle
				while (tmp != root && tmp != ctn) {
					tmp = map.get(tmp.getASTNode().getParent());
				}
				if (tmp == ctn) {
					err.register(err.SEMANT_ERROR, fname, ln,
						"Oops! You suck at extenz: Cycle detected involving "
						+ ctn.getName());
				} else { // good to go
					ctn.setParent(map.get(node.getParent()));
				}
			}
			//valid class, enter scope
			map.get(name).getVarSymbolTable().enterScope();
			map.get(name).getMethodSymbolTable().enterScope();
		} else {
			System.out.println("If you can read this, I fail.");
			// reading the source code doesn't count.
		}
		return null;
	}
}
