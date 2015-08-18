package com.j2swift;

import org.antlr.v4.runtime.*;

/**
 * Utility class for J2Swift tool
 * @author Eyob Tsegaye
 */
public class Util {

	/**
	 * Outputs an error message and the parent of the parser rule which it
	 * crashed on, and then exits with a status of 1
	 * @param message error message to print
	 * @param ctx rule context that caused the crash
	 */
    public static void exitNonTranslatable(String message, ParserRuleContext ctx) {
        System.err.println("Error! Encountered non-translatable: " + message + " \""
                    + ctx.getParent().getText() + "\"");
        System.exit(1);
    }

	/**
     * Gets the number of left square brackets in a String
     * @param s String to find square brackets in
     * @return the number of left square brackets
     */
    public static int numSquareBrackets(String s) {
      return s.length() - s.replace("[", "").length();
    }

}
