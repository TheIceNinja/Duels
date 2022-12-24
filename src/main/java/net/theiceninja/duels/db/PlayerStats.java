package net.theiceninja.duels.db;

import lombok.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStats implements Listener {

    // this stuff in progress
    private String uuid;
    private int wins;
    private int lose;

    private Database database;

    public void addLose(Player player, Database database) {
        PlayerStats playerStats = getPlayerStatsFromDatabase(player, database);
        playerStats.lose++;
        playerStats.updatePlayerStats(database);
    }

    public void addWin(Player player, Database database) {
        PlayerStats playerStats = getPlayerStatsFromDatabase(player, database);
        playerStats.wins++;
        playerStats.updatePlayerStats(database);
    }

    @SneakyThrows
    public void insertDataToPlayer(Player player, Database database) {
       PreparedStatement statement =  database.insertData("playerStats", "uuid, wins, loses", "?, ?, ?");
       statement.setString(1, player.getUniqueId().toString());
       statement.setInt(2, wins);
       statement.setInt(3, lose);

       statement.executeUpdate();
       statement.close();
    }

    @SneakyThrows
    private void updatePlayerStats(Database database) {
     PreparedStatement statement = database.updateDatabase("playerStats", "wins = ?, loses = ? WHERE uuid = ?");
        statement.setString(1, uuid);
        statement.setInt(2, wins);
        statement.setInt(3, lose);

        statement.executeUpdate();
        statement.close();
    }

    @SneakyThrows
    public PlayerStats findPlayerStatsByUUID(String uuid, Database database) {
        PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM playerStats WHERE uuid = ?");
        statement.setString(1, uuid);
        ResultSet resultSet = statement.executeQuery();
        PlayerStats stats;

        if (resultSet.next()) {
            stats = new PlayerStats(resultSet.getString("uuid"), resultSet.getInt("wins")
            ,resultSet.getInt("loses"), database);

            statement.close();

            return stats;
        }

        statement.close();
        return null;
    }

    public PlayerStats getPlayerStatsFromDatabase(Player player, Database database) {

        PlayerStats stats = findPlayerStatsByUUID(player.getUniqueId().toString(), database);

        if (stats == null) {
            stats = new PlayerStats(player.getUniqueId().toString(), 0, 0, database);
            insertDataToPlayer(player, database);
        }

        return stats;
    }

    public int getStats(Player player, String type, Database database) {

        if (type.equalsIgnoreCase("wins")) return getPlayerStatsFromDatabase(player, database).getWins();
        else if (type.equalsIgnoreCase("loses")) return getPlayerStatsFromDatabase(player, database).getLose();

        return 0;
    }
}
