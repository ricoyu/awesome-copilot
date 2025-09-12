package com.awesomecopilot.algorithm.dafuweng;

public interface GameStrategy {
    // 决定每次投掷的最大数值范围
    int chooseMaxDiceValue(Player player, Node[] nodes, Player[] players);
}
