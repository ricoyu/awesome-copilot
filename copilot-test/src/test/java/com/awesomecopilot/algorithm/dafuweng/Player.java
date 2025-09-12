package com.awesomecopilot.algorithm.dafuweng;

public class Player {
    private String name;
    private int position;
    private int throwCount;
    private boolean hasWon;
    private boolean skipNextThrow; // 是否需要跳过下一次投掷（用于C节点）
    private int lastEPosition; // 记录上一个E节点的位置

    public Player(String name) {
        this.name = name;
        this.position = 1; // 从起点1开始
        this.throwCount = 0;
        this.hasWon = false;
        this.skipNextThrow = false;
        this.lastEPosition = -1;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getThrowCount() {
        return throwCount;
    }

    public void incrementThrowCount() {
        this.throwCount++;
    }

    public boolean hasWon() {
        return hasWon;
    }

    public void setWon(boolean hasWon) {
        this.hasWon = hasWon;
    }

    public boolean isSkipNextThrow() {
        return skipNextThrow;
    }

    public void setSkipNextThrow(boolean skipNextThrow) {
        this.skipNextThrow = skipNextThrow;
    }

    public int getLastEPosition() {
        return lastEPosition;
    }

    public void setLastEPosition(int lastEPosition) {
        this.lastEPosition = lastEPosition;
    }
}
