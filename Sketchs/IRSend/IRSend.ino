// Basado en IRremoteESP8266: IRrecvDumpV2

#include <Arduino.h>
#include <ArduinoJson.h>

//---
#include <ESP8266WiFi.h>
WiFiClient net;

//---
#define WIFI_SSID     "your wifi SID"
#define WIFI_PASS     "your wifi password"
#define MQTT_SERVER   "test.mosquitto.org"
#define MQTT_PORT     1883
#define MQTT_TOPIC    "rcr/ircontrol"

// modificar PubSubClient.h: #define MQTT_MAX_PACKET_SIZE 1024
#include <PubSubClient.h>
PubSubClient mqtt( net );

char mqtt_clientID[ 128 ];

//---
#define IR_SEND_PIN               D5

#include <IRsend.h>
#include <IRremoteESP8266.h>

IRsend irsend( IR_SEND_PIN );


void setup() {
  pinMode( IR_SEND_PIN, OUTPUT );

  randomSeed( analogRead(0) );

  Serial.begin( 115200, SERIAL_8N1, SERIAL_TX_ONLY );
  while ( !Serial ) delay( 50 );
  Serial.println();
  Serial.println();

  WiFi.mode( WIFI_STA );
  WiFi.setAutoConnect( true );
  WiFi.begin( WIFI_SSID, WIFI_PASS );

  mqtt.setServer( MQTT_SERVER, MQTT_PORT );
  mqtt.setCallback( doReceiveMessage );

  while( !wifiReconnect() || !mqttReconnect() );
}


bool wifiReconnect(){
  if( WiFi.status() == WL_CONNECTED )
    return true;

  Serial.print( "Conectando a la WiFi: ." );
  for( int i=0; i<10; i++ ){
    if( WiFi.status() == WL_CONNECTED ){
      Serial.println( " Ok" );
      return true;
    }

    Serial.print( "." );
    delay( 500 );
  }
  Serial.println( "Timeout" );
  return false;
}


bool mqttReconnect (){
  if( mqtt.connected() )
    return true;

  sprintf( mqtt_clientID, "Node_%04X_%08X", random( 4096 ), ESP.getChipId() );

  Serial.print( "Conectando a MQTT: ." );
  for( int i=0; i<10; i++ ){
    if( mqtt.connect( mqtt_clientID ) ){
      mqtt.subscribe( MQTT_TOPIC, 0 );
      Serial.println( " Ok" );
      return true;
    }

    Serial.print( "." );
    delay( 500 );
  }
  Serial.println( "Timeout" );
  return false;
}


void loop() {
  if( !wifiReconnect() )
    return;

  if( !mqttReconnect() )
    return;

  mqtt.loop();
  delay( 100 );
}

void doReceiveMessage( char *topic, byte *b_payload, unsigned int len  ) {
  char payload[len+1];
  memcpy( payload, b_payload, len );
  payload[len] = 0;

  StaticJsonBuffer<1024> jsonBuffer;
  JsonObject& json = jsonBuffer.parseObject( payload );
  JsonArray& jdata = json["data"];
  String id = json["id"];

  uint16_t n = jdata.size();
  uint16_t data[n];
  for( uint16_t i=0; i<n; i++ )
    data[i] = jdata[i];
  irsend.sendRaw( data, n, 38 );  // Send a raw data at 38kHz.

  Serial.println( id );
  Serial.println();
}
