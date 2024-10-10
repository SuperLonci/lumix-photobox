#include <FastLED.h>

#define LED_PIN     6
#define NUM_LEDS    16
CRGB leds[NUM_LEDS];

int brightness = 154;  // Initial brightness (0-255)
int mode = 4;  

void parseCommand(String);
void steadyMode();
void blinkMode();
void fadeMode();
void rainbowMode();
void pulseAnimation();
void cameraShutterAnimation();
void offMode();
void chasingLight(CRGB);

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
            offMode();
            break;
        case 1:
            steadyMode();
            break;
        case 2:
            cameraShutterAnimation();
            break;
        case 3:
            rainbowMode();
            break;
        case 4:
            chasingLight(CRGB::White);
            break;
    }

    FastLED.show();  // Update the LED strip
    delay(20);       // Small delay to avoid overloading the CPU
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

void offMode() {
    fill_solid(leds, NUM_LEDS, CRGB::Black);  // Set all LEDs to white
}

//  Steady light 
void steadyMode() {
    fill_solid(leds, NUM_LEDS, CRGB::White);  // Set all LEDs to white
}

// Blinking mode (toggle LEDs on and off)
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

// Fading mode (fade all LEDs in and out)
void fadeMode() {
    static uint8_t fadeValue = 0;
    static int fadeDirection = 1;
    fadeValue += fadeDirection * 5;  // Adjust fade value
    if (fadeValue == 0 || fadeValue == 255) {
        fadeDirection *= -1;  // Reverse direction at boundaries
    }
    fill_solid(leds, NUM_LEDS, CHSV(0, 0, fadeValue));  // Set LEDs to fade in and out
}

// Rainbow mode (cycle through colors)
void rainbowMode() {
    static uint8_t hue = 0;
    fill_rainbow(leds, NUM_LEDS, hue, 7);  // Cycle through rainbow colors
    hue++;  // Increment hue for smooth transition
}

void pulseAnimation() {
  int middleLED = NUM_LEDS / 2;
  
  for (int i = 0; i <= middleLED; i++) {
    // Light up LEDs from bottom to top
    leds[i] = CRGB::White;
    leds[NUM_LEDS - 1 - i] = CRGB::White;
    
    // Fade out previous LEDs
    if (i > 0) {
      fadeToBlackBy(&leds[i-1], 1, 128);
      fadeToBlackBy(&leds[NUM_LEDS - i], 1, 128);
    }
    
    FastLED.show();
    delay(1000 / NUM_LEDS);  // Adjust speed of animation
  }
}

void cameraShutterAnimation() {
  // Shutter closing animation
  for (int i = 0; i < NUM_LEDS / 2; i++) {
    leds[i] = CRGB::White;
    leds[NUM_LEDS - 1 - i] = CRGB::White;
    FastLED.show();
    delay(50);
  }
  
  // Flash effect
  fill_solid(leds, NUM_LEDS, CRGB::White);
  FastLED.show();
  delay(100);
  
  // Shutter opening animation
  for (int i = (NUM_LEDS / 2) - 1; i >= 0; i--) {
    leds[i] = CRGB::Black;
    leds[NUM_LEDS - 1 - i] = CRGB::Black;
    FastLED.show();
    delay(20);
  }
}

void chasingLight(CRGB color) {
  static int pos = 0;
  
  // Clear the LED at the tail
  leds[(pos + NUM_LEDS - 4) % NUM_LEDS] = CRGB::Black;
  
  // Create the meteor effect
  for (int i = 0; i < 4; i++) {
    int tailPos = (pos + i) % NUM_LEDS;
    int brightness = 255 - (i * 64);  // 255, 191, 127, 63
    leds[tailPos] = color.nscale8(brightness);
  }
  
  FastLED.show();
  
  // Move to the next position
  pos = (pos + 1) % NUM_LEDS;
  
  delay(50);  // Adjust this value to change the speed of the chase
}

