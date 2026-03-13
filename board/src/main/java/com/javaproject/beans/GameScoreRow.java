package com.javaproject.beans;

import lombok.Data;

@Data
public class GameScoreRow {

    private String gameKey;
    private int wins;
    private int losses;
    private int draws;
    private int points;
    private int gamesPlayed;
}
