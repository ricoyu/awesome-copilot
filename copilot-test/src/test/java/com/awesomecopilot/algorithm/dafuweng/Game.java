package com.awesomecopilot.algorithm.dafuweng;

import java.util.*;

public class Game {
    private Node[] nodes;
    private Player[] players;
    private int[] throwOrder;
    private GameStrategy strategy;
    private int roundNumber;

    public Game(Node[] nodes, Player[] players, GameStrategy strategy) {
        this.nodes = nodes;
        this.players = players;
        this.strategy = strategy;
        this.throwOrder = generateRandomThrowOrder(players.length);
        this.roundNumber = 0;
    }

    // 生成随机的投掷顺序
    private int[] generateRandomThrowOrder(int playerCount) {
        List<Integer> orderList = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            orderList.add(i);
        }
        Collections.shuffle(orderList);
        return orderList.stream().mapToInt(Integer::intValue).toArray();
    }

    // 开始游戏
    public void start() {
        System.out.println("赛道顺序：");
        for (int i = 1; i < nodes.length; i++) {
            System.out.print(nodes[i] + (i < nodes.length - 1 ? "、" : "\n\n"));
        }

        // 输出初始状态
        System.out.println("0：" + getPlayersStatus());
        System.out.println("投掷顺序（" + getThrowOrderDescription() + "）");

        // 游戏主循环，直到所有玩家都到达终点
        while (!allPlayersWon()) {
            roundNumber++;
            System.out.println("第" + roundNumber + "轮：");
            
            // 按顺序进行投掷
            for (int playerIndex : throwOrder) {
                Player player = players[playerIndex];
                if (player.hasWon()) {
                    System.out.println(player.getName() + "：已经到达终点");
                    continue;
                }
                
                // 处理C节点的无效投掷
                if (player.isSkipNextThrow()) {
                    System.out.println(player.getName() + "：投掷无效一次");
                    player.setSkipNextThrow(false);
                    continue;
                }
                
                // 玩家选择骰子最大值
                int maxDice = strategy.chooseMaxDiceValue(player, nodes, players);
                // 投掷骰子
                int diceResult = Dice.roll(maxDice);
                player.incrementThrowCount();
                
                // 处理移动
                int newPosition = processMovement(player, diceResult);
                
                // 输出本轮结果
                System.out.println(player.getName() + "：投掷选择" + maxDice + "，结果" + diceResult + 
                                   "，" + getMovementDescription(player.getPosition(), newPosition));
                
                // 更新玩家位置
                player.setPosition(newPosition);
                
                // 检查是否到达终点
                if (newPosition >= nodes.length - 1) {
                    player.setPosition(nodes.length - 1);
                    player.setWon(true);
                    System.out.println(player.getName() + "：到达终点！");
                } else {
                    // 检查是否需要踢回其他玩家
                    checkAndKickOtherPlayers(player, newPosition);
                }
            }
            System.out.println();
        }

        // 输出最终结果
        outputFinalResult();
    }

    // 处理玩家移动，包括节点类型的特殊规则
    private int processMovement(Player player, int diceResult) {
        int currentPos = player.getPosition();
        int newPos = currentPos + diceResult;
        
        // 处理连续规则应用
        boolean processed;
        do {
            processed = false;
            // 确保不超过终点
            if (newPos >= nodes.length - 1) {
                break;
            }
            
            NodeType type = nodes[newPos].getType();
            int prevPos = newPos;
            
            // 处理节点规则
            newPos = type.processStep(currentPos, diceResult, nodes, 
                                     Arrays.asList(players).indexOf(player), players);
            
            // 如果位置发生变化，需要重新检查新位置的规则
            if (newPos != prevPos) {
                processed = true;
                currentPos = prevPos; // 更新当前位置为上一步处理后的位置
            }
            
            // 处理C节点的特殊情况（需要跳过下一次投掷）
            if (type == NodeType.C) {
                player.setSkipNextThrow(true);
            }
            
            // 记录E节点位置
            if (type == NodeType.E) {
                player.setLastEPosition(newPos);
            }
            
        } while (processed && newPos < nodes.length - 1);
        
        return newPos;
    }

    // 检查并踢回其他玩家
    private void checkAndKickOtherPlayers(Player currentPlayer, int position) {
        for (Player player : players) {
            if (!player.equals(currentPlayer) && !player.hasWon() && player.getPosition() == position) {
                System.out.println(currentPlayer.getName() + "：将" + player.getName() + "踢回至节点1");
                player.setPosition(1);
            }
        }
    }

    // 获取玩家状态描述
    private String getPlayersStatus() {
        StringBuilder sb = new StringBuilder();
        for (Player player : players) {
            sb.append(player.getName()).append("在节点").append(player.getPosition()).append("，");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    // 获取投掷顺序描述
    private String getThrowOrderDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < throwOrder.length; i++) {
            sb.append(players[throwOrder[i]].getName());
            if (i < throwOrder.length - 1) {
                sb.append("，");
            }
        }
        return sb.toString();
    }

    // 获取移动描述
    private String getMovementDescription(int from, int to) {
        if (to >= nodes.length - 1) {
            return "前进至节点" + (nodes.length - 1);
        }
        
        if (from < to) {
            return "前进至节点" + to;
        } else if (from > to) {
            return "回退至节点" + to;
        } else {
            return "停留在节点" + to;
        }
    }

    // 检查是否所有玩家都已获胜
    private boolean allPlayersWon() {
        for (Player player : players) {
            if (!player.hasWon()) {
                return false;
            }
        }
        return true;
    }

    // 输出最终结果
    private void outputFinalResult() {
        System.out.println("最终结果：");
        for (Player player : players) {
            System.out.println(player.getName() + "：投掷次数 " + player.getThrowCount());
        }
        
        // 确定获胜者
        List<Player> winners = new ArrayList<>();
        int minThrows = Integer.MAX_VALUE;
        
        for (Player player : players) {
            if (player.getThrowCount() < minThrows) {
                minThrows = player.getThrowCount();
                winners.clear();
                winners.add(player);
            } else if (player.getThrowCount() == minThrows) {
                winners.add(player);
            }
        }
        
        System.out.print("获胜选手：");
        if (winners.size() == players.length) {
            System.out.println("平局");
        } else {
            for (int i = 0; i < winners.size(); i++) {
                System.out.print(winners.get(i).getName());
                if (i < winners.size() - 1) {
                    System.out.print("、");
                }
            }
            System.out.println();
        }
    }
}
