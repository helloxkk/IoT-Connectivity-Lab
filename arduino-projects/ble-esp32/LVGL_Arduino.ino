#define CONFIG_BT_ENABLED 1
#define CONFIG_BT_CLASSIC_ENABLED 0

#include <BLEDevice.h>
#include <BLEServer.h>
#include "Display_ST7789.h"
#include "LVGL_Driver.h"
#include <Adafruit_NeoPixel.h>

// RGB LED pin and count
#define RGB_PIN 8
#define NUM_LEDS 1

Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, RGB_PIN, NEO_GRB + NEO_KHZ800);

// Display objects
lv_obj_t *bt_indicator;    // Bluetooth status indicator
lv_obj_t *wifi_indicator;  // WiFi status indicator
lv_obj_t *color_preview;   // Color preview area
lv_obj_t *rgb_value_label; // RGB value display
lv_obj_t *hex_value_label; // Hex value display
lv_obj_t *r_value_label;   // R value label
lv_obj_t *g_value_label;   // G value label
lv_obj_t *b_value_label;   // B value label
lv_obj_t *status_label;    // Status message label
lv_obj_t *uptime_label;    // Uptime label
lv_obj_t *mem_label;       // Memory usage label

BLEServer *pServer = NULL;
bool deviceConnected = false;
uint8_t currentRed = 0;
uint8_t currentGreen = 0;
uint8_t currentBlue = 0;
unsigned long startTime = 0;

/**
 * @brief Set RGB LED color
 * Note: Red and Green swapped due to wiring issue
 */
void setColor(uint8_t red, uint8_t green, uint8_t blue) {
    currentRed = red;
    currentGreen = green;
    currentBlue = blue;
    
    // Swap red and green due to wiring issue
    strip.setPixelColor(0, strip.Color(green, red, blue));
    strip.show();
    
    // Update color preview (keep RGB values in UI in standard order)
    lv_obj_set_style_bg_color(color_preview, lv_color_make(red, green, blue), 0);
    
    // Update RGB value display
    char rgb_text[32];
    snprintf(rgb_text, sizeof(rgb_text), "RGB: (%d, %d, %d)", red, green, blue);
    lv_label_set_text(rgb_value_label, rgb_text);
    
    // Update hex value display
    char hex_text[20];
    snprintf(hex_text, sizeof(hex_text), "#%02X%02X%02X", red, green, blue);
    lv_label_set_text(hex_value_label, hex_text);
    
    // Update RGB channel values
    char channel_text[20];
    snprintf(channel_text, sizeof(channel_text), "%d (%d%%)", red, (red * 100) / 255);
    lv_label_set_text(r_value_label, channel_text);
    
    snprintf(channel_text, sizeof(channel_text), "%d (%d%%)", green, (green * 100) / 255);
    lv_label_set_text(g_value_label, channel_text);
    
    snprintf(channel_text, sizeof(channel_text), "%d (%d%%)", blue, (blue * 100) / 255);
    lv_label_set_text(b_value_label, channel_text);
    
    // Update status message
    char status_text[50];
    snprintf(status_text, sizeof(status_text), "COLOR SET: #%02X%02X%02X", red, green, blue);
    lv_label_set_text(status_label, status_text);
}

/**
 * @brief Update connection status indicators
 */
void updateConnectionStatus() {
    // Update Bluetooth status
    if (deviceConnected) {
        lv_obj_set_style_bg_color(bt_indicator, lv_color_make(76, 175, 80), 0); // Green
    } else {
        lv_obj_set_style_bg_color(bt_indicator, lv_color_make(244, 67, 54), 0); // Red
    }
    
    // WiFi not connected for now
    lv_obj_set_style_bg_color(wifi_indicator, lv_color_make(244, 67, 54), 0); // Red
}

/**
 * @brief Update system information
 */
void updateSystemInfo() {
    // Calculate uptime
    unsigned long uptime = millis() - startTime;
    unsigned long seconds = uptime / 1000;
    unsigned long minutes = seconds / 60;
    unsigned long hours = minutes / 60;
    minutes %= 60;
    
    char uptime_text[20];
    snprintf(uptime_text, sizeof(uptime_text), "UP: %02lu:%02lu", hours, minutes);
    lv_label_set_text(uptime_label, uptime_text);
    
    // Memory information (example values - replace with actual if available)
    char mem_text[20];
    snprintf(mem_text, sizeof(mem_text), "FREE: 96KB");
    lv_label_set_text(mem_label, mem_text);
}

/**
 * @brief BLE server callbacks
 */
class MyServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        updateConnectionStatus();
        lv_label_set_text(status_label, "BT CONNECTED");
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        updateConnectionStatus();
        pServer->getAdvertising()->start();
        lv_label_set_text(status_label, "BT DISCONNECTED");
    }
};

