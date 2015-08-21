package com.j2swift;

import org.antlr.v4.runtime.*;
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
        modifierMap.put("synchronized", "error");
        modifierMap.put("native", "error");
        modifierMap.put("default", "error");
    }

    private StringBuilder code = new StringBuilder();

    private int depth = 0;
    private boolean skipping = false;
    private int skipDepth;

    /**
     * Returns the swift code to be outputted to a file, or the empty string if
     * the tree hasn't been walked with this listener.
     * @return swift code as a giant String
     */
    public String swiftCode() {
        return code.length() == 0 ? code.toString() :  code.insert(0, "import Foundation\n\n").toString();
    }

    public int numProtected() {
        return code.toString().split("2public", -1).length - 1;
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

    private boolean shouldSkipExit() {
        if (skipping) {
            if (skipDepth == depth) {
                skipping = false;
            }
            depth--;
            return true;
        }
        depth--;
        return false;
    }

    private boolean shouldSkipEnter() {
        depth++;
        return skipping;
    }

    private void skipSubtree() {
        skipping = true;
        skipDepth = depth;
    }

    @Override
    public void enterNormalClassDeclaration(NormalClassDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("\n");
        if (ctx.classModifier() == null)
            code.append("class ").append(ctx.Identifier());
    }

    @Override
    public void exitNormalClassDeclaration(NormalClassDeclarationContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterClassModifier(ClassModifierContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            Util.exitNonTranslatable("class modifier '"+ctx.getText()+"'", ctx);
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitClassModifier(ClassModifierContext ctx) {
        if (shouldSkipExit()) return;

        if (ctx.getParent() instanceof NormalClassDeclarationContext) {
            NormalClassDeclarationContext parent = (NormalClassDeclarationContext) ctx.getParent();
            List<ClassModifierContext> modifierList = parent.classModifier();
            if (modifierList.get(modifierList.size()-1) == ctx) {
                code.append("class ").append(parent.Identifier());
            }
        }
        else {
            EnumDeclarationContext parent = (EnumDeclarationContext) ctx.getParent();
            List<ClassModifierContext> modifierList = parent.classModifier();
            if (modifierList.get(modifierList.size()-1) == ctx) {
                code.append("enum ").append(parent.Identifier());
            }
        }
    }

    @Override
    public void enterSuperclass(SuperclassContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(": ");
    }

    @Override
    public void exitSuperclass(SuperclassContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterSuperinterfaces(SuperinterfacesContext ctx) {
        if (shouldSkipEnter()) return;

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
    public void exitSuperinterfaces(SuperinterfacesContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterInterfaceType(InterfaceTypeContext ctx) {
        if (shouldSkipEnter()) return;
    }

    @Override
    public void exitInterfaceType(InterfaceTypeContext ctx) {
        if (shouldSkipExit()) return;

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
    public void enterClassType(ClassTypeContext ctx) {
        if (shouldSkipEnter()) return;
    }

    @Override
    public void exitClassType(ClassTypeContext ctx) {
        if (shouldSkipExit()) return;

        // the identifier is usually printed in enterTypeArguments()
        if (ctx.typeArguments() == null)
            code.append(ctx.Identifier());
    }

    @Override
    public void enterClassBody(ClassBodyContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.getParent() instanceof EnumConstantContext) {
            Util.exitNonTranslatable("enum constant class body", ctx);
        }
        code.append(" {\n");
    }

    @Override
    public void exitClassBody(ClassBodyContext ctx) {
        if (shouldSkipExit()) return;

        code.append("\n}\n");
    }

    @Override
    public void enterTypeParameters(TypeParametersContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("<");
    }

    @Override
    public void exitTypeParameters(TypeParametersContext ctx) {
        if (shouldSkipExit()) return;

        code.append(">");
    }

    @Override
    public void enterTypeParameter(TypeParameterContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(ctx.Identifier());
    }

    @Override
    public void exitTypeParameter(TypeParameterContext ctx) {
        if (shouldSkipExit()) return;

        List<TypeParameterContext> typeParameterList =
                    ((TypeParameterListContext) ctx.getParent()).typeParameter();
        if (typeParameterList.get(typeParameterList.size()-1) != ctx) {
            code.append(", ");
        }
    }

    @Override
    public void enterTypeArguments(TypeArgumentsContext ctx) {
        if (shouldSkipEnter()) return;

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
        if (shouldSkipExit()) return;

        code.append(">");
    }

    @Override
    public void enterTypeArgument(TypeArgumentContext ctx) {
        if (shouldSkipEnter()) return;
    }

    @Override
    public void exitTypeArgument(TypeArgumentContext ctx) {
        if (shouldSkipExit()) return;

        List<TypeArgumentContext> typeArgumentList =
                    ((TypeArgumentListContext) ctx.getParent()).typeArgument();

        if (typeArgumentList.get(typeArgumentList.size()-1) != ctx) {
            code.append(", ");
        }
    }

    @Override
    public void enterTypeBound(TypeBoundContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(": ");
    }

    @Override
    public void exitTypeBound(TypeBoundContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterTypeVariable(TypeVariableContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(ctx.Identifier());
    }

    @Override
    public void exitTypeVariable(TypeVariableContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterAdditionalBound(AdditionalBoundContext ctx) {
        if (shouldSkipEnter()) return;

        Util.exitNonTranslatable("additional type bound", ctx);
    }

    @Override
    public void exitAdditionalBound(AdditionalBoundContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterClassType_lfno_classOrInterfaceType(ClassType_lfno_classOrInterfaceTypeContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(ctx.Identifier());
    }

    @Override
    public void exitClassType_lfno_classOrInterfaceType(ClassType_lfno_classOrInterfaceTypeContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterClassType_lf_classOrInterfaceType(ClassType_lf_classOrInterfaceTypeContext ctx) {
        if (shouldSkipEnter()) return;

        code.append('.').append(ctx.Identifier());
    }

    @Override
    public void exitClassType_lf_classOrInterfaceType(ClassType_lf_classOrInterfaceTypeContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterWildcard(WildcardContext ctx) {
        if (shouldSkipEnter()) return;

        Util.exitNonTranslatable("wildcard", ctx);
    }

    @Override
    public void exitWildcard(WildcardContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterFieldDeclaration(FieldDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        if (code.charAt(code.length()-2) == '{') {
            code.append("\n");
        }
    }

    @Override
    public void exitFieldDeclaration(FieldDeclarationContext ctx) {
        if (shouldSkipExit()) return;

        code.append('\n');
    }

    @Override
    public void enterFieldModifier(FieldModifierContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            Util.exitNonTranslatable("field modifier '"+ctx.getText()+"'", ctx);
        }
        if (text.equals("final")) {
            return;
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitFieldModifier(FieldModifierContext ctx) {
        if (shouldSkipExit()) return;

        List<FieldModifierContext> list = ((FieldDeclarationContext) ctx.getParent()).fieldModifier();
        if (list.get(list.size()-1) != ctx) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("final")) {
            code.append("let ");
        }
        else {
            code.append("var ");
        }
        code.append("@@");   // mark start of unannType
    }

    @Override
    public void enterVariableDeclarator(VariableDeclaratorContext ctx) {
        if (shouldSkipEnter()) return;
    }

    @Override
    public void exitVariableDeclarator(VariableDeclaratorContext ctx) {
        if (shouldSkipExit()) return;

        List<VariableDeclaratorContext> list = ((VariableDeclaratorListContext) ctx.getParent()).variableDeclarator();
        if (list.get(list.size()-1) != ctx) {
            code.append(", ");
        }
    }

    @Override
    public void enterVariableDeclaratorId(VariableDeclaratorIdContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.dims() != null) {
            Util.exitNonTranslatable("C-style array declaration", ctx);
        }

        if (ctx.getParent() instanceof FormalParameterContext) {
            int index = code.lastIndexOf("!!");
            String paramType = code.substring(index+2);
            code.delete(index, code.length());
            code.append(ctx.Identifier()).append(": ").append(paramType);
        }
        else if (ctx.getParent() instanceof LastFormalParameterContext) {
            int index = code.lastIndexOf("!!");
            String paramType = code.substring(index+2);
            code.delete(index, code.length());
            code.append(ctx.Identifier()).append(": ").append(paramType).append("...");
        }
        else if (ctx.getParent() instanceof VariableDeclaratorContext) {
            List<VariableDeclaratorContext> list = ((VariableDeclaratorListContext) ctx.getParent().getParent()).variableDeclarator();
            int index = code.lastIndexOf("@@");
            String type;

            if (list.get(0) == ctx.getParent()) {
                type = code.substring(index+2);
                code.delete(index, code.length());
            }
            else {
                type = code.substring(index+2, code.length()-2);
                code.delete(index, index+2);
            }

            if (list.get(list.size()-1) != ctx.getParent()) {
                code.append(ctx.Identifier()).append(": @@").append(type);
            }
            else {
                code.append(ctx.Identifier()).append(": ").append(type);
            }
        }
        else {
            code.append(ctx.Identifier());
        }
    }

    public void exitVariableDeclaratorId(VariableDeclaratorIdContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterUnannType(UnannTypeContext ctx) {
        if (shouldSkipEnter()) return;

        String text = typeMap.get(ctx.getText());
        if (text != null) {
            code.append(text);
            code.append('#');
        }
    }

    @Override
    public void exitUnannType(UnannTypeContext ctx) {
        if (shouldSkipExit()) return;

        if (code.lastIndexOf("#") != -1) {
            code.delete(code.lastIndexOf("#"), code.length());
        }
    }

    @Override
    public void enterUnannPrimitiveType(UnannPrimitiveTypeContext ctx) {
        if (shouldSkipEnter()) return;

        String text = typeMap.get(ctx.getText());
        if (text == null) {
            code.append(ctx.getText());
        }
        else {
            code.append(text);
        }
    }

    @Override
    public void exitUnannPrimitiveType(UnannPrimitiveTypeContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterUnannTypeVariable(UnannTypeVariableContext ctx) {
        if (shouldSkipEnter()) return;

        String text = typeMap.get(ctx.Identifier());
        if (text == null) {
            code.append(ctx.Identifier());
        }
        else {
            code.append(text);
        }
    }

    @Override
    public void exitUnannTypeVariable(UnannTypeVariableContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterUnannClassType_lf_unannClassOrInterfaceType(UnannClassType_lf_unannClassOrInterfaceTypeContext ctx) {
        if (shouldSkipEnter()) return;

        code.append('.').append(ctx.Identifier());
    }

    @Override
    public void exitUnannClassType_lf_unannClassOrInterfaceType(UnannClassType_lf_unannClassOrInterfaceTypeContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterUnannClassType_lfno_unannClassOrInterfaceType(UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(ctx.Identifier());
    }

    @Override
    public void exitUnannClassType_lfno_unannClassOrInterfaceType(UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterUnannArrayType(UnannArrayTypeContext ctx) {
        if (shouldSkipEnter()) return;

        int numDims = Util.numSquareBrackets(ctx.dims().getText());
        for (int i = 0; i < numDims; i++) {
            code.append('[');
        }
    }

    @Override
    public void exitUnannArrayType(UnannArrayTypeContext ctx) {
        if (shouldSkipExit()) return;

        int numDims = Util.numSquareBrackets(ctx.dims().getText());
        for (int i = 0; i < numDims; i++) {
            code.append(']');
        }
    }

    @Override
    public void enterMethodDeclaration(MethodDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        code.append('\n');
    }

    @Override
    public void exitMethodDeclaration(MethodDeclarationContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterMethodModifier(MethodModifierContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            Util.exitNonTranslatable("class method modifier '"+ctx.getText()+"'", ctx);
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitMethodModifier(MethodModifierContext ctx) {
        if (shouldSkipExit()) return;
    }

    public void enterMethodHeader(MethodHeaderContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("??");  // start of possible type parameters
    }

    @Override
    public void exitMethodHeader(MethodHeaderContext ctx) {
        if (shouldSkipExit()) return;

        int resultEnd = code.lastIndexOf("@@");
        int resultStart = code.lastIndexOf("@@", resultEnd-2);
        if (resultEnd-resultStart == 2) {
            // no return value
            code.delete(resultStart, resultEnd+2);
            return;
        }
        String result = code.substring(resultStart+2, resultEnd);
        code.delete(resultStart, resultEnd+2);
        code.append(" -> ").append(result);
    }

    @Override
    public void enterResult(ResultContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("??");   // end of possible type parameters
        code.append("@@");  // mark the beginning of the unannType
    }

    @Override
    public void exitResult(ResultContext ctx) {
        if (shouldSkipExit()) return;

        code.append("@@");  // mark the end of the unannType
    }

    @Override
    public void enterMethodDeclarator(MethodDeclaratorContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.dims() != null) {
            Util.exitNonTranslatable("C-style array declaration", ctx);
        }

        code.append("func ").append(ctx.Identifier());
        int typeParamsEnd = code.lastIndexOf("??");
        int typeParamsStart = code.lastIndexOf("??", typeParamsEnd-2);
        String typeParams = code.substring(typeParamsStart+2, typeParamsEnd);
        code.delete(typeParamsStart, typeParamsEnd+2);
        code.append(typeParams).append('(');
    }

    @Override
    public void exitMethodDeclarator(MethodDeclaratorContext ctx) {
        if (shouldSkipExit()) return;

        code.append(')');
    }

    @Override
    public void enterFormalParameter(FormalParameterContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.variableModifier() != null) {
            boolean isConstant = false;
            for (VariableModifierContext varMod : ctx.variableModifier()) {
                if (varMod.getText().equals("final")) {
                    isConstant = true;
                    break;
                }
            }
            if (!isConstant) {
                code.append("var ");
            }
        }

        if (ctx.getParent() instanceof FormalParametersContext) {
            if (((FormalParametersContext) ctx.getParent()).formalParameter().get(0) != ctx || !(ctx.getParent().getParent().getParent() instanceof MethodDeclaratorContext)) {
                code.append("_ ");
            }
        }
        else if (!(ctx.getParent().getParent().getParent() instanceof MethodDeclaratorContext)) {
            code.append("_ ");
        }

        code.append("!!");  // to mark the start of the unannType
    }

    @Override
    public void exitFormalParameter(FormalParameterContext ctx) {
        if (shouldSkipExit()) return;

        if (ctx.getParent() instanceof FormalParametersContext) {
            code.append(", ");
        }
    }

    @Override
    public void enterReceiverParameter(ReceiverParameterContext ctx) {
        if (shouldSkipEnter()) return;

        Util.exitNonTranslatable("receiver parameter", ctx);
    }

    @Override
    public void exitReceiverParameter(ReceiverParameterContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterLastFormalParameter(LastFormalParameterContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.formalParameter() != null) return;

        if (ctx.variableModifier() != null) {
            boolean isConstant = false;
            for (VariableModifierContext varMod : ctx.variableModifier()) {
                if (varMod.getText().equals("final")) {
                    isConstant = true;
                    break;
                }
            }
            if (!isConstant) {
                code.append("var ");
            }
        }

        if (ctx.getParent() instanceof FormalParameterListContext) {
            if (((FormalParameterListContext) ctx.getParent()).formalParameters() != null || !(ctx.getParent().getParent() instanceof MethodDeclaratorContext)) {
                code.append("_ ");
            }
        }

        code.append("!!");  // to mark the start of the unannType
    }

    @Override
    public void exitLastFormalParameter(LastFormalParameterContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterThrows_(Throws_Context ctx) {
        if (shouldSkipEnter()) return;

        code.append(" throws!!");
    }

    @Override
    public void exitThrows_(Throws_Context ctx) {
        if (shouldSkipExit()) return;

        code.delete(code.lastIndexOf("!!"), code.length());
    }

    @Override
    public void enterMethodBody(MethodBodyContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.block() == null) {
            code.append('\n');
        }
        else {
            code.append(' ');
        }
    }

    @Override
    public void exitMethodBody(MethodBodyContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterBlock(BlockContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("{\n");
    }

    @Override
    public void exitBlock(BlockContext ctx) {
        if (shouldSkipExit()) return;

        code.append("}\n");
    }

    @Override
    public void enterConstructorDeclaration(ConstructorDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        code.append('\n');
    }

    @Override
    public void exitConstructorDeclaration(ConstructorDeclarationContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterConstructorModifier(ConstructorModifierContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        code.append(text).append(' ');
    }

    @Override
    public void exitConstructorModifier(ConstructorModifierContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterConstructorDeclarator(ConstructorDeclaratorContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("??");  // start of possible type parameters
    }

    @Override
    public void exitConstructorDeclarator(ConstructorDeclaratorContext ctx) {
        if (shouldSkipExit()) return;

        code.append(')');
    }

    @Override
    public void enterSimpleTypeName(SimpleTypeNameContext ctx) {
        if (shouldSkipEnter()) return;

        int index = code.lastIndexOf("??");
        String typeParams = code.substring(index+2);
        code.delete(index, code.length());
        code.append("init").append(typeParams).append('(');
    }

    @Override
    public void exitSimpleTypeName(SimpleTypeNameContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterConstructorBody(ConstructorBodyContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(" {\n");
    }

    @Override
    public void exitConstructorBody(ConstructorBodyContext ctx) {
        if (shouldSkipExit()) return;

        code.append("}\n");
    }

    @Override
    public void enterStaticInitializer(StaticInitializerContext ctx) {
        if (shouldSkipEnter()) return;

        Util.exitNonTranslatable("static initializer block", ctx);
    }

    @Override
    public void exitStaticInitializer(StaticInitializerContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterInstanceInitializer(InstanceInitializerContext ctx) {
        if (shouldSkipEnter()) return;

        Util.exitNonTranslatable("instance initializer block", ctx);
    }

    @Override
    public void exitInstanceInitializer(InstanceInitializerContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterNormalInterfaceDeclaration(NormalInterfaceDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        code.append('\n');
    }

    @Override
    public void exitNormalInterfaceDeclaration(NormalInterfaceDeclarationContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterInterfaceModifier(InterfaceModifierContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            Util.exitNonTranslatable("interface modifier '"+ctx.getText()+"'", ctx);
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitInterfaceModifier(InterfaceModifierContext ctx) {
        if (shouldSkipExit()) return;

        NormalInterfaceDeclarationContext parent = (NormalInterfaceDeclarationContext) ctx.getParent();
        List<InterfaceModifierContext> list = parent.interfaceModifier();
        if (list.get(list.size()-1) == ctx) {
            code.append("protocol ").append(parent.Identifier());
        }
    }

    @Override
    public void enterExtendsInterfaces(ExtendsInterfacesContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(": ");
    }

    @Override
    public void exitExtendsInterfaces(ExtendsInterfacesContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterInterfaceBody(InterfaceBodyContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(" {\n");
    }

    @Override
    public void exitInterfaceBody(InterfaceBodyContext ctx) {
        if (shouldSkipExit()) return;

        code.append("\n}\n");
    }

    @Override
    public void enterConstantDeclaration(ConstantDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        if (code.charAt(code.length()-2) == '{') {
            code.append("\n");
        }
    }

    @Override
    public void exitConstantDeclaration(ConstantDeclarationContext ctx) {
        if (shouldSkipExit()) return;

        code.append('\n');
    }

    @Override
    public void enterConstantModifier(ConstantModifierContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("final")) {
            return;
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitConstantModifier(ConstantModifierContext ctx) {
        if (shouldSkipExit()) return;

        List<ConstantModifierContext> list = ((ConstantDeclarationContext) ctx.getParent()).constantModifier();
        if (list.get(list.size()-1) != ctx) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("final")) {
            code.append("let ");
        }
        else {
            code.append("var ");
        }
        code.append("@@");   // mark start of unannType
    }

    @Override
    public void enterInterfaceMethodDeclaration(InterfaceMethodDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        code.append('\n');
    }

    @Override
    public void exitInterfaceMethodDeclaration(InterfaceMethodDeclarationContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterInterfaceMethodModifier(InterfaceMethodModifierContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            Util.exitNonTranslatable("interface method modifier '"+ctx.getText()+"'", ctx);
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitInterfaceMethodModifier(InterfaceMethodModifierContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterEnumDeclaration(EnumDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("\n");
        if (ctx.classModifier() == null)
            code.append("enum ").append(ctx.Identifier());
    }

    @Override
    public void exitEnumDeclaration(EnumDeclarationContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterEnumBody(EnumBodyContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(" {\n");
    }

    @Override
    public void exitEnumBody(EnumBodyContext ctx) {
        if (shouldSkipExit()) return;

        code.append("\n}\n");
    }

    @Override
    public void enterEnumConstantList(EnumConstantListContext ctx) {
        if (shouldSkipEnter()) return;

        code.append("case ");
    }

    @Override
    public void exitEnumConstantList(EnumConstantListContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterEnumConstant(EnumConstantContext ctx) {
        if (shouldSkipEnter()) return;

        code.append(ctx.Identifier());
    }

    @Override
    public void exitEnumConstant(EnumConstantContext ctx) {
        if (shouldSkipExit()) return;

        List<EnumConstantContext> list = ((EnumConstantListContext) ctx.getParent()).enumConstant();
        if (list.get(list.size()-1) != ctx) {
            code.append(", ");
        }
    }

    @Override
    public void enterArgumentList(ArgumentListContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.getParent() instanceof EnumConstantContext) {
            Util.exitNonTranslatable("enum constant initializer", ctx);
        }
    }

    @Override
    public void exitArgumentList(ArgumentListContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterEnumBodyDeclarations(EnumBodyDeclarationsContext ctx) {
        if (shouldSkipEnter()) return;

        if (ctx.classBodyDeclaration().size() > 0) {
            Util.exitNonTranslatable("enum body declaration", ctx);
        }
    }

    @Override
    public void exitEnumBodyDeclarations(EnumBodyDeclarationsContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterAnnotation(AnnotationContext ctx) {
        if (shouldSkipEnter()) return;

        skipSubtree();
    }

    @Override
    public void exitAnnotation(AnnotationContext ctx) {
        if (shouldSkipExit()) return;
    }

    @Override
    public void enterAnnotationTypeDeclaration(AnnotationTypeDeclarationContext ctx) {
        if (shouldSkipEnter()) return;

        skipSubtree();
    }

    @Override
    public void exitAnnotationTypeDeclaration(AnnotationTypeDeclarationContext ctx) {
        if (shouldSkipExit()) return;
    }

}
