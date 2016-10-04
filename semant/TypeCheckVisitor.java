package semant;

import visitor.Visitor;
import ast.*;
import util.*;
import java.util.*;

public class TypeCheckVisitor extends Visitor {
	ClassTreeNode ctn, ptn; // class, parent tree nodes
	String fname;
	Hashtable<String, ClassTreeNode> map;
	ErrorHandler err;
	HashSet<String> prim, rsv; // primitive types, reserved words
	SymbolTable vst, mst, tst; // var, method, temp symbol tables
	ArrayDeque<Integer> breakto;

	/**
	 * TypeCheckVisitor constructor
	 * 
	 * @param ctn
	 *            ClassTreeNode to be visited
	 */
	public TypeCheckVisitor(Hashtable<String, ClassTreeNode> classMap,
		ErrorHandler err, HashSet<String> prim, HashSet<String> rsv) {
		this.map = classMap;
		this.err = err;
		this.prim = prim;
		this.rsv = rsv;
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
		ctn = map.get(node.getName());
		ptn = ctn.getParent();
		fname = node.getFilename();
		vst = ctn.getVarSymbolTable();
		mst = ctn.getMethodSymbolTable();
		breakto = new ArrayDeque<Integer>(13);

		node.getMemberList().accept(this);


		return null;
	}

