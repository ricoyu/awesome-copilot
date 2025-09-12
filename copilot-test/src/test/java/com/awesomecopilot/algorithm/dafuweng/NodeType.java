package com.awesomecopilot.algorithm.dafuweng;

public enum NodeType {
    A {
        @Override
        public int processStep(int currentPosition, int step, Node[] nodes, int playerId, Player[] players) {
            // 正常前进n步
            return currentPosition + step;
        }
    },
    B {
        @Override
        public int processStep(int currentPosition, int step, Node[] nodes, int playerId, Player[] players) {
            // 后退一步
            return currentPosition - 1;
        }
    },
    C {
        @Override
        public int processStep(int currentPosition, int step, Node[] nodes, int playerId, Player[] players) {
            // C节点本身不处理步数，只是标记需要额外投掷一次
            return currentPosition + step;
        }
    },
    D {
        @Override
        public int processStep(int currentPosition, int step, Node[] nodes, int playerId, Player[] players) {
            // 回退到出发节点1
            return 1;
        }
    },
    E {
        @Override
        public int processStep(int currentPosition, int step, Node[] nodes, int playerId, Player[] players) {
            // 回退到上一个E节点，如果是第一个E节点，则按照节点A处理
            int lastEPosition = -1;
            for (int i = currentPosition - 1; i >= 1; i--) {
                if (nodes[i].getType() == E) {
                    lastEPosition = i;
                    break;
                }
            }
            return lastEPosition != -1 ? lastEPosition : currentPosition + step;
        }
    };

    // 处理步数的抽象方法，由各个枚举值实现
    public abstract int processStep(int currentPosition, int step, Node[] nodes, int playerId, Player[] players);
}
