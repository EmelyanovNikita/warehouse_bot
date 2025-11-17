package com.warehouse.bot.service;

import com.warehouse.bot.config.BotConfig;
import com.warehouse.bot.handler.CommandHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
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
                String responseText = commandHandler.handleCommand(messageText, chatId);
                sendMessageWithKeyboard(chatId, responseText, messageText);
                log.info("✅ Sent response to @{} (chatId: {})", userName, chatId);
            }
            catch (Exception e)
            {
                log.error("❌ Error processing message from @{}: {}", userName, e.getMessage());
                sendMessage(chatId, "❌ An error occurred while processing your request. Please try again.");
            }
        }
    }

    /**
     * Send message with appropriate keyboard based on context
     */
    private void sendMessageWithKeyboard(Long chatId, String text, String userMessage)
    {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        
        // Set keyboard based on the user's message and chat context
        ReplyKeyboardMarkup keyboard = createKeyboardForMessage(userMessage, chatId);
        message.setReplyMarkup(keyboard);
        
        try
        {
            execute(message);
        }
        catch (TelegramApiException e)
        {
            log.error("❌ Failed to send message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    // Keyboard creation methods in TelegramBotService
    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📦 Get products");
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("➕ Add new products");
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("✏️ Update products");
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup getProductsSubMenuKeyboard()
    {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("All products");
        row1.add("Products by ID");
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Search by filter");
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("🔙 Back to Main Menu");
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup getAddProductsSubMenuKeyboard()
    {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Add new Thermal mug");
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔙 Back to Main Menu");
        keyboard.add(row2);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup getUpdateProductsSubMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Update thermal mug by ID");
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Update quantity of reserved product");
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("Update product quantity in stock");
        keyboard.add(row3);
        
        KeyboardRow row4 = new KeyboardRow();
        row4.add("🔙 Back to Main Menu");
        keyboard.add(row4);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    // Keep your original sendMessage for simple text responses
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Failed to send message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    private ReplyKeyboardMarkup createKeyboardForMessage(String userMessage, Long chatId)
    {
        // Check if user is in stock update flow
        String userState = commandHandler.getUserState(chatId); // You'll need to add a getter for this
        
        if (userState != null && userState.startsWith("AWAITING_STOCK_")) {
            return createCancelKeyboard();
        }
        
        switch (userMessage) {
            case "/start":
            case "🔙 Back to Main Menu":
            case "❌ Cancel":
                return createMainMenuKeyboard();
                
            case "📦 Get products":
                return createProductsSubMenuKeyboard();
                
            case "➕ Add new products":
                return createAddProductsSubMenuKeyboard();
                
            case "✏️ Update products":
                return createUpdateProductsSubMenuKeyboard();
                
            case "All products":
            case "➡️ Next page":
            case "⬅️ Previous page":
                return createPaginationKeyboard(chatId);
                
            default:
                return createMainMenuKeyboard();
        }
    }

    /**
     * Create cancel keyboard for step-by-step flows
     */
    private ReplyKeyboardMarkup createCancelKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row = new KeyboardRow();
        row.add("❌ Cancel");
        keyboard.add(row);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create pagination keyboard with Next/Previous buttons
     */
    private ReplyKeyboardMarkup createPaginationKeyboard(Long chatId)
    {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        // Pagination row
        KeyboardRow paginationRow = new KeyboardRow();
        paginationRow.add("⬅️ Previous page");
        paginationRow.add("➡️ Next page");
        keyboard.add(paginationRow);
        
        // Navigation row
        KeyboardRow navRow = new KeyboardRow();
        navRow.add("🔙 Back to Main Menu");
        navRow.add("📦 Get products");
        keyboard.add(navRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    // YOUR EXISTING KEYBOARD METHODS REMAIN THE SAME:
    private ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📦 Get products");
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("➕ Add new products");
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("✏️ Update products");
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup createProductsSubMenuKeyboard()
    {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("All products");
        row1.add("Products by ID");
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Search by filter");
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("🔙 Back to Main Menu");
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup createAddProductsSubMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        // Row 1: Add thermocup
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Add new Thermal mug");
        keyboard.add(row1);
        
        // Row 2: Back button
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔙 Back to Main Menu");
        keyboard.add(row2);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup createUpdateProductsSubMenuKeyboard()
    {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        // Row 1: Update thermocup
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Update thermal mug by ID");
        keyboard.add(row1);
        
        // Row 2: Update reserved quantity
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Update quantity of reserved product");
        keyboard.add(row2);
        
        // Row 3: Update stock quantity
        KeyboardRow row3 = new KeyboardRow();
        row3.add("Update product quantity in stock");
        keyboard.add(row3);
        
        // Row 4: Back button
        KeyboardRow row4 = new KeyboardRow();
        row4.add("🔙 Back to Main Menu");
        keyboard.add(row4);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }
}