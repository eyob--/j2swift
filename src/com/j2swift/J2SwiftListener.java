package com.j2swift;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.j2swift.Java8Parser.*;

/**
 * Actual "behind-the-scenes" java to swift converter that processes
 * the parse tree for the java file and gives back swift code
 * @author Eyob Tsegaye
 */
public class J2SwiftListener extends Java8BaseListener {

    private static Map<String, String> typeMap = new HashMap<>();
    private static Map<String, String> modifierMap = new HashMap<>();

    static {
        typeMap.put("boolean", "Bool");
        typeMap.put("Boolean", "Bool");
        typeMap.put("byte", "Int8");
        typeMap.put("Byte", "Int8");
        typeMap.put("short", "Int16");
        typeMap.put("Short", "Int16");
        typeMap.put("int", "Int32");
        typeMap.put("Integer", "Int32");
        typeMap.put("long", "Int64");
        typeMap.put("Long", "Int64");
        typeMap.put("float", "Float");
        typeMap.put("Float", "Float");
        typeMap.put("double", "Double");
        typeMap.put("Double", "Double");
        typeMap.put("char", "Character");
        typeMap.put("Character", "Character");
        typeMap.put("String", "String");

        modifierMap.put("public", "public");
        modifierMap.put("protected", "2public");    // will ask user in a later process
        modifierMap.put("private", "private");
        modifierMap.put("abstract", "error");
        modifierMap.put("static", "static");
        modifierMap.put("final", "final");
        modifierMap.put("strictfp", "error");
        modifierMap.put("transient", "error");
        modifierMap.put("volatile", "error");
    }

    private StringBuilder code = new StringBuilder();

    /**
     * Returns the swift code to be outputted to a file, or the empty string if
     * the tree hasn't been walked with this listener.
     * @return swift code as a giant String
     */
    public String swiftCode() {
        return code.length() == 0 ? code.toString() :  code.insert(0, "import Foundation\n\n").toString();
    }

    /**
     * Returns whether a "protected" has been encountered in the parsed Java code
     * @return whether the "protected" keyword has been encountered in the code
     */
    public boolean protectedEncountered() {
        return code.indexOf("2public") != -1;
    }

    /**
     * Replaces all found occurrences of the protected keyword with either the
     * internal or private keywords
     * @param withInternal whether the protected should be replaced with internal
     * or private
     */
    public void replaceProtected(boolean withInternal) {
        String replace = withInternal ? "internal" : "private";
        String prot = "2public";
        int index = code.indexOf(prot);
        while (index != -1) {
            code.replace(index, index + 7, replace);
            index = code.indexOf(prot);
        }
    }

    private void exitNonTranslatable(String message, ParserRuleContext ctx) {
        System.err.println("Error! Encountered non-translatable: " + message + " \""
                    + ctx.getParent().getText() + "\"");
        System.exit(1);
    }

    @Override
    public void enterNormalClassDeclaration(NormalClassDeclarationContext ctx) {
        code.append("\n");
        if (ctx.classModifier() == null)
            code.append("class ").append(ctx.Identifier());
    }

