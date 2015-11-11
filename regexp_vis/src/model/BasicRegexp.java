package model;

import java.util.*;

/**
 * Class representing a "basic" regular expression, with one top level
 * operator. A tree of these can then be used to any regular
 * expression
 *
 * @author Matthew Nicholls
 */
public class BasicRegexp implements Cloneable {
    public enum RegexpOperator {
        NONE,
        STAR,
        PLUS,
        OPTION,
        SEQUENCE,
        CHOICE
    }

    private ArrayList<BasicRegexp> mOperands;
    private char mChar;
    private RegexpOperator mOperator;

    public BasicRegexp(ArrayList<BasicRegexp> operands, RegexpOperator op)
    {
        // NOTE(mjn33): Need to think about this, make sure these
        // checks are right
        if (op == RegexpOperator.NONE) {
            throw new IllegalArgumentException(
                "RegexpOperator.NONE only allowed for single character " +
                "expressions");
        }
        if (isUnaryOperator(op) && operands.size() > 1) {
            throw new IllegalArgumentException(
                "Multiple operands passed for a unary operator");
        }
        if (operands.size() == 0) {
            throw new IllegalArgumentException("No operands passed");
        }

        mOperands = operands;
        mOperator = op;
    }

    public BasicRegexp(BasicRegexp operand, RegexpOperator op)
    {
        // NOTE(mjn33): Need to think about this, make sure these
        // checks are right
        if (op == RegexpOperator.NONE) {
            throw new IllegalArgumentException(
                "RegexpOperator.NONE only allowed for single character " +
                "expressions");
        }

        mOperands = new ArrayList<>();
        mOperands.add(operand);
        mOperator = op;
    }

    public BasicRegexp(char c, RegexpOperator op)
    {
        // NOTE(mjn33): Need to think about this, make sure these
        // checks are right
        if (!isUnaryOperator(op)) {
            throw new IllegalArgumentException(
                "Non-unary operators require multiple operands");
        }
        mChar = c;
        mOperator = op;
    }

    public boolean isSingleChar()
    {
        return mOperands == null;
    }

    public RegexpOperator getOperator()
    {
        return mOperator;
    }

    public List<BasicRegexp> getOperands()
    {
        if (isSingleChar()) {
            throw new RuntimeException(
                "Cannot call getOperands() on a single character expression");
        }

        return Collections.unmodifiableList(mOperands);
    }

    public char getChar()
    {
        if (!isSingleChar()) {
            throw new RuntimeException(
                "getChar() must only be called on single character " +
                "expressions");
        }

        return mChar;
    }

    public void setOperator(RegexpOperator op)
    {
        // NOTE(mjn33): Need to think about this, make sure these
        // checks are right
        if (op == RegexpOperator.NONE && !isSingleChar()) {
            throw new IllegalArgumentException(
                "RegexpOperator.NONE only allowed for single character " +
                "expressions");
        }
        if (isUnaryOperator(op) && mOperands.size() > 1) {
            throw new IllegalArgumentException(
                "Multiple operands passed for a unary operator");
        }

        mOperator = op;
    }

    /**
     * Performs a deep copy of this BasicRegexp, all sub expressions
     * are copied.
     */
    public BasicRegexp clone()
    {
        ArrayList<BasicRegexp> newOperands = null;
        // Deep copy operands if we have any
        if (mOperands != null) {
            newOperands = new ArrayList<>();
            for (BasicRegexp operand : mOperands) {
                newOperands.add(operand.clone());
            }
            return new BasicRegexp(newOperands, mOperator);
        }

        return new BasicRegexp(mChar, mOperator);
    }

    /**
     * Find the matching closing parenthesis for the parenthesis at
     * the start of this string
     *
     * <b>Precondition:<b> it is assumed that "str" starts with an
     * opening parenthesis
     *
     * @returns: the index of the matching closing parenthesis, -1
     * otherwise if there is no matching closing parenthesis in this
     * string
     */
    private static int findMatchingParenIdx(String str)
    {
        assert(str.charAt(0) == '(');
        int curIdx = 1;
        int parenCount = 1; // 1 open parenthesis at start

        while (curIdx < str.length()) {
            switch (str.charAt(curIdx)) {
            case '(':
                parenCount++;
                break;
            case ')':
                parenCount--;
                break;
            default:
                break;
            }
            if (parenCount == 0) {
                return curIdx;
            }
            curIdx++;
        }

        return -1; // No matching parenthesis found
    }

    /**
     * @param op The operator in question
     * @return True if the specified operator is unary, false
     * otherwise
     */
    public static boolean isUnaryOperator(RegexpOperator op)
    {
        // Only CHOICE and SEQUENCE are not unary
        return op == RegexpOperator.STAR ||
               op == RegexpOperator.PLUS ||
               op == RegexpOperator.OPTION ||
               op == RegexpOperator.NONE;
    }

