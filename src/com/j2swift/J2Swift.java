package com.j2swift;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Main class for the Java to Swift converter
 * @author Eyob Tsegaye
 */
public class J2Swift {
	public static void main(String[] args) throws IOException {
		String inputFile = null;
		if (args.length > 0) inputFile = args[0];
		InputStream is = System.in;
		if (inputFile != null)
			is = new FileInputStream(inputFile);
		ANTLRInputStream input = new ANTLRInputStream(is);
		Java8Lexer lexer = new Java8Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		Java8Parser parser = new Java8Parser(tokens);
		ParserRuleContext tree = parser.compilationUnit();
		ParseTreeWalker walker = new ParseTreeWalker();
		J2SwiftListener listener = new J2SwiftListener();
		walker.walk(listener, tree);
		if (listener.protectedEncountered()) {
			System.out.println("A protected has been encountered");
			System.out.print("Do you want to replace it with the internal keyword (no for private) [Y/n]");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String response = br.readLine();
			if (response.charAt(0) == 'n') {
				listener.replaceProtected(false);
			}
			else {
				listener.replaceProtected(true);
			}
		}
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(inputFile+".swift")));
		pw.println(listener.swiftCode());
		pw.close();
	}
}
