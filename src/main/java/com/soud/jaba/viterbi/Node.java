package com.soud.jaba.viterbi;

/**
 * @author Soud
 */
class Node {
    private Character value;
    private Node parent;

    Node(Character value, Node parent) {
        this.value = value;
        this.parent = parent;
    }

    Character getValue() {
        return value;
    }

    Node getParent() {
        return parent;
    }
}