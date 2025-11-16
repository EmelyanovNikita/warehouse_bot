package com.warehouse.bot.config;

import com.warehouse.bot.service.TelegramBotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.bot.username}")
    private String botUsername;
    
    @Value("${warehouse.service.url}")
    private String warehouseServiceUrl;
    
    // Getters
    public String getBotToken() { return botToken; }
    public String getBotUsername() { return botUsername; }
    public String getWarehouseServiceUrl() { return warehouseServiceUrl; }
    
    /**
     * Register the Telegram bot manually
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) {
        try
        {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            System.out.println("✅ Telegram Bot registered successfully: " + botUsername);
            return botsApi;
        } catch (TelegramApiException e) {
            System.err.println("❌ Failed to register Telegram bot: " + e.getMessage());
            throw new RuntimeException("Failed to register Telegram bot", e);
        }
    }
    
    /**
     * Bot options configuration (optional)
     */
    @Bean
    public DefaultBotOptions botOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        options.setMaxThreads(4);
        return options;
    }
}