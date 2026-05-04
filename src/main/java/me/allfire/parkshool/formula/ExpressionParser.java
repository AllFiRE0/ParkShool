package ml.allfire.parkshool.formula;

import java.util.*;

public class ExpressionParser {

    private final String expression;
    private int pos = -1;
    private int ch;

    private static final Map<String, Double> CONSTANTS = new HashMap<>();

    static {
        // Константы из config.yml будут загружены при инициализации
        CONSTANTS.put("PI", Math.PI);
        CONSTANTS.put("E", Math.E);
    }

    public ExpressionParser(String expression) {
        this.expression = expression.trim();
    }

    public double parse() {
        nextChar();
        double result = parseExpression();
        if (pos < expression.length()) {
            throw new RuntimeException("Неожиданный символ: " + (char) ch + " на позиции " + pos);
        }
        return result;
    }

    private void nextChar() {
        pos++;
        if (pos < expression.length()) {
            ch = expression.charAt(pos);
        } else {
            ch = -1;
        }
    }

    private boolean eat(int charToEat) {
        while (ch == ' ') nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private double parseExpression() {
        double x = parseTerm();
        while (true) {
            if (eat('+')) x += parseTerm();
            else if (eat('-')) x -= parseTerm();
            else return x;
        }
    }

    private double parseTerm() {
        double x = parseFactor();
        while (true) {
            if (eat('*')) x *= parseFactor();
            else if (eat('/')) x /= parseFactor();
            else if (eat('%')) x %= parseFactor();
            else return x;
        }
    }

    private double parseFactor() {
        if (eat('+')) return parseFactor();
        if (eat('-')) return -parseFactor();

        double x;
        int startPos = this.pos;

        if (eat('(')) {
            x = parseExpression();
            eat(')');
        } else if (ch >= '0' && ch <= '9' || ch == '.') {
            StringBuilder sb = new StringBuilder();
            while ((ch >= '0' && ch <= '9') || ch == '.') {
                sb.append((char) ch);
                nextChar();
            }
            x = Double.parseDouble(sb.toString());
        } else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
            StringBuilder funcName = new StringBuilder();
            while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                funcName.append((char) ch);
                nextChar();
            }
            String name = funcName.toString().toLowerCase();

            if (eat('(')) {
                if (name.equals("floor")) {
                    x = Math.floor(parseExpression());
                } else if (name.equals("ceil")) {
                    x = Math.ceil(parseExpression());
                } else if (name.equals("round")) {
                    x = Math.round(parseExpression());
                } else if (name.equals("abs")) {
                    x = Math.abs(parseExpression());
                } else if (name.equals("min")) {
                    double a = parseExpression();
                    eat(',');
                    double b = parseExpression();
                    x = Math.min(a, b);
                } else if (name.equals("max")) {
                    double a = parseExpression();
                    eat(',');
                    double b = parseExpression();
                    x = Math.max(a, b);
                } else {
                    throw new RuntimeException("Неизвестная функция: " + name);
                }
                eat(')');
            } else {
                // Попытка найти константу
                if (CONSTANTS.containsKey(name.toUpperCase())) {
                    x = CONSTANTS.get(name.toUpperCase());
                } else {
                    throw new RuntimeException("Неизвестный идентификатор: " + name);
                }
            }
        } else {
            throw new RuntimeException("Неожиданный символ: " + (char) ch + " на позиции " + pos);
        }

        return x;
    }
}
