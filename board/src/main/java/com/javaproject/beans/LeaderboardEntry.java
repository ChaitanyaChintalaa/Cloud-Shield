package com.javaproject.beans;

import lombok.Data;

@Data
public class LeaderboardEntry {

    private String username;
    private int totalPoints;
    private int totalWins;
    private int totalLosses;
    private int totalDraws;
    private int totalGames;
}
