/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
grammar Plank;

hscript : declaration* ;

declaration
:  sort 'data' CONS forms ';'            #DataDeclaration
|  sort 'scheme' CONS forms ';'          #SchemeDeclaration
|  sort 'variable' ';'                   #VariableDeclaration
|  opts sort 'rule' term '→' term ';'   #RuleDeclaration
;

forms : '(' form (',' form)* ')' | '(' ')' | ;
form
:  bindersorts sort                      #ScopeForm
|  	'{' sort ':' sort '}'                #AssocForm
;
bindersorts : '[' sorts ']' | ;

sorts : sort (',' sort)* | ;
sort
:  CONS sortparams                       #InstanceSort
|  VAR                                   #VarSort
;

sortparams : '<' sorts '>' | ;

terms: '(' term (',' term)* ')' | '(' ')' | ;
term
:  sortanno rawterm
;
sortanno : '<' sort '>' | ;

rawterm
:  CONS pieces                           #ConsTerm
|  VAR                                   #VarTerm
|  META terms                            #MetaTerm
;

pieces : '(' piece (',' piece)* ')' | '(' ')' | ;
piece
:  binders term                          #ScopePiece
|  associations                          #AssocPiece
;
binders : '[' VAR (',' VAR)* ']' | '[' ']' | ;

associations : '{' association (',' association)* '}' | '{' '}' | ;
association
:  VAR ':' term                          #MapAssoc
|  '¬' VAR                               #NotAssoc
|  META terms                            #AllAssoc
;

opts : '[' opt (',' opt)* ']' | '[' ']' | ;
opt
: PRIORITY                               #Priority
| CONS                                   #Name
;

// Tokens.
CONS : [A-Z] [A-Za-z0-9_]* ;
META : '#' [A-Za-z0-9_]* ;
VAR  : [a-z] [A-Za-z0-9_]* ;
PRIORITY : 'default' | 'priority' ;

// Skip.
WS : ([ \t\r\n] | '/*' .*? '*/') -> skip;