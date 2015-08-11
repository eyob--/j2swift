/**
 * Actual "behind-the-scenes" java to swift converter that processes
 * the parse tree for the java file and gives back swift code
 * @author Eyob Tsegaye
 */
public class J2SwiftListener extends Java8BaseListener {

    private String code = "";

    /**
     * Returns the swift code to be outputted to a file, or the empty string if
     * the tree hasn't been walked with this listener.
     * @return swift code as a giant String
     */
    public String swiftCode() {
        return code.isEmpty() ? code : "import Foundation\n\n" + code;
    }

}
