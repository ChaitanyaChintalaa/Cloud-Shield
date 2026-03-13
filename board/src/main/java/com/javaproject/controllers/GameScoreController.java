package com.javaproject.controllers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.javaproject.beans.GameResultRequest;
import com.javaproject.database.DatabaseAccess;

@RestController
@RequestMapping("/secured/api/scores")
public class GameScoreController {

    private static final List<String> VALID_GAMES = Arrays.asList("ttt", "c4", "memory", "rps", "guess");
    private static final List<String> VALID_OUTCOMES = Arrays.asList("win", "loss", "draw");

    private final DatabaseAccess da;

    public GameScoreController(DatabaseAccess da) {
        this.da = da;
    }

    @GetMapping
    public Map<String, Object> getScoreboard(Authentication authentication) {
        return buildPayload(authentication.getName());
    }

    @PostMapping
    public Map<String, Object> saveResult(@RequestBody GameResultRequest request, Authentication authentication) {
        if (request == null || !VALID_GAMES.contains(request.getGameKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported game key");
        }
        if (!VALID_OUTCOMES.contains(request.getOutcome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported outcome");
        }

        da.recordGameResult(authentication.getName(), request.getGameKey(), request.getOutcome(),
                calculatePoints(request.getOutcome()));
        return buildPayload(authentication.getName());
    }

    private Map<String, Object> buildPayload(String username) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", da.getUserScoreSummary(username));
        payload.put("games", da.getUserGameScores(username));
        payload.put("leaderboard", da.getLeaderboard(10));
        return payload;
    }

    private int calculatePoints(String outcome) {
        if ("win".equals(outcome)) {
            return 3;
        }
        if ("draw".equals(outcome)) {
            return 1;
        }
        return 0;
    }
}
