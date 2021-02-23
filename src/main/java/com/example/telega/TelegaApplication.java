package com.example.telega;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.generics.BotSession;

@SpringBootApplication
public class TelegaApplication implements CommandLineRunner {

    @Autowired
    private MyAmazingBot bot;
    @Autowired
    private MyDataBot dataBot;

    static {
        ApiContextInitializer.init();
    }

    @Override
    public void run(String... args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi();
        final BotSession botSession = botsApi.registerBot(bot);
        final BotSession botSession1 = botsApi.registerBot(dataBot);
    }

    public static void main(String[] args) throws Exception {
        final ConfigurableApplicationContext context = SpringApplication.run(TelegaApplication.class, args);
    }

}
