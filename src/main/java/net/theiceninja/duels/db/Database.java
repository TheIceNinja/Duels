package net.theiceninja.duels.db;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;

import java.sql.*;

public class Database {

    @Getter
    private Connection connection;

    private final String url;

    @SneakyThrows
    public Database(String host, String username, String password, String database, int port) {
        url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        connection = DriverManager.getConnection(url, username, password);
        Bukkit.getLogger().info("Database connected!");
    }

    public Connection getConnection() {
        if (connection != null) return connection;
        return null;
    }

    @SneakyThrows
    public void createTable(String name, String execute) {
        Statement statement = getConnection().createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS " + name + "(" + execute + ")";
        statement.execute(sql);
    }

    @SneakyThrows
    public PreparedStatement insertData(String tableName, String values, String question) {
        PreparedStatement statement = getConnection()
                .prepareStatement("INSERT INTO " + tableName + "(" + values + ") VALUES (" + question + ")");
        return statement;
    }

    @SneakyThrows
    public PreparedStatement updateDatabase(String tableName, String execute) {
        PreparedStatement statement = getConnection().prepareStatement("UPDATE " + tableName + " SET " + execute);
        return statement;
    }

}
