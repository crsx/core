/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
grammar Plank;

hscript : declaration* ;

declaration
:  sort 'data' CONS forms ';'              #DataDeclaration
|  sort 'scheme' CONS forms ';'            #SchemeDeclaration
|  sort 'variable' ';'                     #VariableDeclaration
|  sort PRIORITY? 'rule' term '→' term ';' #RuleDeclaration
;

forms : '(' form (',' form)* ')' | '(' ')' | ;
form
:  '[' sorts ']' sort                    #ScopeForm
|  	'{' sort ':' sort '}'                #AssocForm
;

sorts : '<' sort (',' sort)* '>' | '<' '>' | ;
sort
:  CONS sorts                            #InstanceSort
|  VAR                                   #VarSort
;

terms: '(' term (',' term)* ')' | '(' ')' | ;
term
:  CONS pieces                           #ConsTerm
|  VAR                                   #VarTerm
|  META terms                            #MetaTerm
;

pieces : piece (',' piece)* | ;
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

CONS : [A-Z] [A-Za-z0-9_]* ;
META : '#' [A-Za-z0-9_]* ;
VAR  : [a-z] [A-Za-z0-9_]* ;
PRIORITY : 'default' | 'priority' ;
