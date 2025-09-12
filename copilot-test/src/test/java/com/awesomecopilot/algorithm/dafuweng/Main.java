package com.awesomecopilot.algorithm.dafuweng;

public class Main {
    public static void main(String[] args) {
        // 创建赛道节点（1-20号节点）
        Node[] nodes = new Node[21]; // 索引0 unused，节点1-20对应索引1-20
        nodes[1] = new Node(1, NodeType.A);
        nodes[2] = new Node(2, NodeType.A);
        nodes[3] = new Node(3, NodeType.B);
        nodes[4] = new Node(4, NodeType.A);
        nodes[5] = new Node(5, NodeType.C);
        nodes[6] = new Node(6, NodeType.A);
        nodes[7] = new Node(7, NodeType.A);
        nodes[8] = new Node(8, NodeType.A);
        nodes[9] = new Node(9, NodeType.A);
        nodes[10] = new Node(10, NodeType.D);
        nodes[11] = new Node(11, NodeType.E);
        nodes[12] = new Node(12, NodeType.A);
        nodes[13] = new Node(13, NodeType.A);
        nodes[14] = new Node(14, NodeType.A);
        nodes[15] = new Node(15, NodeType.A);
        nodes[16] = new Node(16, NodeType.E);
        nodes[17] = new Node(17, NodeType.B);
        nodes[18] = new Node(18, NodeType.A);
        nodes[19] = new Node(19, NodeType.C);
        nodes[20] = new Node(20, NodeType.A);
        
        // 创建玩家
        Player[] players = {
            new Player("甲"),
            new Player("乙"),
            new Player("丙")
        };
        
        // 创建游戏并开始
        Game game = new Game(nodes, players, new AvoidOthersStrategy());
        game.start();
    }
}
