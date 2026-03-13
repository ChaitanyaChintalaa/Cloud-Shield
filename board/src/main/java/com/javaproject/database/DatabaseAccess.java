package com.javaproject.database;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.GameScoreRow;
import com.javaproject.beans.GameScoreSummary;
import com.javaproject.beans.LeaderboardEntry;
import com.javaproject.beans.Review;

@Repository
public class DatabaseAccess {

    // autowired using AllArgsConstructor
    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    public List<String> getAuthorities() {

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();

        String query = "SELECT DISTINCT authority FROM authorities";

        List<String> authorities = jdbc.queryForList(query, namedParameters, String.class);

        return authorities;
    }

    public List<BoardGame> getBoardGames() {

        String query = "SELECT * FROM boardgames ORDER BY id";

        BeanPropertyRowMapper<BoardGame> boardgameMapper = new BeanPropertyRowMapper<>(BoardGame.class);

        List<BoardGame> boardgames = jdbc.query(query, boardgameMapper);
        return boardgames;
    }

    public BoardGame getBoardGame(Long id) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();

        String query = "SELECT * FROM boardgames WHERE id = :id";
        namedParameters.addValue("id", id);
        BeanPropertyRowMapper<BoardGame> boardgameMapper = new BeanPropertyRowMapper<>(BoardGame.class);
        List<BoardGame> boardgames = jdbc.query(query, namedParameters, boardgameMapper);
        if (boardgames.isEmpty()) {
            return null;
        } else {
            return boardgames.get(0);
        }
    }

    public List<Review> getReviews(Long id) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();

        String query = "SELECT * FROM reviews WHERE gameId = :id ORDER BY id";
        namedParameters.addValue("id", id);
        BeanPropertyRowMapper<Review> reviewMapper = new BeanPropertyRowMapper<>(Review.class);
        List<Review> reviews = jdbc.query(query, namedParameters, reviewMapper);
        return reviews;
    }

    public Long addBoardGame(BoardGame boardgame) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        String query = "INSERT INTO boardgames (name, level, minPlayers, maxPlayers, gameType) VALUES (:name, :level, :minPlayers, :maxPlayers, :gameType)";
        namedParameters
                .addValue("name", boardgame.getName())
                .addValue("level", boardgame.getLevel())
                .addValue("minPlayers", boardgame.getMinPlayers())
                .addValue("maxPlayers", boardgame.getMaxPlayers())
                .addValue("gameType", boardgame.getGameType());
        KeyHolder generatedKey = new GeneratedKeyHolder();
        int returnValue = jdbc.update(query, namedParameters, generatedKey);
        Long boardGameId = (Long) generatedKey.getKey();
        return (returnValue > 0) ? boardGameId : 0;
    }

    public int addReview(Review review) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        String query = "INSERT INTO reviews (gameId, text) VALUES (:gameId, :text)";
        namedParameters.addValue("gameId", review.getGameId())
                .addValue("text", review.getText());

        return jdbc.update(query, namedParameters);
    }

    public int deleteReview(Long id) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        String query = "DELETE FROM reviews WHERE id = :id";
        namedParameters.addValue("id", id);
        return jdbc.update(query, namedParameters);
    }

    public Review getReview(Long id) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();

        String query = "SELECT * FROM reviews WHERE id = :id";
        namedParameters.addValue("id", id);
        BeanPropertyRowMapper<Review> reviewMapper = new BeanPropertyRowMapper<>(Review.class);
        List<Review> reviews = jdbc.query(query, namedParameters, reviewMapper);
        if (reviews.isEmpty()) {
            return null;
        } else {
            return reviews.get(0);
        }
    }

    public int editReview(Review review) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();

        String query = "UPDATE reviews SET text = :text "
                + "WHERE id = :id";

        namedParameters
                .addValue("text", review.getText())
                .addValue("id", review.getId());
        return jdbc.update(query, namedParameters);
    }

    public int recordGameResult(String username, String gameKey, String outcome, int points) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("gameKey", gameKey)
                .addValue("wins", "win".equals(outcome) ? 1 : 0)
                .addValue("losses", "loss".equals(outcome) ? 1 : 0)
                .addValue("draws", "draw".equals(outcome) ? 1 : 0)
                .addValue("points", points);

        String updateQuery = "UPDATE game_scores SET "
                + "wins = wins + :wins, "
                + "losses = losses + :losses, "
                + "draws = draws + :draws, "
                + "points = points + :points, "
                + "gamesPlayed = gamesPlayed + 1, "
                + "updatedAt = CURRENT_TIMESTAMP "
                + "WHERE username = :username AND gameKey = :gameKey";
        int updated = jdbc.update(updateQuery, namedParameters);
        if (updated > 0) {
            return updated;
        }

        String insertQuery = "INSERT INTO game_scores "
                + "(username, gameKey, wins, losses, draws, points, gamesPlayed, updatedAt) "
                + "VALUES (:username, :gameKey, :wins, :losses, :draws, :points, 1, CURRENT_TIMESTAMP)";
        return jdbc.update(insertQuery, namedParameters);
    }

    public int clearGameScores() {
        return jdbc.update("DELETE FROM game_scores", new MapSqlParameterSource());
    }

    public GameScoreSummary getUserScoreSummary(String username) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("username", username);
        String query = "SELECT username, "
                + "COALESCE(SUM(points), 0) AS totalPoints, "
                + "COALESCE(SUM(wins), 0) AS totalWins, "
                + "COALESCE(SUM(losses), 0) AS totalLosses, "
                + "COALESCE(SUM(draws), 0) AS totalDraws, "
                + "COALESCE(SUM(gamesPlayed), 0) AS totalGames "
                + "FROM game_scores WHERE username = :username GROUP BY username";
        BeanPropertyRowMapper<GameScoreSummary> mapper = new BeanPropertyRowMapper<>(GameScoreSummary.class);
        List<GameScoreSummary> summaries = jdbc.query(query, namedParameters, mapper);
        if (summaries.isEmpty()) {
            GameScoreSummary summary = new GameScoreSummary();
            summary.setUsername(username);
            return summary;
        }
        return summaries.get(0);
    }

    public List<GameScoreRow> getUserGameScores(String username) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("username", username);
        String query = "SELECT gameKey, wins, losses, draws, points, gamesPlayed "
                + "FROM game_scores WHERE username = :username ORDER BY points DESC, gameKey ASC";
        BeanPropertyRowMapper<GameScoreRow> mapper = new BeanPropertyRowMapper<>(GameScoreRow.class);
        return jdbc.query(query, namedParameters, mapper);
    }

    public List<LeaderboardEntry> getLeaderboard(int limit) {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        int safeLimit = Math.max(1, limit);
        String query = "SELECT username, "
                + "COALESCE(SUM(points), 0) AS totalPoints, "
                + "COALESCE(SUM(wins), 0) AS totalWins, "
                + "COALESCE(SUM(losses), 0) AS totalLosses, "
                + "COALESCE(SUM(draws), 0) AS totalDraws, "
                + "COALESCE(SUM(gamesPlayed), 0) AS totalGames "
                + "FROM game_scores GROUP BY username "
                + "ORDER BY totalPoints DESC, totalWins DESC, totalDraws DESC, username ASC LIMIT " + safeLimit;
        BeanPropertyRowMapper<LeaderboardEntry> mapper = new BeanPropertyRowMapper<>(LeaderboardEntry.class);
        return jdbc.query(query, namedParameters, mapper);
    }
}
