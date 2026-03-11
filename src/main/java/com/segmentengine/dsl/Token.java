package com.segmentengine.dsl;

public record Token(TokenType type, String text, int position) {
}
