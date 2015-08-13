# j2swift

A Java to Swift syntax converter (still a work in progress).

## Why?

When you're writing giant Android apps with loads of functionality, you really don't want to spend time going through the trouble of converting your non-Android-API code into swift line by laborious line. In this process, you not only waste enormous effort, you also waste an enormous amount of time that you could be using to add even more functionality to your app! Thus, a tool is desperately needed to make this process quicker and virtually painless so that you can go back to more important matters. j2swift hopes to accomplish this task through the use of the antlr4 parser generator (https://github.com/antlr/antlr4). Since semantics is matter that should be left mostly to the user, j2swift focuses mostly on syntax conversion.

## Versions

For now, j2swift converts Java 8 syntax (https://github.com/antlr/grammars-v4/blob/master/java8/Java8.g4) to Swift 1.2 syntax.

## General Process
The program accepts a single java file as the argument for the program and writes the converted program into a file with name "[original filename].swift"

## When it crashes
If there is no Swift equivalent of a section of code, the program will tell the user so in stderr and exit with a status of 1. These are the list of things which will cause a crash:
* Non-translatable modifiers
	* abstract
	* strictfp
	* transient
	* volatile
* Type Parameter Quirks
	* additional bound "&"
	* wildcard "?"

## What it covers
For an actual example of what j2swift covers for now, check Test.java and Test.java.swift

### Class Declaration
* Class modifiers
	* protected	-> 2public (the user will be asked later whether they want to make the modifier "private" or "internal")
* Type Parameters
* Superclass and Superinterfaces
* Class body (the braces)
### Field Declaration
* Field modifiers
	* the "final" modifier will cause the variable to be declared with "let"
* Variable declaration list
	* without initialization of variables
