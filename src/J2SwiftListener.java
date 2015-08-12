import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Actual "behind-the-scenes" java to swift converter that processes
 * the parse tree for the java file and gives back swift code
 * @author Eyob Tsegaye
 */
public class J2SwiftListener extends Java8BaseListener {

    private static Map<String, String> typeMap = new HashMap<>();

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

    private void exitNonTranslatable(String message, ParserRuleContext ctx) {
        System.err.println("Error! Encountered non-translatable: " + message + " \""
                    + ctx.getParent().getText() + "\"");
        System.exit(1);
    }

    public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
        // TODO class modifiers, type parameters
        code.append("\nclass ").append(ctx.Identifier().toString());
    }

    public void enterSuperclass(Java8Parser.SuperclassContext ctx) {
        code.append(": ");
    }

    public void enterSuperinterfaces(Java8Parser.SuperinterfacesContext ctx) {
        boolean superClassExists = ctx.getParent() instanceof Java8Parser.NormalClassDeclarationContext
                    && ((Java8Parser.NormalClassDeclarationContext) ctx.getParent()).superclass() != null;
        if (superClassExists) {
            code.append(", ");
        }
        else {
            code.append(": ");
        }
    }

    public void exitInterfaceType(Java8Parser.InterfaceTypeContext ctx) {
        ParserRuleContext parent = ctx.getParent();
        if (parent instanceof Java8Parser.InterfaceTypeListContext) {
            List<Java8Parser.InterfaceTypeContext> interfaceList =
                        ((Java8Parser.InterfaceTypeListContext) parent).interfaceType();
            if (interfaceList.get(interfaceList.size()-1) != ctx) {
                // if this isn't the last interface in a list, put a comma at the end
                code.append(", ");
            }
        }
    }

    public void exitClassType(Java8Parser.ClassTypeContext ctx) {
        // the identifier is usually printed in enterTypeArguments()
        if (ctx.typeArguments() == null)
            code.append(ctx.Identifier());
    }

    public void enterClassBody(Java8Parser.ClassBodyContext ctx) {
        code.append(" {\n");
    }

    public void exitClassBody(Java8Parser.ClassBodyContext ctx) {
        code.append("\n}");
    }

    public void enterTypeParameters(Java8Parser.TypeParametersContext ctx) {
        code.append("<");
    }

    public void exitTypeParameters(Java8Parser.TypeParametersContext ctx) {
        code.append(">");
    }

    public void enterTypeParameter(Java8Parser.TypeParameterContext ctx) {
        code.append(ctx.Identifier());
    }

    public void exitTypeParameter(Java8Parser.TypeParameterContext ctx) {
        List<Java8Parser.TypeParameterContext> typeParameterList =
                    ((Java8Parser.TypeParameterListContext) ctx.getParent()).typeParameter();
        if (typeParameterList.get(typeParameterList.size()-1) != ctx) {
            code.append(", ");
        }
    }

    public void enterTypeArguments(Java8Parser.TypeArgumentsContext ctx) {
        // print out class identifier if these are the type arguments for a class
        if (ctx.getParent() instanceof Java8Parser.ClassTypeContext) {
            Java8Parser.ClassTypeContext parent = (Java8Parser.ClassTypeContext) ctx.getParent();
            if (parent.classOrInterfaceType() != null) {
                code.append(".").append(parent.Identifier());
            }
        }

        code.append("<");
    }

    public void exitTypeArguments(Java8Parser.TypeArgumentsContext ctx) {
        code.append(">");
    }

    public void exitTypeArgument(Java8Parser.TypeArgumentContext ctx) {
        List<Java8Parser.TypeArgumentContext> typeArgumentList =
                    ((Java8Parser.TypeArgumentListContext) ctx.getParent()).typeArgument();

        if (typeArgumentList.get(typeArgumentList.size()-1) != ctx) {
            code.append(", ");
        }
    }

    public void enterTypeBound(Java8Parser.TypeBoundContext ctx) {
        code.append(": ");
    }

    public void enterTypeVariable(Java8Parser.TypeVariableContext ctx) {
        code.append(ctx.Identifier());
    }

    public void enterAdditionalBound(Java8Parser.AdditionalBoundContext ctx) {
        exitNonTranslatable("additional type bound", ctx);
    }

    public void enterClassType_lfno_classOrInterfaceType(Java8Parser.ClassType_lfno_classOrInterfaceTypeContext ctx) {
        code.append(ctx.Identifier());
    }

    public void enterClassType_lf_classOrInterfaceType(Java8Parser.ClassType_lf_classOrInterfaceTypeContext ctx) {
        code.append('.').append(ctx.Identifier());
    }

    public void enterWildcard(Java8Parser.WildcardContext ctx) {
        exitNonTranslatable("wildcard", ctx);
    }

}
