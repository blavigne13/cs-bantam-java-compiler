# Bantam-Java-Compiler

Course: Compilers

Date: Fall 2014 (4th semester)

* Lexer: Regexes for tokenization
* Grammar: Wanted LL(1), but ended up needing to use a lookahead of 2 in DispatchExpr and a lookahead  of 1 somewhere else (if i remember correctly).
* Semantic analyzer uses visitor pattern to walk the AST. Still has a lot of placeholder text in the error output, and I think there was an issue with array initialization or casting (it's been almost 2 years since I looked at it)
* Java bytecode generator also uses visitor pattern to walk the AST, outputting java bytecode. The copy of the files I have does not include the completed bytecode generator, however.
