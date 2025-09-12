package com.awesomecopilot.algorithm.dafuweng;

public class Node {
    private int position;
    private NodeType type;

    public Node(int position, NodeType type) {
        this.position = position;
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public NodeType getType() {
        return type;
    }

    @Override
    public String toString() {
        return position + "（" + type + "）";
    }
}