/**
 * @brief BLE characteristic callbacks
 */
class MyCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue();
        if (value.length() == 3) {
            // Parse RGB values
            uint8_t red = static_cast<uint8_t>(value[0]);
            uint8_t green = static_cast<uint8_t>(value[1]);
            uint8_t blue = static_cast<uint8_t>(value[2]);
            setColor(red, green, blue); // Set color
        }
    }
};

/**
 * @brief Initialize BLE
 */
void initBLE() {
    BLEDevice::init("ESP32_RGB_CTRL");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    // Create BLE service and characteristic
    BLEService *pService = pServer->createService(BLEUUID((uint16_t)0x180F));
    BLECharacteristic *pCharacteristic = pService->createCharacteristic(
        BLEUUID((uint16_t)0x2A19),
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );

    uint8_t defaultValue[3] = {0, 0, 0};
    pCharacteristic->setValue(defaultValue, 3);
    pCharacteristic->setCallbacks(new MyCharacteristicCallbacks());
    pService->start();
    pServer->getAdvertising()->start();
    lv_label_set_text(status_label, "BT ADVERTISING...");
}

/**
 * @brief Initialize display
 */
void initDisplay() {
    LCD_Init(); // Initialize LCD
    Lvgl_Init(); // Initialize LVGL

    lv_obj_clean(lv_scr_act());
    lv_obj_t *scr = lv_scr_act();
    lv_obj_set_style_bg_color(scr, lv_color_black(), 0);
    lv_obj_set_style_bg_opa(scr, LV_OPA_COVER, 0);

    // === STATUS BAR ===
    lv_obj_t *status_bar = lv_obj_create(scr);
    lv_obj_set_size(status_bar, 172, 24);
    lv_obj_set_style_bg_color(status_bar, lv_color_black(), 0);
    lv_obj_set_style_border_color(status_bar, lv_color_make(51, 51, 51), 0);
    lv_obj_set_style_border_width(status_bar, 1, 0);
    lv_obj_set_style_border_side(status_bar, LV_BORDER_SIDE_BOTTOM, 0);
    lv_obj_set_style_pad_all(status_bar, 0, 0);
    lv_obj_set_pos(status_bar, 0, 0);
    
    // Bluetooth indicator
    bt_indicator = lv_obj_create(status_bar);
    lv_obj_set_size(bt_indicator, 6, 6);
    lv_obj_set_style_radius(bt_indicator, 3, 0);
    lv_obj_set_style_border_width(bt_indicator, 0, 0);
    lv_obj_align(bt_indicator, LV_ALIGN_LEFT_MID, 8, 0);
    
    lv_obj_t *bt_label = lv_label_create(status_bar);
    lv_label_set_text(bt_label, "BT");
    lv_obj_set_style_text_color(bt_label, lv_color_white(), 0);
    lv_obj_align(bt_label, LV_ALIGN_LEFT_MID, 18, 0);
    
    // ESP32 title
    lv_obj_t *title_label = lv_label_create(status_bar);
    lv_label_set_text(title_label, "ESP32");
    lv_obj_set_style_text_color(title_label, lv_color_white(), 0);
    lv_obj_align(title_label, LV_ALIGN_CENTER, 0, 0);
    
    // WiFi indicator
    wifi_indicator = lv_obj_create(status_bar);
    lv_obj_set_size(wifi_indicator, 6, 6);
    lv_obj_set_style_radius(wifi_indicator, 3, 0);
    lv_obj_set_style_border_width(wifi_indicator, 0, 0);
    lv_obj_align(wifi_indicator, LV_ALIGN_RIGHT_MID, -42, 0);
    
    lv_obj_t *wifi_label = lv_label_create(status_bar);
    lv_label_set_text(wifi_label, "WIFI");
    lv_obj_set_style_text_color(wifi_label, lv_color_white(), 0);
    lv_obj_align(wifi_label, LV_ALIGN_RIGHT_MID, -8, 0);

    // === MAIN CONTENT ===
    // Title
    lv_obj_t *main_title = lv_label_create(scr);
    lv_label_set_text(main_title, "RGB LED CONTROLLER");
    lv_obj_set_style_text_color(main_title, lv_color_make(3, 169, 244), 0);
    lv_obj_align(main_title, LV_ALIGN_TOP_MID, 0, 35);
    
    // Color preview area
    color_preview = lv_obj_create(scr);
    lv_obj_set_size(color_preview, 80, 80);
    lv_obj_set_style_radius(color_preview, 4, 0);
    lv_obj_set_style_border_color(color_preview, lv_color_make(102, 102, 102), 0);
    lv_obj_set_style_border_width(color_preview, 1, 0);
    lv_obj_align(color_preview, LV_ALIGN_TOP_MID, 0, 60);
    
    // RGB value display
    rgb_value_label = lv_label_create(scr);
    lv_label_set_text(rgb_value_label, "RGB: (0, 0, 0)");
    lv_obj_set_style_text_color(rgb_value_label, lv_color_make(187, 222, 251), 0);
    lv_obj_align(rgb_value_label, LV_ALIGN_TOP_MID, 0, 150);
    
    // Hex value display
    hex_value_label = lv_label_create(scr);
    lv_label_set_text(hex_value_label, "#000000");
    lv_obj_set_style_text_color(hex_value_label, lv_color_make(187, 222, 251), 0);
    lv_obj_align(hex_value_label, LV_ALIGN_TOP_MID, 0, 170);
    
    // RGB channel displays
    // Note: Colors in UI remain standard (R=red, G=green) regardless of wiring
    lv_obj_t *r_label = lv_label_create(scr);
    lv_label_set_text(r_label, "R:");
    lv_obj_set_style_text_color(r_label, lv_color_make(244, 67, 54), 0);
    lv_obj_align(r_label, LV_ALIGN_TOP_LEFT, 10, 195);
    
    r_value_label = lv_label_create(scr);
    lv_label_set_text(r_value_label, "0 (0%)");
    lv_obj_set_style_text_color(r_value_label, lv_color_white(), 0);
    lv_obj_align(r_value_label, LV_ALIGN_TOP_RIGHT, -10, 195);
    
    lv_obj_t *g_label = lv_label_create(scr);
    lv_label_set_text(g_label, "G:");
    lv_obj_set_style_text_color(g_label, lv_color_make(76, 175, 80), 0);
    lv_obj_align(g_label, LV_ALIGN_TOP_LEFT, 10, 220);
    
    g_value_label = lv_label_create(scr);
    lv_label_set_text(g_value_label, "0 (0%)");
    lv_obj_set_style_text_color(g_value_label, lv_color_white(), 0);
    lv_obj_align(g_value_label, LV_ALIGN_TOP_RIGHT, -10, 220);
    
    lv_obj_t *b_label = lv_label_create(scr);
    lv_label_set_text(b_label, "B:");
    lv_obj_set_style_text_color(b_label, lv_color_make(33, 150, 243), 0);
    lv_obj_align(b_label, LV_ALIGN_TOP_LEFT, 10, 245);
    
    b_value_label = lv_label_create(scr);
    lv_label_set_text(b_value_label, "0 (0%)");
    lv_obj_set_style_text_color(b_value_label, lv_color_white(), 0);
    lv_obj_align(b_value_label, LV_ALIGN_TOP_RIGHT, -10, 245);
    
    // Status message
    status_label = lv_label_create(scr);
    lv_label_set_text(status_label, "READY");
    lv_obj_set_style_text_color(status_label, lv_color_white(), 0);
    lv_obj_align(status_label, LV_ALIGN_TOP_MID, 0, 270);
    
    // === FOOTER ===
    lv_obj_t *footer = lv_obj_create(scr);
    lv_obj_set_size(footer, 172, 24);
    lv_obj_set_style_bg_color(footer, lv_color_black(), 0);
    lv_obj_set_style_border_color(footer, lv_color_make(51, 51, 51), 0);
    lv_obj_set_style_border_width(footer, 1, 0);
    lv_obj_set_style_border_side(footer, LV_BORDER_SIDE_TOP, 0);
    lv_obj_set_style_pad_all(footer, 0, 0);
    lv_obj_set_pos(footer, 0, 296);
    
    // Uptime
    uptime_label = lv_label_create(footer);
    lv_label_set_text(uptime_label, "UP: 00:00");
    lv_obj_set_style_text_color(uptime_label, lv_color_white(), 0);
    lv_obj_align(uptime_label, LV_ALIGN_LEFT_MID, 6, 0);
    
    // Memory usage
    mem_label = lv_label_create(footer);
    lv_label_set_text(mem_label, "FREE: 96KB");
    lv_obj_set_style_text_color(mem_label, lv_color_white(), 0);
    lv_obj_align(mem_label, LV_ALIGN_RIGHT_MID, -6, 0);
    
    // Initialize status
    updateConnectionStatus();
}

/**
 * @brief Setup
 */
void setup() {
    startTime = millis();
    
    strip.begin();
    strip.show(); // Turn off LED initially

    initDisplay(); // Initialize display
    initBLE();     // Initialize BLE
    updateSystemInfo(); // Update system information
}

/**
 * @brief Main loop
 */
void loop() {
    Timer_Loop();
    lv_task_handler();
    
    // Update system info every 5 seconds
    static uint32_t lastUpdateTime = 0;
    if (millis() - lastUpdateTime > 5000) {
        updateSystemInfo();
        lastUpdateTime = millis();
    }
    
    delay(5);
}