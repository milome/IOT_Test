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

/* For Dallas temperature sensor */
#include <OneWire.h>

#define WATER_LEVEL_THRESHOLD 570 //mapping to the sensor value 3.8 cm
#define WATER_FLOW_THRESHOLD 70
/* Begin definition of dfrobot PH sensor */
#define pHSensorPin A2            //pH meter Analog output to Arduino Analog Input 2
#define Offset 0.13            //deviation compensate
#define LED 13
#define samplingInterval 20
#define printInterval 800
#define ArrayLenth  40    //times of collection
int pHArray[ArrayLenth];   //Store the average value of the sensor feedback
int pHArrayIndex=0;  
/* End of definition */

/*
This is for Series 2 XBee
Sends a ZB TX request with the value of analogRead(pin5) and checks the status response for success
*/
// create the XBee object
XBee xbee = XBee();

/* Begin definition for hall sensor(water flow) */
volatile int NbTopsFan; //定义函数NbTopsFan 为整形
int Calc; //定义函数Calc 为整形变量
int on = 9; //定义on 为数字口3, relay as toggle switch.
int key = 5; //定义key 为数字口5, For Test - button as a control trigger.
int hallsensor = 2; //定义hallsensor 为数字口2, use the int.0, since only D3 can be IO output
int flag; //定义flag 为整形变量
void rpm ()
{
  NbTopsFan++; //函数自动+1
}
/* End of definition */

/* Begin definition of Temperature DS18S20 sensor */
int DS18S20_Temperature = 4; //DS18S20 Signal pin on digital 4
//Temperature chip i/o
OneWire ds(DS18S20_Temperature);  // on digital pin 4
/* End of definition */

/* Need to measure five sensors.
 * !!! Note D 2,7,8,10 is reserved for relay shield. 
 */
// payload[0]-[1]:  PH sensor - A2  pH meter Analog output to Arduino Analog Input 2
// payload[2]-[3]:  water Temperature - D4
// payload[4]-[5]:  hall sensor(water flow) - D2(Need interruption support D2 OR D3!)
// payload[6]-[7]:  water level sensor - A5
// payload[8]-[9]:  light sensor - A1, only for test
uint8_t payload[] = { 
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

// SH + SL Address of receiving XBee
XBeeAddress64 addr64 = XBeeAddress64(0x0013a200, 0x40C8C952);
ZBTxRequest zbTx = ZBTxRequest(addr64, payload, sizeof(payload));
ZBTxStatusResponse txStatus = ZBTxStatusResponse();
// create reusable response objects for responses we expect to handle 
ZBRxResponse rx = ZBRxResponse();
//
int pinLight = 0;
int pinWaterLevel = 0;
int pinPH = 0;
int pinHall = 0;
int pinTemp = 0;

int statusLed = 13;
int errorLed = 13;

int relayMotorPin = 3;
int relayNo2Pin = 6;
/* End of relay pins */

int sluiceOpenFlag = 1;

/* This is for notification sound usage. Can be a buzzer, or a mp3 player*/
int speakerPin = 11;

//For buzzer control
int length = 15; // the number of notes
char notes[] = "ccggaagffeeddc "; // a space represents a rest
int beats[] = { 
  1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 4 };
int tempo = 5;

void setup() {
  pinMode(14, INPUT); //??
  pinMode(statusLed, OUTPUT);
  pinMode(errorLed, OUTPUT);
  //digit buzzer
  pinMode(speakerPin, OUTPUT);
  
  //Relays
  pinMode(relayMotorPin, OUTPUT);
  pinMode(relayNo2Pin, OUTPUT);

  //Default enable the Motor
  digitalWrite(relayMotorPin, HIGH);
  digitalWrite(relayNo2Pin, LOW);

  /* Initialization for hall sensor interruption */
  pinMode(hallsensor, INPUT); //定义hallsensor 为输入信号
  delay(1000);
  /* For mega 2560:
   * int.0 D2
   * int.1 D3
   * int.2 D21
   * int.3 D20
   * int.4 D19
   * int.5 D18
   */
  attachInterrupt(0, rpm, RISING); //定义中断进程 http://www.arduino.cn/thread-2421-1-1.html
  
  /* Initialization for temperature chip i/o */
  pinMode(DS18S20_Temperature, INPUT);

  /* Following is for test purpose, control relay by button */
  pinMode(key,INPUT); //定义key 为输入信号
  pinMode(on, OUTPUT); //定义on 为输出入信号
  digitalWrite(on,LOW); //定义on 初始信号为low
  /* End of test code */
  
  Serial.begin(9600);
  xbee.setSerial(Serial);

  delay(1000);
}

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
  char names[] = { 
    'c', 'd', 'e', 'f', 'g', 'a', 'b', 'C'             };
  int tones[] = { 
    1915, 1700, 1519, 1432, 1275, 1136, 1014, 956             };

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
    } 
    else {
      playNote(notes[i], times, beats[i] * tempo);
    }
  }
}