    /**
     * Factored out of parseRegexp, common code for processing unary
     * operators such as PLUS, STAR and OPTION
     *
     * @param sequenceOperands The current working list of operands in
     * sequence
     * @param op The operator that we are processing
     * @see parseRegexp
     */
    private static void processUnaryOp(ArrayList<BasicRegexp> sequenceOperands,
        RegexpOperator op) throws InvalidRegexpException
    {
        if (!sequenceOperands.isEmpty()) {
            BasicRegexp back = sequenceOperands
                    .remove(sequenceOperands.size() - 1);

            // If the last regexp operand was just a char on its own
            if (back.isSingleChar() && back.getOperator() ==
                RegexpOperator.NONE) {
                back.setOperator(op);
            } else {
                BasicRegexp wrappedRegexp = new BasicRegexp(back, op);

                sequenceOperands.add(wrappedRegexp);
            }
        } else {
            throw new InvalidRegexpException(
                op.name() + " operator on empty word");
        }
    }

    /**
     * Factored out of parseRegexp, common code for processing the
     * choice operator
     *
     * @param sequenceOperands The current working list of operands in
     * sequence
     * @param choiceOperands The current working list of operands for
     * choice
     * @see parseRegexp
     */
    private static void processChoiceOp(ArrayList<BasicRegexp> sequenceOperands,
        ArrayList<BasicRegexp> choiceOperands) throws InvalidRegexpException
    {
        if (sequenceOperands.size() > 1) {
            // Found multiple operands, they will be in sequence
            BasicRegexp re = new BasicRegexp(new ArrayList<>(sequenceOperands),
                RegexpOperator.SEQUENCE);
            // BasicRegexp takes ownership of the old ArrayList
            sequenceOperands.clear();
            choiceOperands.add(re);
        } else if (!sequenceOperands.isEmpty()) {
            // One element
            BasicRegexp back = sequenceOperands.remove(0);
            choiceOperands.add(back);
        } else {
            throw new InvalidRegexpException("CHOICE operator on empty word");
        }
    }

    public static BasicRegexp parseRegexp(String str)
        throws InvalidRegexpException
    {
        ArrayList<BasicRegexp> sequenceOperands = new ArrayList<>();
        ArrayList<BasicRegexp> choiceOperands = new ArrayList<>();
        int idx = 0;

        while (idx < str.length()) {
            switch (str.charAt(idx)) {
            case '(': {
                int parenIdx = findMatchingParenIdx(str.substring(idx));
                if (parenIdx == -1) {
                    throw new InvalidRegexpException(
                        "Unclosed parenthesis found");
                }
                parenIdx += idx; // parenIdx is relative to idx
                BasicRegexp re = parseRegexp(str.substring(idx + 1, parenIdx));
                sequenceOperands.add(re);
                idx = parenIdx;
                break;
            }
            case ')':
                // No last matching opening parenthesis, error
                throw new InvalidRegexpException(
                    "Stray closing parenthesis found");
            case '*':
                processUnaryOp(sequenceOperands, RegexpOperator.STAR);
                break;
            case '+':
                processUnaryOp(sequenceOperands, RegexpOperator.PLUS);
                break;
            case '?':
                processUnaryOp(sequenceOperands, RegexpOperator.OPTION);
                break;
            case '|':
                processChoiceOp(sequenceOperands, choiceOperands);
                break;
            default:
                // Ignore whitespace
                if (!Character.isWhitespace(str.charAt(idx))) {
                    // Normal character
                    BasicRegexp re = new BasicRegexp(str.charAt(idx),
                        RegexpOperator.NONE);
                    sequenceOperands.add(re);
                }
            }
            idx++;
        }

        if (!choiceOperands.isEmpty()) {
            // The remaining sequence operands are part of the choice operation
            processChoiceOp(sequenceOperands, choiceOperands);
            return new BasicRegexp(choiceOperands, RegexpOperator.CHOICE);
        } else if (!sequenceOperands.isEmpty()) {
            return new BasicRegexp(sequenceOperands, RegexpOperator.SEQUENCE);
        } else {
            return null;
        }
    }

    public static void debugPrintBasicRegexp(int indent, BasicRegexp re)
    {
        String indentStr = "";
        for (int i = 0; i < indent; i++) {
            indentStr += "    ";
        }

        String opStr = re.getOperator().name();
        if (re.isSingleChar()) {
            System.out.println(indentStr + "[BasicRegexp:" + opStr + "] char = "
                + re.mChar);
        } else {
            System.out.println(indentStr + "[BasicRegexp:" + opStr + "] {");
            for (int i = 0; i < re.mOperands.size(); i++) {
                debugPrintBasicRegexp(indent + 1, re.mOperands.get(i));
            }
            System.out.println(indentStr + "}");
        }
    }
}
