package com.javaproject.beans;

import lombok.Data;

@Data
public class GameResultRequest {

    private String gameKey;
    private String outcome;
}
