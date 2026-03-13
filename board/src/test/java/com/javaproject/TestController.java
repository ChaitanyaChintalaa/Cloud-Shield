package com.javaproject;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;
import com.javaproject.database.DatabaseAccess;

@SpringBootTest
@AutoConfigureMockMvc
class TestController {

    private DatabaseAccess da;
    private MockMvc mockMvc;

    @Autowired
    public void setDatabase(DatabaseAccess da) {
        this.da = da;
    }

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    public void testRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    public void testPlayPage() throws Exception {
        mockMvc.perform(get("/play"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/secured/play"));
    }

    @Test
    public void testSecuredPlayRequiresLogin() throws Exception {
        mockMvc.perform(get("/secured/play"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    public void testAddBoardGame() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        requestParams.add("name", "onecard");
        requestParams.add("level", "1");
        requestParams.add("minPlayers", "2");
        requestParams.add("maxPlayers", "+");
        requestParams.add("gameType", "Party Game");

        int origSize = da.getBoardGames().size();
        mockMvc.perform(post("/boardgameAdded").params(requestParams))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andDo(print());
        int newSize = da.getBoardGames().size();
        assertEquals(newSize, origSize + 1);
    }

    @Test
    public void testEditReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        if (reviews.isEmpty()) {
            Review seedReview = new Review();
            seedReview.setGameId(boardgameId);
            seedReview.setText("Seed review for edit test");
            da.addReview(seedReview);
            reviews = da.getReviews(boardgameId);
        }
        Review review = reviews.get(0);
        Long reviewId = review.getId();

        review.setText("Edited text");

        mockMvc.perform(post("/reviewAdded").flashAttr("review", review))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/boardgames/" + review.getGameId() + "/reviews"));

        review = da.getReview(reviewId);
        assertEquals(review.getText(), "Edited text");
    }

    @Test
    public void testDeleteReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        if (reviews.isEmpty()) {
            Review seedReview = new Review();
            seedReview.setGameId(boardgameId);
            seedReview.setText("Seed review for delete test");
            da.addReview(seedReview);
            reviews = da.getReviews(boardgameId);
        }
        Long reviewId = reviews.get(0).getId();

        int origSize = reviews.size();

        mockMvc.perform(get("/deleteReview/{id}", reviewId))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/boardgames/" + boardgameId + "/reviews"));

        int newSize = da.getReviews(boardgameId).size();

        assertEquals(newSize, origSize - 1);
    }

    @Test
    public void testScoreboardApiRequiresLogin() throws Exception {
        mockMvc.perform(get("/secured/api/scores"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    public void testRecordRankedScore() throws Exception {
        da.clearGameScores();
        mockMvc.perform(post("/secured/api/scores")
                .with(user("ranked-player").roles("USER"))
                .contentType("application/json")
                .content("{\"gameKey\":\"ttt\",\"outcome\":\"win\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.username").value("ranked-player"))
                .andExpect(jsonPath("$.summary.totalPoints").value(3))
                .andExpect(jsonPath("$.summary.totalWins").value(1))
                .andExpect(jsonPath("$.games[0].gameKey").value("ttt"))
                .andExpect(jsonPath("$.leaderboard[0].username").value("ranked-player"));
    }
}
