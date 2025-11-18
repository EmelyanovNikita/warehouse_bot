// handler/CommandHandler.java
package com.warehouse.bot.handler;

import com.warehouse.bot.model.Product;
import com.warehouse.bot.model.ProductWithAttributes;
import com.warehouse.bot.model.ServerAttributes;
import com.warehouse.bot.model.ThermocupAttributes;
import com.warehouse.bot.service.WarehouseApiService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CommandHandler
{

    private static final String AWAITING_STOCK_PRODUCT_ID = "AWAITING_STOCK_PRODUCT_ID";
    private static final String AWAITING_STOCK_WAREHOUSE_ID = "AWAITING_STOCK_WAREHOUSE_ID";
    private static final String AWAITING_STOCK_QUANTITY = "AWAITING_STOCK_QUANTITY";
    private static final String AWAITING_RESERVED_PRODUCT_ID = "AWAITING_RESERVED_PRODUCT_ID";
    private static final String AWAITING_RESERVED_QUANTITY = "AWAITING_RESERVED_QUANTITY";

    private Map<Long, Long> reservedProductIdCache = new HashMap<>();

    private final WarehouseApiService warehouseApiService;
    private Map<Long, String> userStates = new HashMap<>();

    // Pagination state
    private Map<Long, Integer> userPageStates = new HashMap<>(); // chatId -> current page
    private Map<Long, List<Product>> userProductCache = new HashMap<>(); // chatId -> cached products

    private Map<Long, Long> stockProductIdCache = new HashMap<>(); // chatId -> productId
    private Map<Long, Integer> stockWarehouseIdCache = new HashMap<>(); // chatId -> warehouseId

    // Constants
    private static final int PRODUCTS_PER_PAGE = 5;

    public CommandHandler(WarehouseApiService warehouseApiService)
    {
        this.warehouseApiService = warehouseApiService;
    }

    public String handleCommand(String message, Long chatId)
    {
        try
        {
            if (userStates.containsKey(chatId))
            {
                return handleState(message, chatId);
            }

            // Handle pagination commands
            if (message.equals("➡️ Next page") || message.equals("⬅️ Previous page"))
            {
                return handlePagination(message, chatId);
            }

            switch (message)
            {
                case "/start":
                    // Clear any existing pagination state
                    userPageStates.remove(chatId);
                    userProductCache.remove(chatId);
                    return getWelcomeMessage();
                    
                case "🔙 Back to Main Menu":
                    return getWelcomeMessage();

                case "📦 Get products":
                    return getProductsMenu();

                case "All products":
                    return getAllProducts(chatId);

                case "Products by ID":
                    userStates.put(chatId, "AWAITING_PRODUCT_ID");
                    return "Please enter the product ID:";
                
                case "Search by filter":
                    userStates.put(chatId, "AWAITING_FILTER_PARAMETERS");
                    return "Please choose params:";
                
                case "Add new products":
                    return getAddProductsMenu();
                
                case "Add new Thermal mug":
                    userStates.put(chatId, "AWAITING_THERMOCUP_CREATE");
                    return getThermocupCreateInstructions();
                
                case "Update products":
                    return getUpdateProductsMenu();
                
                case "Update thermal mug by ID":
                    userStates.put(chatId, "AWAITING_THERMOCUP_UPDATE");
                    return "Please enter thermocup ID and update data in format:\n" +
                           "ID|name|category_id|base_price|SKU|is_active|path_to_photo|volume_ml|color|brand|model|is_hermetic|material\n" +
                           "Example: 123|New Name|1|29.99|SKU123|true|/photos/1.jpg|500|Red|BrandX|ModelY|true|Stainless Steel";

                case "Update quantity of reserved product":
                    reservedProductIdCache.remove(chatId);
                    
                    userStates.put(chatId, AWAITING_RESERVED_PRODUCT_ID);
                    return "📦 Update Reserved Quantity\n\n" +
                        "Please enter the Product ID:";

                case "Update product quantity in stock":
                    stockProductIdCache.remove(chatId);
                    stockWarehouseIdCache.remove(chatId);

                    userStates.put(chatId, AWAITING_STOCK_PRODUCT_ID);
                    return "📦 Update Product Stock Quantity\n\n" +
                        "Please enter the Product ID:";
                
                default:
                    return "Unknown command. Please use the menu buttons or type /start to see available options.";
            }
        }
        catch (Exception e)
        {
            log.error("Error handling command: {}", e.getMessage());
            return "An error occurred while processing your request. Please try again.";
        }
    }

    private String handlePagination(String command, Long chatId)
    {
        Integer currentPage = userPageStates.get(chatId);
        List<Product> products = userProductCache.get(chatId);
        
        if (currentPage == null || products == null)
        {
            return "❌ No products session found. Please select 'All products' again.";
        }
        
        int newPage = currentPage;
        
        if (command.equals("➡️ Next page"))
        {
            newPage++;
        } else if (command.equals("⬅️ Previous page"))
        {
            newPage--;
        }
        
        // Validate new page number
        int totalPages = (int) Math.ceil((double) products.size() / PRODUCTS_PER_PAGE);
        if (newPage < 0) newPage = 0;
        if (newPage >= totalPages) newPage = totalPages - 1;
        
        // Update page state
        userPageStates.put(chatId, newPage);
        
        return formatProductsPage(chatId, newPage);
    }

    private String handleState(String message, Long chatId)
    {
        String state = userStates.get(chatId);
        userStates.remove(chatId);

        try
        {
            switch (state)
            {
                case "AWAITING_PRODUCT_ID":
                    Long productId = Long.parseLong(message.trim());
                    ProductWithAttributes<?> productWithAttrs = warehouseApiService.getProductWithAttributes(productId);
                    return productWithAttrs != null ? formatProduct(productWithAttrs) : "Product not found!";

                case "AWAITING_FILTER_PARAMETERS":
                    Long thermocupId = Long.parseLong(message.trim());
                    ProductWithAttributes<?> THERMOCUPWithAttrs = warehouseApiService.getProductWithAttributes(thermocupId);
                    return THERMOCUPWithAttrs != null ? formatProduct(THERMOCUPWithAttrs) : "productWithAttrs not found!";
                
                case "AWAITING_THERMOCUP_CREATE":
                    return createThermocupFromInput(message);
                
                case "AWAITING_THERMOCUP_UPDATE":
                    // return updateThermocupFromInput(message);
                    return "updateThermocupFromInput not found";
                
                case "AWAITING_RESERVED_UPDATE":
                    return updateReservedQuantityFromInput(message);

                case "AWAITING_STOCK_UPDATE":
                    return updateStockQuantityFromInput(message);

                case AWAITING_STOCK_PRODUCT_ID:
                    return handleStockProductId(message, chatId);
                    
                case AWAITING_STOCK_WAREHOUSE_ID:
                    return handleStockWarehouseId(message, chatId);
                    
                case AWAITING_STOCK_QUANTITY:
                    return handleStockQuantity(message, chatId);

                case AWAITING_RESERVED_PRODUCT_ID:
                    return handleReservedProductId(message, chatId);
                    
                case AWAITING_RESERVED_QUANTITY:
                    return handleReservedQuantity(message, chatId);
                
                default:
                    return "Invalid state. Please start over.";
            }
        }
        catch (NumberFormatException e)
        {
            return "Invalid number format. Please try again with valid numbers.";
        }
        catch (Exception e)
        {
            log.error("Error handling state: {}", e.getMessage());
            return "An error occurred while processing your input. Please try again.";
        }
    }

    private String getWelcomeMessage()
    {
        return "🏭 Welcome to Warehouse Bot! 🏭\n\n" +
               "Please choose an option from the menu:\n\n" +
               "📦 Get products\n" +
               "➕ Add new products\n" +
               "✏️ Update products";
    }

    private String getProductsMenu()
    {
        return "📦 Get Products Menu:\n\n" +
               "• All products\n" +
               "• Products by ID\n" +
               "• Search by filter";
    }

    private String getAddProductsMenu() {
        return "➕ Add New Products:\n\n" +
               "• Add new Thermal mug";
    }

    private String getUpdateProductsMenu() {
        return "✏️ Update Products:\n\n" +
               "• Update thermal mug by ID\n" +
               "• Update quantity of reserved product\n" +
               "• Update product quantity in stock";
    }

    private String getAllProducts(Long chatId)
    {
        try
        {
            List<Product> products = warehouseApiService.getProducts(new HashMap<>());
            if (products.isEmpty())
            {
                return "No products found.";
            }

            // Cache products and reset page for this user
            userProductCache.put(chatId, products);
            userPageStates.put(chatId, 0); // Start at page 0

            return formatProductsPage(chatId, 0);
            
        }
        catch (Exception e)
        {
            log.error("Error getting all products: {}", e.getMessage());
            return "❌ Error retrieving products. Please try again.";
        }
        //     StringBuilder sb = new StringBuilder("📦 All Products:\n\n");
        // for (int i = 0; i < Math.min(products.size(), 10); i++) {
        //     Product product = products.get(i);
        //     sb.append(formatProduct(product)).append("\n\n");
        // }
        // System.out.println(String.format("Size: %d", products.size()));
        
        // if (products.size() > 10) {
        //     sb.append("... and ").append(products.size() - 10).append(" more products");
        // }
        
        // return sb.toString();
    }

    /**
     * Format products for a specific page
     */
    private String formatProductsPage(Long chatId, int page)
    {
        List<Product> products = userProductCache.get(chatId);
        if (products == null || products.isEmpty()) {
            return "No products available.";
        }
        
        int totalProducts = products.size();
        int totalPages = (int) Math.ceil((double) totalProducts / PRODUCTS_PER_PAGE);
        
        // Validate page number
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        
        // Calculate start and end indices
        int startIndex = page * PRODUCTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PRODUCTS_PER_PAGE, totalProducts);
        
        StringBuilder sb = new StringBuilder();
        sb.append("📦 All Products\n");
        sb.append(String.format("📄 Page %d of %d\n\n", page + 1, totalPages));
        
        // Add products for current page
        for (int i = startIndex; i < endIndex; i++) {
            Product product = products.get(i);
            sb.append(formatProduct(product)).append("\n\n");
        }
        
        // Add pagination info
        int remaining = totalProducts - endIndex;
        if (remaining > 0) {
            sb.append(String.format("... and %d more products\n", remaining));
        }

        return sb.toString();
    }

    // private ReplyKeyboard getMainMenuKeyboard()
    // {
    //     // Create the keyboard object
    //     ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
    //     keyboardMarkup.setResizeKeyboard(true); // Makes keyboard compact
    //     keyboardMarkup.setOneTimeKeyboard(false); // Keeps keyboard visible
    //     keyboardMarkup.setSelective(true);

    //     // Create rows of buttons
    //     List<KeyboardRow> keyboard = new ArrayList<>();
        
    //     // First row
    //     KeyboardRow row1 = new KeyboardRow();
    //     row1.add("📦 Get products");
    //     keyboard.add(row1);

    //     // First row
    //     KeyboardRow row2 = new KeyboardRow();
    //     row2.add("➕ Add new products");
    //     keyboard.add(row2);
    
    //     // Second row
    //     KeyboardRow row3 = new KeyboardRow();
    //     row3.add("✏️ Update products");
    //     keyboard.add(row3);

    //     keyboardMarkup.setKeyboard(keyboard);
    //     return keyboardMarkup;
    // }

    // /**
    //  * Sub-menu for Get products
    //  */
    // private ReplyKeyboardMarkup getProductsSubMenuKeyboard()
    // {
    //     ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
    //     keyboardMarkup.setResizeKeyboard(true);
    //     keyboardMarkup.setOneTimeKeyboard(false);
        
    //     List<KeyboardRow> keyboard = new ArrayList<>();
        
    //     KeyboardRow row1 = new KeyboardRow();
    //     row1.add("All products");
    //     row1.add("Products by ID");
    //     keyboard.add(row1);
        
    //     KeyboardRow row2 = new KeyboardRow();
    //     row2.add("Search by filter");
    //     keyboard.add(row2);
        
    //     KeyboardRow row3 = new KeyboardRow();
    //     row3.add("🔙 Back to Main Menu");
    //     keyboard.add(row3);
        
    //     keyboardMarkup.setKeyboard(keyboard);
    //     return keyboardMarkup;
    // }

    // /**
    //  * Sub-menu for Add new products
    //  */
    // private ReplyKeyboardMarkup getAddProductsSubMenuKeyboard() {
    //     ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
    //     keyboardMarkup.setResizeKeyboard(true);
    //     keyboardMarkup.setOneTimeKeyboard(false);
        
    //     List<KeyboardRow> keyboard = new ArrayList<>();
        
    //     KeyboardRow row1 = new KeyboardRow();
    //     row1.add("Add new Thermal mug");
    //     keyboard.add(row1);
        
    //     KeyboardRow row2 = new KeyboardRow();
    //     row2.add("🔙 Back to Main Menu");
    //     keyboard.add(row2);
        
    //     keyboardMarkup.setKeyboard(keyboard);
    //     return keyboardMarkup;
    // }

    // /**
    //  * Sub-menu for Update products
    //  */
    // private ReplyKeyboardMarkup getUpdateProductsSubMenuKeyboard() {
    //     ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
    //     keyboardMarkup.setResizeKeyboard(true);
    //     keyboardMarkup.setOneTimeKeyboard(false);
        
    //     List<KeyboardRow> keyboard = new ArrayList<>();
        
    //     KeyboardRow row1 = new KeyboardRow();
    //     row1.add("Update thermal mug by ID");
    //     keyboard.add(row1);
        
    //     KeyboardRow row2 = new KeyboardRow();
    //     row2.add("Update quantity of reserved product");
    //     keyboard.add(row2);
        
    //     KeyboardRow row3 = new KeyboardRow();
    //     row3.add("Update product quantity in stock");
    //     keyboard.add(row3);
        
    //     KeyboardRow row4 = new KeyboardRow();
    //     row4.add("🔙 Back to Main Menu");
    //     keyboard.add(row4);
        
    //     keyboardMarkup.setKeyboard(keyboard);
    //     return keyboardMarkup;
    // }

    private String updateStockQuantityFromInput(String input)
    {
        try
        {
            String[] parts = input.split("\\|");
            if (parts.length != 3)
            {
                return "❌ Invalid format. Please use: PRODUCT_ID|WAREHOUSE_ID|QUANTITY_CHANGE\n" +
                    "📝 Example: 1|1|33  (add 33 items)\n" +
                    "📝 Example: 1|1|-5 (remove 5 items)";
            }

            Long productId = Long.parseLong(parts[0].trim());
            Integer warehouseId = Integer.parseInt(parts[1].trim());
            Integer quantityChange = Integer.parseInt(parts[2].trim());
            
            log.info("🔄 Processing stock update - Product: {}, Warehouse: {}, Change: {}", 
                    productId, warehouseId, quantityChange);
            
            // Validate quantity change is not zero
            if (quantityChange == 0) {
                return "❌ Quantity change cannot be zero. Use positive number to add or negative to remove.";
            }
            
            String result = warehouseApiService.updateStockQuantity(productId, warehouseId, quantityChange);
            return result;
            
        }
        catch (NumberFormatException e)
        {
            return "❌ Invalid number format. Please enter valid numbers.\n" +
                "📝 Format: PRODUCT_ID|WAREHOUSE_ID|QUANTITY_CHANGE\n" +
                "📝 Example: 1|1|33";
        } catch (Exception e) {
            log.error("Error updating stock quantity: {}", e.getMessage());
            return "❌ Error updating stock quantity: " + e.getMessage();
        }
    }

    private String handleStockProductId(String message, Long chatId)
    {
        try
        {
            Long productId = Long.parseLong(message.trim());
            
            // Optional: Validate product exists
            Product product = warehouseApiService.getProductById(productId);
            if (product == null) {
                return "❌ Product with ID " + productId + " not found. Please enter a valid Product ID:";
            }
            
            // Store product ID and move to next step
            stockProductIdCache.put(chatId, productId);
            userStates.put(chatId, AWAITING_STOCK_WAREHOUSE_ID);
            
            return "✅ Product found: " + product.getName() + "\n\n" +
                "Please enter the Warehouse ID:";
            
        }
        catch (NumberFormatException e)
        {
            return "❌ Invalid Product ID. Please enter a valid number:";
        }
    }

    /**
     * Step 2: Handle Warehouse ID input
     */
    private String handleStockWarehouseId(String message, Long chatId)
    {
        try {
            Integer warehouseId = Integer.parseInt(message.trim());
            
            // Store warehouse ID and move to next step
            stockWarehouseIdCache.put(chatId, warehouseId);
            userStates.put(chatId, AWAITING_STOCK_QUANTITY);
            
            return "✅ Warehouse ID: " + warehouseId + "\n\n" +
                "Please enter the Quantity Change:\n" +
                "• Positive number to ADD items (e.g., 33)\n" +
                "• Negative number to REMOVE items (e.g., -5)";
            
        } catch (NumberFormatException e) {
            return "❌ Invalid Warehouse ID. Please enter a valid number:";
        }
    }

    /**
     * Step 3: Handle Quantity Change input and perform the update
     */
    private String handleStockQuantity(String message, Long chatId)
    {
        try {
            Integer quantityChange = Integer.parseInt(message.trim());
            
            // Get stored values
            Long productId = stockProductIdCache.get(chatId);
            Integer warehouseId = stockWarehouseIdCache.get(chatId);
            
            if (productId == null || warehouseId == null) {
                clearStockStates(chatId);
                return "❌ Session expired. Please start over.";
            }
            
            // Validate quantity change is not zero
            if (quantityChange == 0) {
                return "❌ Quantity change cannot be zero. Please enter a positive or negative number:";
            }
            
            // Perform the stock update
            log.info("🔄 Updating stock - Product: {}, Warehouse: {}, Change: {}", 
                    productId, warehouseId, quantityChange);
            
            String result = warehouseApiService.updateStockQuantity(productId, warehouseId, quantityChange);
            
            // Clear all stock states after completion
            clearStockStates(chatId);
            
            return result;
            
        } catch (NumberFormatException e) {
            return "❌ Invalid quantity. Please enter a valid number:";
        }
    }

    private String handleReservedProductId(String message, Long chatId)
    {
        try {
            Long productId = Long.parseLong(message.trim());
            
            // Optional: Validate product exists
            Product product = warehouseApiService.getProductById(productId);
            if (product == null) {
                return "❌ Product with ID " + productId + " not found. Please enter a valid Product ID:";
            }
            
            // Store product ID and move to next step
            reservedProductIdCache.put(chatId, productId);
            userStates.put(chatId, AWAITING_RESERVED_QUANTITY);
            
            return "✅ Product found: " + product.getName() + "\n\n" +
                "Please enter the Quantity Change:\n" +
                "• Positive number to INCREASE reserved (e.g., 12)\n" +
                "• Negative number to DECREASE reserved (e.g., -5)";
            
        } catch (NumberFormatException e) {
            return "❌ Invalid Product ID. Please enter a valid number:";
        }
    }

    /**
     * Step 2: Handle Quantity Change input and perform the update
     */
    private String handleReservedQuantity(String message, Long chatId) {
        try {
            Integer quantityChange = Integer.parseInt(message.trim());
            
            // Get stored product ID
            Long productId = reservedProductIdCache.get(chatId);
            
            if (productId == null) {
                clearReservedStates(chatId);
                return "❌ Session expired. Please start over.";
            }
            
            // Validate quantity change is not zero
            if (quantityChange == 0) {
                return "❌ Quantity change cannot be zero. Please enter a positive or negative number:";
            }
            
            // Perform the reserved quantity update
            log.info("🔄 Updating reserved quantity - Product: {}, Change: {}", 
                    productId, quantityChange);
            
            String result = warehouseApiService.updateReservedQuantity(productId, quantityChange);
            
            // Clear all reserved states after completion
            clearReservedStates(chatId);
            
            return result;
            
        } catch (NumberFormatException e) {
            return "❌ Invalid quantity. Please enter a valid number:";
        }
    }

    /**
     * Clear all reserved-related states
     */
    private void clearReservedStates(Long chatId) {
        userStates.remove(chatId);
        reservedProductIdCache.remove(chatId);
    }

    /**
     * Clear all stock-related states
     */
    private void clearStockStates(Long chatId)
    {
        userStates.remove(chatId);
        stockProductIdCache.remove(chatId);
        stockWarehouseIdCache.remove(chatId);
    }

    public String getUserState(Long chatId)
    {
        return userStates.get(chatId);
    }

    // private String updateThermocupFromInput(String input) {
    //     try {
    //         String[] parts = input.split("\\|");
    //         if (parts.length < 13) {
    //             return "Invalid format. Please provide all required fields.";
    //         }

    //         Long productId = Long.parseLong(parts[0]);
    //         Thermocup thermocup = new Thermocup();
    //         thermocup.setName(parts[1]);
    //         thermocup.setCategory_id(Integer.parseInt(parts[2]));
    //         thermocup.setBase_price(Double.parseDouble(parts[3]));
    //         thermocup.setSKU(parts[4]);
    //         thermocup.setIs_active(Boolean.parseBoolean(parts[5]));
    //         thermocup.setPath_to_photo(parts[6]);

    //         Attribute attributes = new Attribute();
    //         attributes.setVolume_ml(Integer.parseInt(parts[7]));
    //         attributes.setColor(parts[8]);
    //         attributes.setBrand(parts[9]);
    //         attributes.setModel(parts[10]);
    //         attributes.setIs_hermetic(Boolean.parseBoolean(parts[11]));
    //         attributes.setMaterial(parts[12]);

    //         thermocup.setAttributes(attributes);

    //         return warehouseApiService.updateThermocup(productId, thermocup);
    //     } catch (Exception e) {
    //         return "Error updating thermocup: " + e.getMessage();
    //     }
    // }

    private String updateReservedQuantityFromInput(String input)
    {
        try
        {
            String[] parts = input.split("\\|");
            if (parts.length != 2) {
                return "Invalid format. Please use: ID|QUANTITY";
            }

            Long productId = Long.parseLong(parts[0]);
            Integer quantityChange = Integer.parseInt(parts[1]);

            return warehouseApiService.updateReservedQuantity(productId, quantityChange);
        }
        catch (Exception e)
        {
            return "Error updating reserved quantity: " + e.getMessage();
        }
    }

    private String getThermocupCreateInstructions() {
        return "Please enter thermocup data in the following format:\n\n" +
               "name|category_id|base_price|starting_quantity|warehouse_id|path_to_photo|volume_ml|color|brand|model|is_hermetic|material\n\n" +
               "Example:\n" +
               "Premium Thermo|1|29.99|100|1|/photos/thermo1.jpg|500|Blue|ThermoBrand|PremiumX|true|Stainless Steel";
    }

    private String formatProduct(Product product)
    {
        return String.format(
            "🆔 ID: %d\n📛 Name: %s\n🏷️ Category: %s\n💰 Price: $%.2f\n📦 Quantity: %d\n📦 Reserved: %d\n" + //
                                "🔧 Active: %s\n📸 Photo: %s",
            product.getId(),
            product.getName(),
            product.getCategory_name(),
            product.getBase_price(),
            product.getTotal_quantity(),
            product.getNum_reserved_goods(),
            product.getIs_active() ? "Yes" : "No",
            product.getPath_to_photo()
        );
    }

    private String formatProductShort(Product product)
    {
        return String.format(
            "🆔 %d | 📛 %s | 💰 $%.2f | 📦 %d | 📦 %d",
            product.getId(),
            product.getName(),
            product.getBase_price(),
            product.getTotal_quantity(),
            product.getNum_reserved_goods()
        );
    }

    /**
     * Format product information with category-specific attributes
     */
    private String formatProduct(ProductWithAttributes<?> productWithAttrs)
    {
        Product product = productWithAttrs.getProduct();
        Object attributes = productWithAttrs.getAttributes();
        
        // Start with basic product info
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "🆔 ID: %d\n" +
            "📛 Name: %s\n" +
            "🔘 Category: %s\n" +
            "🦾 Sku: %s\n" +
            "💰 Price: $%.2f\n" +
            "🏠 Quantity: %d\n" +
            "📦 Reserved: %d\n" +
            "🌐 Path to photo: %s",
            product.getId(),
            product.getName(),
            product.getCategory_name(),
            product.getSku(),
            product.getBase_price(),
            product.getTotal_quantity(),
            product.getNum_reserved_goods(),
            product.getPath_to_photo()
        ));
        
        sb.append("\n");
        // Add category-specific attributes
        sb.append("\n📋 Attributes:\n");
        
        switch (product.getCategory_name())
        {
            case "Thermocups":
                if (attributes instanceof ThermocupAttributes)
                {
                    sb.append(formatThermocupAttributes((ThermocupAttributes) attributes));
                }
                break;
                
            case "Servers":
                if (attributes instanceof ServerAttributes)
                {
                    sb.append(formatServerAttributes((ServerAttributes) attributes));
                }
                break;
                
            default:
                sb.append(product.getCategory_name());
                break;
        }
        
        return sb.toString();
    }

    /**
     * Format thermocup-specific attributes
     */
    private String formatThermocupAttributes(ThermocupAttributes thermocup)
    {
        return String.format(
            "• Volume: %d ml\n• Color: %s\n• Brand: %s\n• Model: %s\n• Hermetic: %s\n• Material: %s",
            thermocup.getVolume_ml(),
            thermocup.getColor(),
            thermocup.getBrand(),
            thermocup.getModel(),
            thermocup.getIs_hermetic() ? "Yes" : "No",
            thermocup.getMaterial()
        );
    }

    /**
     * Format server-specific attributes
     */
    private String formatServerAttributes(ServerAttributes server)
    {
        // Build storage info
        String storageInfo = "";
        if (server.getHdd_size_gb() != null && server.getSsd_size_gb() != null)
        {
            storageInfo = String.format("HDD: %d GB, SSD: %d GB", 
                server.getHdd_size_gb(), server.getSsd_size_gb());
        }
        else if (server.getHdd_size_gb() != null)
        {
            storageInfo = String.format("HDD: %d GB", server.getHdd_size_gb());
        }
        else if (server.getSsd_size_gb() != null)
        {
            storageInfo = String.format("SSD: %d GB", server.getSsd_size_gb());
        }
        
        return String.format(
            "• RAM: %d GB\n• CPU: %s (%d cores)\n• %s\n• Form Factor: %s\n• Manufacturer: %s",
            server.getRam_gb(),
            server.getCpu_model(),
            server.getCpu_cores(),
            storageInfo,
            server.getForm_factor(),
            server.getManufacturer()
        );
    }

    // In CommandHandler.java - replace the thermocup methods:

    private String createThermocupFromInput(String input)
    {
        try
        {
            String[] parts = input.split("\\|");
            if (parts.length < 13) //there are can not be more than 13 params (NEED TEST)
            {
                return "Invalid format. Please provide all required fields.";
            }

            // Create Product
            Product product = new Product();
            product.setName(parts[0]);
            product.setCategory_name(parts[1]);
            product.setBase_price(new java.math.BigDecimal(parts[2]));
            product.setSku(parts[3]);
            product.setIs_active(Boolean.parseBoolean(parts[4]));
            product.setPath_to_photo(parts[5]);

            // Create ThermocupAttributes
            ThermocupAttributes attributes = new ThermocupAttributes();
            attributes.setVolume_ml(Integer.parseInt(parts[6]));
            attributes.setColor(parts[7]);
            attributes.setBrand(parts[8]);
            attributes.setModel(parts[9]);
            attributes.setIs_hermetic(Boolean.parseBoolean(parts[10]));
            attributes.setMaterial(parts[11]);

            return warehouseApiService.createThermocup(product, attributes);
        }
        catch (Exception e)
        {
            return "Error creating thermocup: " + e.getMessage();
        }
    }


    private String formatProductWithAttributes(ProductWithAttributes<?> productWithAttributes)
    {
        Product product = productWithAttributes.getProduct();
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format
        (
            "🆔 ID: %d\n📛 Name: %s\n🏷️ Category: %s\n💰 Price: $%.2f\n📦 Reserved: %d\n🔧 Active: %s\n",
            product.getId(),
            product.getName(),
            product.getCategory_name(),
            product.getBase_price(),
            product.getNum_reserved_goods(),
            product.getIs_active() ? "Yes" : "No"
        ));
        
        // Add attributes based on category
        Object attributes = productWithAttributes.getAttributes();
        if (attributes instanceof ThermocupAttributes)
        {
            ThermocupAttributes thermocup = (ThermocupAttributes) attributes;
            sb.append("\n🧴 Thermocup Attributes:\n")
            .append(String.format("• Volume: %d ml\n• Color: %s\n• Brand: %s\n• Model: %s\n• Hermetic: %s\n• Material: %s",
                    thermocup.getVolume_ml(), thermocup.getColor(), thermocup.getBrand(),
                    thermocup.getModel(), thermocup.getIs_hermetic() ? "Yes" : "No", thermocup.getMaterial()));
        }
        else if (attributes instanceof ServerAttributes)
        {
            ServerAttributes server = (ServerAttributes) attributes;
            sb.append("\n🖥️ Server Attributes:\n")
            .append(String.format("• RAM: %d GB\n• CPU: %s (%d cores)\n• HDD: %d GB\n• SSD: %d GB\n• Form: %s\n• Manufacturer: %s",
                    server.getRam_gb(), server.getCpu_model(), server.getCpu_cores(),
                    server.getHdd_size_gb(), server.getSsd_size_gb(), server.getForm_factor(), server.getManufacturer()));
        }
        
        return sb.toString();
    }

}