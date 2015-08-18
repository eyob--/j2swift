# j2swift

A Java to Swift syntax converter (still a work in progress).

## Table of Contents
* [Why?](#why)
* [Versions](#versions)
* [General Process](#general-process)
* [When It Crashes](#when-it-crashes)
* [Covered Syntax](#covered-syntax)
	* [Class Declaration](#class-declaration)
	* [Interface Declaration](#interface-declaration)
	* [Field Declaration](#field-declaration)
	* [Method Declaration](#method-declaration)
	* [Constructor Declaration](#constructor-declaration)

## Why?

When you're writing giant Android apps with loads of functionality, you really don't want to spend time going through the trouble of converting your non-Android-API code into swift line by laborious line. In this process, you not only waste enormous effort, you also waste an enormous amount of time that you could be using to add even more functionality to your app! Thus, a tool is desperately needed to make this process quicker and virtually painless so that you can go back to more important matters. j2swift hopes to accomplish this task through the use of the [antlr4 parser generator](https://github.com/antlr/antlr4). Since semantics is matter that should be left mostly to the user, j2swift focuses mostly on syntax conversion.

## Versions

For now, j2swift converts Java 8 syntax (https://github.com/antlr/grammars-v4/blob/master/java8/Java8.g4) to Swift 1.2 syntax.

## Install

First follow [these instructions on installing the antlr4 library](https://theantlrguy.atlassian.net/wiki/display/ANTLR4/Getting+Started+with+ANTLR+v4). Then, you can install and run the tool on the test file as follows:

```sh
$ git clone https://github.com/eyob--/j2swift.git
$ cd j2swift/src/
$ javac com/j2swift/*.java
$ java com.j2swift.J2Swift ../Test.java
```

## General Process

The program accepts a single java file as the argument for the program and writes the converted program into a file with name "[original filename].swift"

## When It Crashes

If there is no Swift equivalent of a section of code, the program will tell the user so in stderr and exit with a status of 1. These are the list of things which will cause a crash:
* Non-translatable modifiers
	* abstract
	* strictfp
	* transient
	* volatile
	* native
	* synchronized
	* default
* Type Parameter Quirks
	* additional bound "&"
	* wildcard "?"
* C-Style array declaration
* Receiver parameter

## Covered Syntax

For an actual example of what j2swift covers for now, check Test.java and Test.java.swift

### Class Declaration

* Class modifiers
	* protected	-> internal or private (user is asked)
* Type parameters
* Superclass and Superinterfaces
* Class body (braces)

### Interface Declaration
* Interface modifiers
* Type parameters
* Parent interfaces
* Interface body (braces)

### Field Declaration

* Field modifiers (class and interface)
	* the "final" modifier will cause the variable to be declared with "let"
* Variable declaration list
	* without initialization of variables
	* type declarations included (e.g. "var a: Int32")

### Method Declaration

* Method modifiers (class and interface)
* Type parameters
* Return value
* Formal Parameters
	* regular parameters
	* constant parameters
	* varargs
* Throws declaration
* Method body (braces or semicolon)

### Constructor Declaration

* Constructor modifiers
* Type parameters
* Formal Parameters
	* regular parameters
	* constant parameters
	* varargs
* Throws declaration
* Constructor body (braces)
