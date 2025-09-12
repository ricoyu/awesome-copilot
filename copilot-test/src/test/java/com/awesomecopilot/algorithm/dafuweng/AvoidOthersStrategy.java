package com.awesomecopilot.algorithm.dafuweng;

public class AvoidOthersStrategy implements GameStrategy {
    @Override
    public int chooseMaxDiceValue(Player player, Node[] nodes, Player[] players) {
        // 检查前方是否有其他玩家，尽量避开
        int currentPos = player.getPosition();
        
        // 检查前方6步内是否有其他玩家
        for (int i = 1; i <= 6; i++) {
            int targetPos = currentPos + i;
            if (targetPos > nodes.length - 1) {
                continue; // 超出终点，无需考虑
            }
            
            // 如果目标位置有其他玩家，选择较小的最大值来避免
            for (Player p : players) {
                if (!p.equals(player) && !p.hasWon() && p.getPosition() == targetPos) {
                    return i - 1 > 0 ? i - 1 : 1;
                }
            }
        }
        
        // 检查是否能到达有其他玩家的位置，尝试将其踢回起点
        for (int i = 1; i <= 6; i++) {
            int targetPos = currentPos + i;
            if (targetPos > nodes.length - 1) {
                continue;
            }
            
            for (Player p : players) {
                if (!p.equals(player) && !p.hasWon() && p.getPosition() == targetPos) {
                    return i;
                }
            }
        }
        
        // 默认选择6，争取最快到达终点
        return 6;
    }
}