	/**
	 * Visit a list node of members
	 * 
	 * @param node
	 *            the member list node
	 * @return result of the visit
	 * */
	public Object visit(MemberList node) {
		for (Iterator it = node.getIterator(); it.hasNext();)
			((Member) it.next()).accept(this);
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
		int ln = node.getLineNum();
		String name = node.getName();
		String ftype, itype; // field, init types

		ftype = node.getType();
		if (node.getInit() != null) {
			node.getInit().accept(this);
			itype = node.getInit().getExprType();
			if (itype.equals("void")) {
				err.register(err.SEMANT_ERROR, fname, ln,
				"expression type '"+itype+"' of field 's' cannot be void");
			} else if (!ftype.equals(itype) && (prim.contains(ftype)
				|| prim.contains(ftype))) {
				//string sonsidered primitive?
				err.register(err.SEMANT_ERROR, fname, ln, "return type '"
					+ itype + "' is not compatible with declared return type '"
					+ ftype + "' in method '" + name + "'");
			} else {
				// some ref checking. wtf did i mean by this?
			}
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
		vst.enterScope();
		mst.enterScope();

		node.getFormalList().accept(this);
		node.getStmtList().accept(this);

		vst.exitScope();
		mst.exitScope();
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
	   	 String name = node.getName();
	   	 String type = node.getType();
	   	 boolean isArr = false;
	   	 
	   	 if(type.contains("[]")){
	   		 isArr = true;
	   		 type = type.substring(0,type.length()-2);
	   	 }

	   	

	   	 if (rsv.contains(name)) {
	   		
	   		 // err
	   	 } else if (vst.peek(name) != null) {
	   		 
	   		 // err
	   	 } else {
	   		 if (!(prim.contains(type) || map.containsKey(type))) {
	   			
	   			 type = "Object";
	   			 // err
	   		 }
	   		 if(isArr){
	   			 type = type + "[]";
	   		 }
	   		 
	   		 vst.add(name, type);
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
		for (Iterator it = node.getIterator(); it.hasNext();) {
			((Stmt) it.next()).accept(this);
		}
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
		String name = node.getName();
		String type = node.getType();

		node.getInit().accept(this);

		

		if (rsv.contains(name)) {
			// err
		} else if (vst.peek(name) != null) {
			// err
		} else {
			if (!(prim.contains(type) || map.containsKey(type))) {
				// err
				type = "Object";
			} else if (!type.equals(node.getInit().getExprType())) {
				// err, keep going? (prim types not children of obj)
				type = "Object";
			}
			vst.add(node.getName(), node.getType());
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
		String exp = node.getExpr().getClass().toString();

		switch (exp) {
			case "AssignExpr":
			case "ArrayAssignExpr":
			case "NewExpr":
			case "DispatchExpr":
			case "UnaryIncExpr":
			case "UnaryDecExpr":
				break;
			default:
				// err
		}
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
		if (!node.getPredExpr().getExprType().equals("boolean")) {
			// err
		}
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
		breakto.push(vst.getCurrScopeLevel());
		node.getPredExpr().accept(this);
		if (!node.getPredExpr().getExprType().equals("boolean")) {
			// err
		}
		node.getBodyStmt().accept(this);
		breakto.pop();
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
		breakto.push(vst.getCurrScopeLevel());
		if (node.getInitExpr() != null) {
			node.getInitExpr().accept(this);
		}
		// loop++;
		if (node.getPredExpr() != null) {
			node.getPredExpr().accept(this);
			if (!node.getPredExpr().getExprType().equals("boolean")) {
				// err
			}
		}
		if (node.getUpdateExpr() != null) {
			node.getUpdateExpr().accept(this);
		}
		node.getBodyStmt().accept(this);
		breakto.pop();
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
		if (breakto.isEmpty()) {
			// err
		}
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
		vst.enterScope();
		mst.enterScope();

		node.getStmtList().accept(this);

		vst.exitScope();
		mst.exitScope();
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
		String type = "void";
		if (node.getExpr() != null) {
			node.getExpr().accept(this);
			type = node.getExpr().getExprType();
			if (type.equals("void")) {
				// err
				type = "Object";
			}
		}
		return type;
	}

	/**
	 * Visit a list node of expressions
	 * 
	 * @param node
	 *            the expression list node
	 * @return result of the visit
	 * */
	public Object visit(ExprList node) {
		for (Iterator it = node.getIterator(); it.hasNext();) {
			((Expr) it.next()).accept(this);
		}
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
	   	 String refType, name = node.getMethodName();
	   	 ExprList alist = node.getActualList();
	   	 FormalList flist = null;
	   	 Iterator<ASTNode> ait, fit;

	   			 
	   	 node.getRefExpr().accept(this);
	   	 refType = node.getRefExpr().getExprType();
	   			 
	   	 if (refType.contains("[]")) {
	   				 
	   		 refType = "Object"; // array types inherit from Object   		 
	   	 }
	   	 if (map.containsKey(refType)) {
	   				
	   		 tst = map.get(refType).getMethodSymbolTable();
	   				 
	   		 node.setExprType(((Method) tst.lookup(name)).getReturnType());
	   				 
	   	 } else {
	   				
	   		 // err?
	   	 }
	   			 
	   	 if (refType.equals(ctn.getName())) {
	   				 
	   		 flist = ((Method) mst.lookup(name)).getFormalList();
	   	 } else if (refType.equals(ptn.getName())) {
	   				 
	   		 tst = ptn.getMethodSymbolTable();
	   		 flist = ((Method) tst.peek(name)).getFormalList();
	   	 } else if (prim.contains(refType)) {
	   				
	   		 //err
	   	 } else {
	   				 
	   		 tst = map.get(refType).getMethodSymbolTable();
	   				
	   		 flist = ((Method) tst.lookup(name)).getFormalList();
	   				 
	   	 }

	   	 if (flist == null) {
	   		 // prior error
	   				
	   	 } else if (flist.getSize() != alist.getSize()) {
	   		 // err
	   	 } else {
	   			
	   		 fit = flist.getIterator();
	   		 ait = alist.getIterator();
	   				
	   		 for (Expr e; ait.hasNext();) {
	   					 
	   			 e = ((Expr) ait.next());
	   			 e.accept(this);
	   					 
	   			 if (!e.getExprType().equals(((Formal) fit.next()).getType())
	   				 || e.getExprType().equals("void")) {
	   				 // err
	   			 }
	   		 }
	   	 }
	   			

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
		String type = node.getType();

		

		if (!map.containsKey(type)) {
			type = "Object";
			// err
		}
		node.setExprType(type);
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
		String type = node.getType();

		

		if (!map.containsKey(type)) {
			type = "Object";
			// err
		}

		node.getSize().accept(this);
		if (!node.getSize().getExprType().equals("int")) {
			// err
		}

		

		node.setExprType(node.getType() + "[]");
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
		String lhs, rhs = node.getType();

		if (rhs.endsWith("[]")
			&& !(map.contains(rhs.substring(0, rhs.length() - 3))
				|| prim.contains(rhs.substring(0, rhs.length() - 3)))) {
			rhs = "Object[]";
			// arrerrarr
		} else if (!map.contains(rhs)) {
			rhs = "Object";
			// err
		}

		node.getExpr().accept(this);
		lhs = node.getExpr().getExprType();

		if (lhs.equals("void")) {
			// err
		} else if (prim.contains(lhs)) {
			// err
		} else {
			node.setExprType("boolean");
			return "boolean";
		}
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
	   	 boolean up, down, farr, tarr, gogogo;
	   	 String from, to = node.getType(), ftmp = "", ttmp = "";
	   	 ClassTreeNode tmp;
	   	 up = down = farr = tarr = gogogo = false;

	   			 
	   	 if (prim.contains(to)) {
	   		 to = "Object";
	   		 // err
	   			
	   	 } else if (to.contains("[]")) {
	   		 tarr = true;
	   		 ttmp = to;
	   		 to = to.substring(0, to.length() - 2);
	   				   		 
	   	 }
	   	 if (!map.containsKey(to)) {
	   		 to = "Object";
	   		 if(!tarr) {
	   			 //err
	   					 
	   		 }
	   	 }

	   	 node.getExpr().accept(this);
	   	 from = node.getExpr().getExprType();
	   			

	   	 if (prim.contains(from)) {
	   				
	   		 // err
	   	 } else if (from.contains("[]")) {
	   		 farr = true;
	   		 ftmp = from;
	   		 from = from.substring(0, from.length() - 2);
	   				  		 
	   	 }
	   	 if (!map.containsKey(from)) {
	   		 if(!farr) {
	   					
	   			 // err, should be picked up by expr though?
	   		 } else {
	   			 gogogo = true;
	   			 from = "Object";
	   		 }
	   	} else {
	   		 gogogo = true;
	   	 }
	   	 if (gogogo) {
	   		
	   		 for (tmp = map.get(from); tmp != null && !tmp.getName().equals(to);
	   			 tmp = tmp.getParent()) {
	   				
	   		 }
	   		 // if from is child of to, upcast
	   		 up = tmp == null ? false : true;
	   				
	   		 for (tmp = map.get(to); tmp != null && !tmp.getName().equals(from);
	   			 tmp = tmp.getParent()) {
	   					 
	   		 }
	   		 // if to is child of from, downcast :(
	   		 down = tmp == null ? false : true;   				 
	   				 

	   		 to = tarr ? ttmp : to;
	   		 from = farr ? ftmp : from;

	   		 if (up || down) {
	   				
	   			 node.setExprType(to);
	   			 node.setUpCast(up);
	   		 } else {
	   			 // err
	   					
	   		 }
	   	 }
	   	
	   	 return to;
	}


	/**
	 * Visit an assignment expression node
	 * 
	 * @param node
	 *            the assignment expression node
	 * @return result of the visit
	 * */
	public Object visit(AssignExpr node) {
		String lhs, rhs, ref = node.getRefName(), name = node.getName();

		

		node.getExpr().accept(this);
		rhs = node.getExpr().getExprType();

		if (ref != null) {
			if (ref.equals("this")) {
				lhs = (String) vst.lookup("this." + name);

				

			} else if (ref.equals("super")) {
				lhs = (String) ptn.getVarSymbolTable().peek("this." + name);

				

			} else {
				lhs = "";
				// err
			}
		} else {
			lhs = (String) vst.lookup(name);
		}

		if (rhs.equals("void")) {
			// err
		} else if (rhs.equals(lhs)) {
			// err
		}

		node.setExprType(rhs); // should be in else?
		return rhs;
	}

	/**
	 * Visit an array assignment expression node
	 * 
	 * @param node
	 *            the array assignment expression node
	 * @return result of the visit
	 * */
	public Object visit(ArrayAssignExpr node) {
		String name = node.getName();
		String lhs, rhs, ref = node.getRefName();

		

		node.getIndex().accept(this);
		node.getExpr().accept(this);
		rhs = node.getExpr().getExprType();

		if (ref != null) {
			if (ref.equals("this")) {
				lhs = (String) vst.lookup("this." + name);

			
			} else if (ref.equals("super")) {
				lhs = (String) ptn.getVarSymbolTable().peek(name);

				
			} else {
				lhs = "";
				// err
			}
		} else {
			lhs = (String) vst.lookup(name);
		}

		if (rhs.equals("void")) {
			// err
		} else if (rhs.equals(lhs)) {
			// err
		} else if (!node.getIndex().getExprType().equals("int")) {
			// err
		}
		node.setExprType(rhs); // should be in else?
		return rhs;
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

		node.setExprType("boolean");
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

		node.setExprType("boolean");
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

		node.setExprType("boolean");
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

		node.setExprType("boolean");
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

		node.setExprType("boolean");
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

		node.setExprType("boolean");
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
		String lhs, rhs;

		node.getLeftExpr().accept(this);
		node.getRightExpr().accept(this);

		lhs = node.getLeftExpr().getExprType();
		rhs = node.getRightExpr().getExprType();
		if (lhs.equals(rhs)) {
			node.setExprType(lhs);
		}
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
		String lhs, rhs;

		node.getLeftExpr().accept(this);
		node.getRightExpr().accept(this);

		lhs = node.getLeftExpr().getExprType();
		rhs = node.getRightExpr().getExprType();
		if (lhs.equals(rhs)) {
			node.setExprType(lhs);
		}
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
		String lhs, rhs;

		node.getLeftExpr().accept(this);
		node.getRightExpr().accept(this);

		lhs = node.getLeftExpr().getExprType();
		rhs = node.getRightExpr().getExprType();

		if (lhs.equals(rhs)) {
			node.setExprType(lhs);
		}
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
		String lhs, rhs;

		node.getLeftExpr().accept(this);
		node.getRightExpr().accept(this);

		lhs = node.getLeftExpr().getExprType();
		rhs = node.getRightExpr().getExprType();
		if (lhs.equals(rhs)) {
			node.setExprType(lhs);
		}
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
		String lhs, rhs;

		node.getLeftExpr().accept(this);
		node.getRightExpr().accept(this);

		lhs = node.getLeftExpr().getExprType();
		rhs = node.getRightExpr().getExprType();
		if (lhs.equals(rhs)) {
			node.setExprType(lhs);
		}
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

		node.setExprType("boolean");
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

		node.setExprType("boolean");
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

	   	 if (!node.getExpr().getExprType().equals("int")) {
	   		 err.register(err.SEMANT_ERROR, fname, node.getLineNum(),
	   			 "unaryNeg must be int, fool");
	   	 } else {
	   		 node.setExprType(node.getExpr().getExprType());
	   	 }
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

	   	 

	   	 if (!node.getExpr().getExprType().equals("boolean")) {
	   		 err.register(err.SEMANT_ERROR, fname, node.getLineNum(),
	   			 "\tunaryNot must be boolean, fool");

	   		
	   	 } else {

	   		

	   		 node.setExprType(node.getExpr().getExprType());
	   		 
	   		    
	   	 }
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

	   	 if (!(node.getExpr() instanceof VarExpr
	   		 || node.getExpr() instanceof ArrayExpr)) {
	   		 err.register(err.SEMANT_ERROR, fname, node.getLineNum(),
	   			 "++ should prolly be used on var or arr");
	   		 
	   	 } else if (!node.getExpr().getExprType().equals("int")) {
	   		 err.register(err.SEMANT_ERROR, fname, node.getLineNum(),
	   			 "++ needs int, fool");
	   	 } else {
	   		 node.setExprType(node.getExpr().getExprType());
	   	 }   	 return null;
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

	   	 if (!(node.getExpr() instanceof VarExpr
	   		 || node.getExpr() instanceof ArrayExpr)) {
	   		 err.register(err.SEMANT_ERROR, fname, node.getLineNum(),
	   			 "-- should prolly be used on var or arr");
	   	 } else if (!node.getExpr().getExprType().equals("int")) {
	   		 err.register(err.SEMANT_ERROR, fname, node.getLineNum(),
	   			 "-- needs int, fool");
	   	 } else {
	   		 node.setExprType(node.getExpr().getExprType());
	   	 }
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
		int ln = node.getLineNum();
		String refType, refName, type, name = node.getName();
		type = null;

		

		if (node.getRef() != null) {
			node.getRef().accept(this);
			refName = (node.getRef() instanceof VarExpr) ? 
					((VarExpr) node.getRef()).getName() : node.getName();
			refType = node.getRef().getExprType();

			if (refType.contains("[]")) {
				if (name.equals("length")){ // array.length
					type = "int";
				} else {
					err.register(err.SEMANT_ERROR, fname, ln,
						"array ref type may only access .length");
				}
			} else if (refName.equals("super")) {
				//cast to string
				type = (String) ptn.getVarSymbolTable().peek("this." + name);
			} else if (refName.equals("this")) {
				type = (String) vst.lookup("this." + name);
			} else {
				err.register(err.SEMANT_ERROR, fname, ln,
						"varExpr: invalid refExpr");
			}

			

		} else if (name.equals("super")) {
			

			type = ptn.getName();
		} else if (name.equals("this")) {
			

			type = ctn.getName();
		} else if (name.equals("null")) {
			

			type = "null";
		} else {
			

			type = vst.lookup(name).toString();
		}

		if(type == null) {
			

			type = "Object";
			//err?
		}

		

		node.setExprType(type);
		return type;
	}

	/**
	 * Visit an array expression node
	 * 
	 * @param node
	 *            the array expression node
	 * @return result of the visit
	 * */
	public Object visit(ArrayExpr node) {
	   	 int ln = node.getLineNum();
	   	 String refType, refName, type = null, name = node.getName();

	  

	   	 if (node.getRef() != null) {
	   		 node.getRef().accept(this);
	   		 refName = ((VarExpr) node.getRef()).getName();
	   		 refType = node.getRef().getExprType();

	   		 if (refType.contains("[]")) { // not possible?
	   			 if (name.equals("length")){ // array.length
	   				 type = "int";
	   			 } else {
	   				 err.register(err.SEMANT_ERROR, fname, ln,
	   					 "array ref type may only access .length");
	   			 }
	   		 } else if (refName.equals("super")) {
	   			 type = (String) ptn.getVarSymbolTable().peek("this." + name);
	   		 } else if (refName.equals("this")) {
	   			 type = (String) vst.lookup("this." + name);
	   		 } else {
	   			 err.register(err.SEMANT_ERROR, fname, ln,
	   				 "varExpr: invalid refExpr");
	   		 }

	   		

	   	 } else if (name.equals("super")) {
	   		

	   		 type = ptn.getName();
	   	 } else if (name.equals("this")) {
	   		

	   		 type = ctn.getName();
	   	 } else if (name.equals("null")) {
	   		

	   		 type = "null";
	   	 } else {
	   		 type = vst.lookup(name).toString();

	   		
	   	 }

	   	 if(type == null) {
	   		

	   		 type = "Object";
	   		 //err?
	   	 }

	   	

	   	 node.getIndex().accept(this);
	   	 if (!node.getIndex().getExprType().equals("int")) {
	   		 err.register(err.SEMANT_ERROR, fname, ln,
	   			 "varExpr: index must be int " + node.getIndex().getExprType());
	   	 }

	   	 type = type.contains("[]")? type.substring(0, type.length()-2) : type;
	   	 node.setExprType(type);

	   	

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
		

		node.setExprType("int");
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
		

		node.setExprType("boolean");
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
		

		node.setExprType("String");
		return null;
	}
}