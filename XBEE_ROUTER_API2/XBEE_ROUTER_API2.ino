/**
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 *
 * This file is part of XBee-Arduino.
 *
 * XBee-Arduino is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XBee-Arduino is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XBee-Arduino.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <XBee.h>

/*
This example is for Series 2 XBee
 Sends a ZB TX request with the value of analogRead(pin5) and checks the status response for success
*/

// create the XBee object
XBee xbee = XBee();

uint8_t payload[] = { 0, 0 };

// SH + SL Address of receiving XBee
XBeeAddress64 addr64 = XBeeAddress64(0x0013a200, 0x40C8C952);
ZBTxRequest zbTx = ZBTxRequest(addr64, payload, sizeof(payload));
ZBTxStatusResponse txStatus = ZBTxStatusResponse();
// create reusable response objects for responses we expect to handle 
ZBRxResponse rx = ZBRxResponse();

int pin5 = 0;

int statusLed = 13;
int errorLed = 13;
int speakerPin = 9;

//For buzzer control
int length = 15; // the number of notes
char notes[] = "ccggaagffeeddc "; // a space represents a rest
int beats[] = { 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 4 };
int tempo = 5;

void flashLed(int pin, int times, int wait) {

  for (int i = 0; i < times; i++) {
    digitalWrite(pin, HIGH);
    delay(wait);
    digitalWrite(pin, LOW);

    if (i + 1 < times) {
      delay(wait);
    }
  }
}

void playTone(int tone, int duration) {
  for (long i = 0; i < duration * 1000L; i += tone * 2) {
    digitalWrite(speakerPin, HIGH);
    delayMicroseconds(tone);
    digitalWrite(speakerPin, LOW);
    delayMicroseconds(tone);
  }
}

void playNote(char note, int times, int duration) {
  char names[] = { 'c', 'd', 'e', 'f', 'g', 'a', 'b', 'C' };
  int tones[] = { 1915, 1700, 1519, 1432, 1275, 1136, 1014, 956 };

  // play the tone corresponding to the note name
  for (int i = 0; i < times; i++) {
    if (names[i] == note) {
      playTone(tones[i], duration);
    }
  }
}

void playNotes(int times){
  for (int i = 0; i < length; i++) {
    if (notes[i] == ' ') {
      delay(beats[i] * tempo); // rest
    } else {
      playNote(notes[i], times, beats[i] * tempo);
    }
  }
}

void setup() {
  //light sensor
  pinMode(14, INPUT);
  pinMode(statusLed, OUTPUT);
  pinMode(errorLed, OUTPUT);
  //digit buzzer
  pinMode(speakerPin, OUTPUT);

  Serial.begin(9600);
  xbee.setSerial(Serial);
}

void readPacketIn() {
      xbee.readPacket(500);
      if (xbee.getResponse().getApiId() == ZB_RX_RESPONSE) {
        // got a zb rx packet
  
        // now fill our zb rx class
        xbee.getResponse().getZBRxResponse(rx);
            
        if (rx.getOption() == ZB_PACKET_ACKNOWLEDGED) {
            // the sender got an ACK
            //!!!Note that flash led contains delay, which will block the loop thread, be careful!!!
//            flashLed(statusLed, 5, 2000);
                    // set vibration PWM to value of the first byte in the data
        if(rx.getData(0) == 'X' && rx.getData(2) == 'e'){
                  playNotes(2);
        }
        } else {
            // we got it (obviously) but sender didn't get an ACK
             playNotes(1);
        }

//      } 
//      else if (xbee.getResponse().getApiId() == MODEM_STATUS_RESPONSE) {
//        xbee.getResponse().getModemStatusResponse(msr);
//        // the local XBee sends this response on certain events, like association/dissociation
//        
//        if (msr.getStatus() == ASSOCIATED) {
//          // yay this is great.  flash led
//          flashLed(statusLed, 10, 10);
//        } else if (msr.getStatus() == DISASSOCIATED) {
//          // this is awful.. flash led to show our discontent
//          flashLed(errorLed, 10, 10);
//        } else {
//          // another status
//          flashLed(statusLed, 5, 10);
//        }
//      } else {
//      	// not something we were expecting
//        flashLed(errorLed, 1, 25);    
//      }
    } else if (xbee.getResponse().isError()) {
      //nss.print("Error reading packet.  Error code: ");  
      //nss.println(xbee.getResponse().getErrorCode());
    }
}

void loop() {   
  // break down 10-bit reading into two bytes and place in payload
  pin5 = analogRead(A1);

//   Serial.println("SAMPLE DATA --> " + pin5);
  payload[0] = pin5 >> 8 & 0xff;
  payload[1] = pin5 & 0xff;

  xbee.send(zbTx);

  // flash TX indicator
  flashLed(statusLed, 1, 100);

  // after sending a tx request, we expect a status response
  // wait up to half second for the status response
  if (xbee.readPacket(500)) {
    // got a response!

    // should be a znet tx status            	
    if (xbee.getResponse().getApiId() == ZB_TX_STATUS_RESPONSE) {
      xbee.getResponse().getZBTxStatusResponse(txStatus);

      // get the delivery status, the fifth byte
      if (txStatus.getDeliveryStatus() == SUCCESS) {
        // success.  time to celebrate
        //Test DIO
        flashLed(statusLed, 5, 50);
        } else {
           // the remote XBee did not receive our packet. is it powered on?
        flashLed(errorLed, 3, 500);
        }
      } else {
        // the remote XBee did not receive our packet. is it powered on?
        flashLed(errorLed, 3, 500);
      }
  } else if (xbee.getResponse().isError()) {
    //nss.print("Error reading packet.  Error code: ");  
//    nss.println(xbee.getResponse().getErrorCode());
  } else {
    // local XBee did not provide a timely TX Status Response -- should not happen
    flashLed(errorLed, 10, 500);
  }
  readPacketIn();
  delay(1000);
}
