package com.segmentengine.dsl;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                i++;
                while (i < input.length()) {
                    char n = input.charAt(i);
                    if (Character.isLetterOrDigit(n) || n == '_') {
                        i++;
                    } else {
                        break;
                    }
                }
                String word = input.substring(start, i);
                String upper = word.toUpperCase();
                TokenType type = switch (upper) {
                    case "AND" -> TokenType.AND;
                    case "OR" -> TokenType.OR;
                    case "TRUE" -> TokenType.TRUE;
                    case "FALSE" -> TokenType.FALSE;
                    default -> TokenType.IDENTIFIER;
                };
                tokens.add(new Token(type, word, start));
                continue;
            }
            if (Character.isDigit(c)) {
                int start = i;
                i++;
                boolean dotSeen = false;
                while (i < input.length()) {
                    char n = input.charAt(i);
                    if (Character.isDigit(n)) {
                        i++;
                        continue;
                    }
                    if (n == '.' && !dotSeen) {
                        dotSeen = true;
                        i++;
                        continue;
                    }
                    break;
                }
                tokens.add(new Token(TokenType.NUMBER, input.substring(start, i), start));
                continue;
            }
            if (c == '(') {
                tokens.add(new Token(TokenType.LPAREN, "(", i));
                i++;
                continue;
            }
            if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")", i));
                i++;
                continue;
            }
            if (i + 1 < input.length()) {
                String two = input.substring(i, i + 2);
                if (two.equals(">=") || two.equals("<=") || two.equals("==") || two.equals("!=")) {
                    tokens.add(new Token(TokenType.OPERATOR, two, i));
                    i += 2;
                    continue;
                }
            }
            if (c == '>' || c == '<') {
                tokens.add(new Token(TokenType.OPERATOR, Character.toString(c), i));
                i++;
                continue;
            }
            throw new ParseException("Unexpected character '" + c + "' at position " + i);
        }
        tokens.add(new Token(TokenType.EOF, "", input.length()));
        return tokens;
    }
}
