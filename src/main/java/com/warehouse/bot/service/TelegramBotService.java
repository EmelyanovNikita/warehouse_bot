package com.warehouse.bot.service;

import com.warehouse.bot.config.BotConfig;
import com.warehouse.bot.handler.CommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot
{

    private final BotConfig botConfig;
    private final CommandHandler commandHandler;

    public TelegramBotService(BotConfig botConfig, CommandHandler commandHandler)
    {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.commandHandler = commandHandler;
        log.info("🤖 Telegram Bot Service initialized with bot: {}", botConfig.getBotUsername());
    }

    @Override
    public void onUpdateReceived(Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText())
        {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getFrom().getUserName();

            log.info("📨 Received message: '{}' from @{} (chatId: {})", messageText, userName, chatId);

            try
            {
                String response = commandHandler.handleCommand(messageText, chatId);
                sendMessage(chatId, response);
                log.info("✅ Sent response to @{} (chatId: {})", userName, chatId);
            } catch (Exception e) {
                log.error("❌ Error processing message from @{}: {}", userName, e.getMessage());
                sendMessage(chatId, "❌ An error occurred while processing your request. Please try again.");
            }
        }
    }

    private void sendMessage(Long chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("⚠️ Attempted to send empty message to chatId: {}", chatId);
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
            log.debug("📤 Message sent successfully to chatId: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Failed to send message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }
}