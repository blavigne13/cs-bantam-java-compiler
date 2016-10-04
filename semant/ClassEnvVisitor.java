package semant;

import util.ClassTreeNode;
import util.ErrorHandler;
import util.SymbolTable;
import visitor.Visitor;
import ast.*;

import java.util.*;

public class ClassEnvVisitor extends Visitor {
    String fname;
    Hashtable<String, ClassTreeNode> map;
    HashSet<String> prim, rsv;
    SymbolTable vst, mst, tst;
    ErrorHandler err;

    boolean debug = true;

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
    public ClassEnvVisitor(Hashtable<String, ClassTreeNode> map,
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
        for (ClassTreeNode ctn : map.values()) {
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
        fname = node.getFilename();
        String name = node.getName();

        

        vst = map.get(name).getVarSymbolTable();
        mst = map.get(name).getMethodSymbolTable();

        

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
        for (Iterator<ASTNode> it = node.getIterator(); it.hasNext();){
            ((Member) it.next()).accept(this);
        }
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
        String name = node.getName(), type = node.getType();
        boolean arr = false;

        

        if (type.contains("[]")) {
            arr = true;
            type = type.substring(0, type.length() - 2);

            
        }
        if (rsv.contains(name)) { // this, super, null
            err.register(err.SEMANT_ERROR, fname,
                ln, "fields cannot be named '" + name + "'");
        } else if (vst.peek(name) != null) {
            err.register(err.SEMANT_ERROR, fname, ln, "duplicate");
        } else if (!(prim.contains(type) || map.containsKey(type))) {
            err.register(err.SEMANT_ERROR, fname, ln, "undefined");
        } else { // add to var st
            type += arr ? "[]" : ""; // add braces back for array fields
            vst.add(name, type);
            vst.add("this." + name, type);

            
        }
        if (node.getInit() != null) {
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
        int ln = node.getLineNum();
        String name = node.getName(), type = node.getReturnType();
        boolean arr = false, overload = false;
        Method doppelganger = (Method) mst.lookup(name);
        String ntype, dtype; // node & doppelganger formal types
        Iterator<ASTNode> itn, itd; //node & doppelganger iterators

        

        if (type.contains("[]")) {
            arr = true;
            type = type.substring(0, type.length() - 2);

            
        }

        if (rsv.contains(name)) {
            err.register(err.SEMANT_ERROR, fname, 
                ln, "methods cannot be named '" + name + "'");
        } else if (mst.peek(name) != null) {
            err.register(err.SEMANT_ERROR, fname, ln, "duplicate");
        } else if (!(prim.contains(type) || map.containsKey(type)
            || type.equals("void"))) {
         err.register(err.SEMANT_ERROR, fname, ln, "method: " + name
                + " invalid type: " + type);
        } else {
            type += arr ? "[]" : ""; 

  if (doppelganger != null) { // if method name is inherited
     if (!type.equals(doppelganger.getReturnType())) {
            overload = true;
            err.register(err.SEMANT_ERROR, fname, ln, "method: "
                + name + " ret type: " + type);
     } else if (node.getFormalList().getSize()
       != doppelganger.getFormalList().getSize()) {
         overload = true;
        err.register(err.SEMANT_ERROR, fname, ln, "method: "
                  + name + " formal counts differ ");
      } else {
        itn = node.getFormalList().getIterator();
        itd = doppelganger.getFormalList().getIterator();
                    
        for (int f = 1; itn.hasNext(); f++) {
          ntype = ((Formal) itn.next()).getType();
          dtype = ((Formal) itd.next()).getType();

          if (!ntype.equals(dtype)) {
            overload = true;
            err.register(err.SEMANT_ERROR, fname, ln, "method: "
              + name + " formal/doppleganger type mismatch.");
          }
        }
      }
    } // end doppelganger

            

            if (overload) {
                err.register(err.SEMANT_ERROR, fname, ln,
                    "overload not allowed");
            } else {
                
                

                mst.add(name, node);
            }
        }
        node.getFormalList().accept(this);
        node.getStmtList().accept(this);
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
        for (Iterator it = node.getIterator(); it.hasNext();){
            ((Formal) it.next()).accept(this);
        }
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
    node.getInit().accept(this);
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
    if (node.getExpr() != null)
        node.getExpr().accept(this);
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
    node.getRefExpr().accept(this);
    node.getActualList().accept(this);
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
    node.getExpr().accept(this);
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
     *     the binary comparison less than or equal to expression node
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
     *    the binary comparison greater to or equal to expression node
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
    if (node.getRef() != null)
        node.getRef().accept(this);
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
    return null;
    }
}