void turnOffRelay(int index){
  digitalWrite(index, LOW);   //Turn off relay 
}

void turnOnRelay(int index){
  digitalWrite(index, HIGH);   //Turn on relay 
}

int readHallSensor() {
  NbTopsFan = 0; //NbTops 初始值为0
  sei(); //初始化中断
  delay(1000); //?? Need or not?
  cli(); //禁用中断
  Calc = (NbTopsFan * 60 / 8.1); //(脉冲频率×60)/ 8.1 Q,=流量L /小时
  delay(1000); 
  sei(); //Very important to recover the interruption!!!
  return Calc;
}

void controlRelayByButton()
{
  if (LOW == digitalRead(key))//如果digitalRead 为低电平则执行下面的语句
  {
    //    Serial.println("RELEASE BUTTON!");
    digitalWrite(on,LOW);
  }//on 输出低电平
  else if (HIGH == digitalRead(key))//如果digitalRead 为高电平则执行下面的语句
  {
    //    Serial.println("PRESS BUTTON!");
    digitalWrite(on,HIGH);
  }//on 输出高电平
}

int readTemperature() {
  float temperature = getTemp();
  return (int) (temperature * 100);
}

float getTemp(){
  //returns the temperature from one DS18S20 in DEG Celsius

  byte data[12];
  byte addr[8];

  if ( !ds.search(addr)) {
    //no more sensors on chain, reset search
    ds.reset_search();
    return -1000;
  }

  if ( OneWire::crc8( addr, 7) != addr[7]) {
    //      Serial.println("CRC is not valid!");
    return -1000;
  }

  if ( addr[0] != 0x10 && addr[0] != 0x28) {
    //      Serial.print("Device is not recognized");
    return -1000;
  }

  ds.reset();
  ds.select(addr);
  ds.write(0x44,1); // start conversion, with parasite power on at the end

  byte present = ds.reset();
  ds.select(addr);    
  ds.write(0xBE); // Read Scratchpad


  for (int i = 0; i < 9; i++) { // we need 9 bytes
    data[i] = ds.read();
  }

  ds.reset_search();

  byte MSB = data[1];
  byte LSB = data[0];

  float tempRead = ((MSB << 8) | LSB); //using two's compliment
  float TemperatureSum = tempRead / 16;

  return TemperatureSum;

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
      // Receive command to turn down No.4 sluice to 50%
      if(rx.getData(0) == 'o' && rx.getData(1) == 'f' && rx.getData(2) == 'f' && rx.getData(3) == '2'){
         turnOnRelay(relayNo2Pin); // HIGH
         // set the sluice open flag to 0, means forcibly close it, do not care the sensor threshold.
         sluiceOpenFlag = 0;
         // If the No.2 sluice turned off, need to turn off the pump accordingly.
         turnOffRelay(relayMotorPin);
      } else if(rx.getData(0) == 'o' && rx.getData(1) == 'n' && rx.getData(2) == '2') {
         turnOffRelay(relayNo2Pin); // LOW
         sluiceOpenFlag = 1;
      } 
    } 
    else {
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
  } 
  else if (xbee.getResponse().isError()) {
    //nss.print("Error reading packet.  Error code: ");  
    //nss.println(xbee.getResponse().getErrorCode());
  }
}

