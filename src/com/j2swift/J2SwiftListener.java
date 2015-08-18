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
            Util.exitNonTranslatable("class modifier '"+ctx.getText()+"'", ctx);
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
        code.append("\n}\n");
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
        Util.exitNonTranslatable("additional type bound", ctx);
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
        Util.exitNonTranslatable("wildcard", ctx);
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
            Util.exitNonTranslatable("field modifier '"+ctx.getText()+"'", ctx);
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
        code.append('\0');   // mark start of unannType
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
            int index = code.lastIndexOf("\0");
            String type;

            if (list.get(0) == ctx.getParent()) {
                type = code.substring(index+1);
                code.delete(index, code.length());
            }
            else {
                type = code.substring(index+1, code.length()-2);
                code.deleteCharAt(index);
            }

            if (list.get(list.size()-1) != ctx.getParent()) {
                code.append(ctx.Identifier()).append(": \0").append(type);
            }
            else {
                code.append(ctx.Identifier()).append(": ").append(type);
            }
        }
        else {
            code.append(ctx.Identifier());
        }
    }

    @Override
    public void enterUnannType(UnannTypeContext ctx) {
        String text = typeMap.get(ctx.getText());
        if (text != null) {
            code.append(text);
            code.append('#');
        }
    }

    @Override
    public void exitUnannType(UnannTypeContext ctx) {
        if (code.lastIndexOf("#") != -1) {
            code.delete(code.lastIndexOf("#"), code.length());
        }
    }

    @Override
    public void enterUnannPrimitiveType(UnannPrimitiveTypeContext ctx) {
      String text = typeMap.get(ctx.getText());
      if (text == null) {
        code.append(ctx.getText());
      }
      else {
        code.append(text);
      }
    }

    @Override
    public void enterUnannTypeVariable(UnannTypeVariableContext ctx) {
      String text = typeMap.get(ctx.Identifier());
      if (text == null) {
        code.append(ctx.Identifier());
      }
      else {
        code.append(text);
      }
    }

    @Override
    public void enterUnannClassType_lf_unannClassOrInterfaceType(UnannClassType_lf_unannClassOrInterfaceTypeContext ctx) {
      code.append('.').append(ctx.Identifier());
    }

    @Override
    public void enterUnannClassType_lfno_unannClassOrInterfaceType(UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx) {
      code.append(ctx.Identifier());
    }

    @Override
    public void enterUnannArrayType(UnannArrayTypeContext ctx) {
      int numDims = Util.numSquareBrackets(ctx.dims().getText());
      for (int i = 0; i < numDims; i++) {
        code.append('[');
      }
    }

    @Override
    public void exitUnannArrayType(UnannArrayTypeContext ctx) {
      int numDims = Util.numSquareBrackets(ctx.dims().getText());
      for (int i = 0; i < numDims; i++) {
        code.append(']');
      }
    }

    @Override
    public void enterMethodDeclaration(MethodDeclarationContext ctx) {
        code.append('\n');
    }

    @Override
    public void enterMethodModifier(MethodModifierContext ctx) {
        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            Util.exitNonTranslatable("method modifier '"+ctx.getText()+"'", ctx);
        }
        code.append(text).append(' ');
    }

    public void enterMethodHeader(MethodHeaderContext ctx) {
        code.append("??");  // start of possible type parameters
    }

    @Override
    public void exitMethodHeader(MethodHeaderContext ctx) {
        int resultEnd = code.lastIndexOf("\0");
        int resultStart = code.lastIndexOf("\0", resultEnd-1);
        if (resultEnd-resultStart == 1) {
            // no return value
            code.append(' ');
            return;
        }
        String result = code.substring(resultStart+1, resultEnd);
        code.delete(resultStart, resultEnd+1);
        code.append(" -> ").append(result).append(' ');
    }

    @Override
    public void enterResult(ResultContext ctx) {
        code.append("??");   // end of possible type parameters
        code.append('\0');  // mark the beginning of the unannType
    }

    @Override
    public void exitResult(ResultContext ctx) {
        code.append('\0');  // mark the end of the unannType
    }

    @Override
    public void enterMethodDeclarator(MethodDeclaratorContext ctx) {
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
        code.append(')');
    }

    @Override
    public void enterFormalParameter(FormalParameterContext ctx) {
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
        if (ctx.getParent() instanceof FormalParametersContext) {
            code.append(", ");
        }
    }

    @Override
    public void enterReceiverParameter(ReceiverParameterContext ctx) {
        Util.exitNonTranslatable("receiver parameter", ctx);
    }

    @Override
    public void enterLastFormalParameter(LastFormalParameterContext ctx) {
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
    public void enterThrows_(Throws_Context ctx) {
        code.append(" throws!!");
    }

    @Override
    public void exitThrows_(Throws_Context ctx) {
        code.delete(code.lastIndexOf("!!"), code.length());
    }

    @Override
    public void enterMethodBody(MethodBodyContext ctx) {
        if (ctx.block() == null) {
            code.append(ctx.getText());
        }
    }

    @Override
    public void enterBlock(BlockContext ctx) {
        code.append("{\n");
    }

    @Override
    public void exitBlock(BlockContext ctx) {
        code.append("}\n");
    }

    @Override
    public void enterConstructorDeclaration(ConstructorDeclarationContext ctx) {
        code.append('\n');
    }

    @Override
    public void enterConstructorModifier(ConstructorModifierContext ctx) {
        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        code.append(text).append(' ');
    }

    @Override
    public void enterConstructorDeclarator(ConstructorDeclaratorContext ctx) {
        code.append("??");  // start of possible type parameters
    }

    @Override
    public void exitConstructorDeclarator(ConstructorDeclaratorContext ctx) {
        code.append(')');
    }

    @Override
    public void enterSimpleTypeName(SimpleTypeNameContext ctx) {
        int index = code.lastIndexOf("??");
        String typeParams = code.substring(index+2);
        code.delete(index, code.length());
        code.append("init").append(typeParams).append('(');
    }

    @Override
    public void enterConstructorBody(ConstructorBodyContext ctx) {
        code.append(" {\n");
    }

    @Override
    public void exitConstructorBody(ConstructorBodyContext ctx) {
        code.append("}\n");
    }

    @Override
    public void enterStaticInitializer(StaticInitializerContext ctx) {
        Util.exitNonTranslatable("static initializer block", ctx);
    }

    @Override
    public void enterInstanceInitializer(InstanceInitializerContext ctx) {
        Util.exitNonTranslatable("instance initializer block", ctx);
    }

    @Override
    public void enterNormalInterfaceDeclaration(NormalInterfaceDeclarationContext ctx) {
        code.append('\n');
    }

    @Override
    public void enterInterfaceModifier(InterfaceModifierContext ctx) {
        if (ctx.annotation() != null) return;
        String text = modifierMap.get(ctx.getText());
        if (text.equals("error")) {
            Util.exitNonTranslatable("interface modifier '"+ctx.getText()+"'", ctx);
        }
        code.append(text).append(' ');
    }

    @Override
    public void exitInterfaceModifier(InterfaceModifierContext ctx) {
        NormalInterfaceDeclarationContext parent = (NormalInterfaceDeclarationContext) ctx.getParent();
        List<InterfaceModifierContext> list = parent.interfaceModifier();
        if (list.get(list.size()-1) == ctx) {
            code.append("protocol ").append(parent.Identifier());
        }
    }

    @Override
    public void enterExtendsInterfaces(ExtendsInterfacesContext ctx) {
        code.append(": ");
    }

    @Override
    public void enterInterfaceBody(InterfaceBodyContext ctx) {
        code.append(" {\n");
    }

    @Override
    public void exitInterfaceBody(InterfaceBodyContext ctx) {
        code.append("}\n");
    }

}
