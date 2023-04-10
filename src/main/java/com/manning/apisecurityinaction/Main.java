package com.manning.apisecurityinaction;

import com.manning.apisecurityinaction.controller.SpaceController;
import org.dalesbred.Database;
import org.dalesbred.result.EmptyResultException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static spark.Spark.*;

public class Main {

    public static void main(String... args) throws Exception {
        JdbcConnectionPool datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password");
        Database database = Database.forDataSource(datasource);
        createTables(database);
        datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter_api_user", "password");
        database = Database.forDataSource(datasource);

        SpaceController spaceController = new SpaceController(database);
        post("/spaces", spaceController::createSpace);

        after((request, response) -> {
            response.type("aplication/json");
        });

        internalServerError(new JSONObject()
                .put("error", "internal server error").toString());
        notFound(new JSONObject()
                .put("error", "not found").toString());

        exception(IllegalArgumentException.class, Main::badRequest);
        exception(JSONException.class, Main::badRequest);
        exception(EmptyResultException.class,
                (e, request, responce) -> responce.status(404));
    }

    private static void createTables(Database database) throws Exception {
        Path path = Paths.get(Main.class.getResource("/schema.sql").toURI());
        database.update(Files.readString(path));
    }

    private static void badRequest(Exception ex, Request request, Response response) {
        response.status(400);
        response.body("{\"error\": \"" + ex + "\"}");
    }
}