    @Override
    public void enterClassModifier(ClassModifierContext ctx) {
        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            exitNonTranslatable("class modifier '"+ctx.getText()+"'", ctx);
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitClassModifier(ClassModifierContext ctx) {
        if (ctx.getParent() instanceof NormalClassDeclarationContext) {
            NormalClassDeclarationContext parent = (NormalClassDeclarationContext) ctx.getParent();
            List<ClassModifierContext> modifierList = parent.classModifier();
            if (modifierList.get(modifierList.size()-1) == ctx) {
                code.append("class ").append(parent.Identifier());
            }
        }
    }

    @Override
    public void enterSuperclass(SuperclassContext ctx) {
        code.append(": ");
    }

    @Override
    public void enterSuperinterfaces(SuperinterfacesContext ctx) {
        boolean superClassExists = ctx.getParent() instanceof NormalClassDeclarationContext
                    && ((NormalClassDeclarationContext) ctx.getParent()).superclass() != null;
        if (superClassExists) {
            code.append(", ");
        }
        else {
            code.append(": ");
        }
    }

    @Override
    public void exitInterfaceType(InterfaceTypeContext ctx) {
        ParserRuleContext parent = ctx.getParent();
        if (parent instanceof InterfaceTypeListContext) {
            List<InterfaceTypeContext> interfaceList =
                        ((InterfaceTypeListContext) parent).interfaceType();
            if (interfaceList.get(interfaceList.size()-1) != ctx) {
                // if this isn't the last interface in a list, put a comma at the end
                code.append(", ");
            }
        }
    }

    @Override
    public void exitClassType(ClassTypeContext ctx) {
        // the identifier is usually printed in enterTypeArguments()
        if (ctx.typeArguments() == null)
            code.append(ctx.Identifier());
    }

    @Override
    public void enterClassBody(ClassBodyContext ctx) {
        code.append(" {\n");
    }

    @Override
    public void exitClassBody(ClassBodyContext ctx) {
        code.append("\n}");
    }

    @Override
    public void enterTypeParameters(TypeParametersContext ctx) {
        code.append("<");
    }

    @Override
    public void exitTypeParameters(TypeParametersContext ctx) {
        code.append(">");
    }

    @Override
    public void enterTypeParameter(TypeParameterContext ctx) {
        code.append(ctx.Identifier());
    }

    @Override
    public void exitTypeParameter(TypeParameterContext ctx) {
        List<TypeParameterContext> typeParameterList =
                    ((TypeParameterListContext) ctx.getParent()).typeParameter();
        if (typeParameterList.get(typeParameterList.size()-1) != ctx) {
            code.append(", ");
        }
    }

    @Override
    public void enterTypeArguments(TypeArgumentsContext ctx) {
        // print out class identifier if these are the type arguments for a class
        if (ctx.getParent() instanceof ClassTypeContext) {
            ClassTypeContext parent = (ClassTypeContext) ctx.getParent();
            if (parent.classOrInterfaceType() != null) {
                code.append(".");
            }
            code.append(parent.Identifier());
        }

        code.append("<");
    }

    @Override
    public void exitTypeArguments(TypeArgumentsContext ctx) {
        code.append(">");
    }

    @Override
    public void exitTypeArgument(TypeArgumentContext ctx) {
        List<TypeArgumentContext> typeArgumentList =
                    ((TypeArgumentListContext) ctx.getParent()).typeArgument();

        if (typeArgumentList.get(typeArgumentList.size()-1) != ctx) {
            code.append(", ");
        }
    }

    @Override
    public void enterTypeBound(TypeBoundContext ctx) {
        code.append(": ");
    }

    @Override
    public void enterTypeVariable(TypeVariableContext ctx) {
        code.append(ctx.Identifier());
    }

    @Override
    public void enterAdditionalBound(AdditionalBoundContext ctx) {
        exitNonTranslatable("additional type bound", ctx);
    }

    @Override
    public void enterClassType_lfno_classOrInterfaceType(ClassType_lfno_classOrInterfaceTypeContext ctx) {
        code.append(ctx.Identifier());
    }

    @Override
    public void enterClassType_lf_classOrInterfaceType(ClassType_lf_classOrInterfaceTypeContext ctx) {
        code.append('.').append(ctx.Identifier());
    }

    @Override
    public void enterWildcard(WildcardContext ctx) {
        exitNonTranslatable("wildcard", ctx);
    }

    @Override
    public void enterFieldDeclaration(FieldDeclarationContext ctx) {
        if (code.charAt(code.length()-2) == '{') {
            code.append("\n");
        }
    }

    @Override
    public void exitFieldDeclaration(FieldDeclarationContext ctx) {
        code.append(";\n");
    }

    @Override
    public void enterFieldModifier(FieldModifierContext ctx) {
        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            exitNonTranslatable("field modifier '"+ctx.getText()+"'", ctx);
        }
        if (text.equals("final")) {
            return;
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitFieldModifier(FieldModifierContext ctx) {
        List<FieldModifierContext> list = ((FieldDeclarationContext) ctx.getParent()).fieldModifier();
        if (list.get(list.size()-1) != ctx) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("final")) {
            code.append("let ");
        }
        else {
            code.append("var ");
        }
    }

    @Override
    public void exitVariableDeclarator(VariableDeclaratorContext ctx) {
        List<VariableDeclaratorContext> list = ((VariableDeclaratorListContext) ctx.getParent()).variableDeclarator();
        if (list.get(list.size()-1) != ctx) {
            code.append(", ");
        }
    }

    @Override
    public void enterVariableDeclaratorId(VariableDeclaratorIdContext ctx) {
        code.append(ctx.Identifier());
    }

}