void loop() {   
  // break down 10-bit reading into two bytes and place in payload
  pinLight = analogRead(A1);
  pinTemp = readTemperature();
  pinWaterLevel = analogRead(A5);
  pinPH = readDFPHSensor();//readPHSensor();
  pinHall = readHallSensor();
  // When water flow < 2L/H, just turn off the pump for now.
  if((pinHall < WATER_FLOW_THRESHOLD && pinWaterLevel < WATER_LEVEL_THRESHOLD) || !sluiceOpenFlag) {
    turnOffRelay(relayMotorPin);
  } else {
    turnOnRelay(relayMotorPin);
  }
  // data group 0: PH
  payload[0] = pinPH >> 8 & 0xff;
  payload[1] = pinPH & 0xff;
  // data group 1: Temperature
  payload[2] = pinTemp >>8 & 0xff;
  payload[3] = pinTemp & 0xff;
  // data group 2: hall sensor(water flow)
  payload[4] = pinHall >> 8 & 0xff;
  payload[5] = pinHall & 0xff;
  // data group 3: water level
  payload[6] = pinWaterLevel >> 8 & 0xff;
  payload[7] = pinWaterLevel & 0xff;
  // data group 4: light, for test
  payload[8] = pinLight >>8 & 0xff;
  payload[9] = pinLight & 0xff;

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
      } 
      else {
        // the remote XBee did not receive our packet. is it powered on?
        flashLed(errorLed, 3, 500);
      }
    } 
    else {
      // the remote XBee did not receive our packet. is it powered on?
      flashLed(errorLed, 3, 500);
    }
  } 
  else if (xbee.getResponse().isError()) {
    //nss.print("Error reading packet.  Error code: ");  
    //    nss.println(xbee.getResponse().getErrorCode());
  } 
  else {
    // local XBee did not provide a timely TX Status Response -- should not happen
    flashLed(errorLed, 10, 500);
  }
  readPacketIn();
  controlRelayByButton();
  delay(1000);
}

/* For DFROBOT PH Meter V1.1*/
int readDFPHSensor() {
  static unsigned long samplingTime = millis();
  static unsigned long printTime = millis();
  static float pHValue,voltage;
  pHArrayIndex=0;
  while(pHArrayIndex < ArrayLenth){
    if(millis()-samplingTime > samplingInterval)
    {
      pHArray[pHArrayIndex++]=analogRead(pHSensorPin);
      voltage = avergearray(pHArray, ArrayLenth)*5.0/1024;
      pHValue = 3.5*voltage+Offset;
      samplingTime=millis();

    }
    delay(20);
  }

  if(millis() - printTime > printInterval)   //Every 800 milliseconds, print a numerical, convert the state of the LED indicator
  {
    printTime=millis();
    return (int)(pHValue * 100);
  }
}

/* for dfrobot ph meter v1.1*/
double avergearray(int* arr, int number){
  int i;
  int max,min;
  double avg;
  long amount=0;
  if(number<=0){
    Serial.println("Error number for the array to avraging!/n");
    return 0;
  }
  if(number<5){   //less than 5, calculated directly statistics
    for(i=0;i<number;i++){
      amount+=arr[i];
    }
    avg = amount/number;
    return avg;
  }
  else{
    if(arr[0]<arr[1]){
      min = arr[0];
      max=arr[1];
    }
    else{
      min=arr[1];
      max=arr[0];
    }
    for(i=2;i<number;i++){
      if(arr[i]<min){
        amount+=min;        //arr<min
        min=arr[i];
      }
      else {
        if(arr[i]>max){
          amount+=max;    //arr>max
          max=arr[i];
        }
        else{
          amount+=arr[i]; //min<=arr<=max
        }
      }//if
    }//for
    avg = (double)amount/(number-2);
  }//if
  return avg;
}
