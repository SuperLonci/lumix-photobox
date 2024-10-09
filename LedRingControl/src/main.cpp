#include <FastLED.h>

#define LED_PIN     6
#define NUM_LEDS    16
CRGB leds[NUM_LEDS];

int brightness = 154;  // Initial brightness (0-255)
int mode = 0;  

void parseCommand(String);
void steadyMode();
void blinkMode();
void fadeMode();
void rainbowMode();

void setup() {
    Serial.begin(9600); 
    FastLED.addLeds<WS2812B, LED_PIN>(leds, NUM_LEDS);  // Define the data pin as D6
    FastLED.setBrightness(brightness); // Set initial brightness
    FastLED.show(); 
}

void loop() {
    if (Serial.available() > 0) {
        String input = Serial.readStringUntil('\n');  // Read incoming serial data
        parseCommand(input);  // Parse the received command
    }

    // Execute the current mode
    switch (mode) {
        case 0:
            steadyMode();
            break;
        case 1:
            blinkMode();
            break;
        case 2:
            fadeMode();
            break;
        case 3:
            rainbowMode();
            break;
    }

    FastLED.show();  // Update the LED strip
    delay(50);       // Small delay to avoid overloading the CPU
}

void parseCommand(String input) {
    if (input.startsWith("brightness")) {
        // Parse and set brightness
        brightness = input.substring(11).toInt();
        brightness = constrain(brightness, 0, 255);  // Limit brightness between 0 and 255
        FastLED.setBrightness(brightness);
        Serial.println("Brightness set to " + String(brightness));
    } else if (input.startsWith("mode")) {
        // Parse and set mode (0 = steady, 1 = blinking, 2 = fading, 3 = rainbow)
        mode = input.substring(5).toInt();
        Serial.println("Mode set to " + String(mode));
    }
}

// Mode 0: Steady light with the current brightness
void steadyMode() {
    fill_solid(leds, NUM_LEDS, CRGB::White);  // Set all LEDs to white
}

// Mode 1: Blinking mode (toggle LEDs on and off)
void blinkMode() {
    static bool ledState = false;
    if (ledState) {
        fill_solid(leds, NUM_LEDS, CRGB::White);  // Turn all LEDs on
    } else {
        fill_solid(leds, NUM_LEDS, CRGB::Black);  // Turn all LEDs off
    }
    ledState = !ledState;
    delay(500);  // Delay for blink effect
}

// Mode 2: Fading mode (fade all LEDs in and out)
void fadeMode() {
    static uint8_t fadeValue = 0;
    static int fadeDirection = 1;
    fadeValue += fadeDirection * 5;  // Adjust fade value
    if (fadeValue == 0 || fadeValue == 255) {
        fadeDirection *= -1;  // Reverse direction at boundaries
    }
    fill_solid(leds, NUM_LEDS, CHSV(0, 0, fadeValue));  // Set LEDs to fade in and out
}

// Mode 3: Rainbow mode (cycle through colors)
void rainbowMode() {
    static uint8_t hue = 0;
    fill_rainbow(leds, NUM_LEDS, hue, 7);  // Cycle through rainbow colors
    hue++;  // Increment hue for smooth transition
}
