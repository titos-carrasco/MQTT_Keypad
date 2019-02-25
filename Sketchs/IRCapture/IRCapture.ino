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

//---
#define IR_RECV_PIN               D5
#define IR_CAPTURE_BUFFER_SIZE    1024
#define IR_TIMEOUT                15
#define IR_MIN_UNKNOWN_SIZE       12

#include <IRrecv.h>
#include <IRremoteESP8266.h>
#include <IRutils.h>

IRrecv irrecv( IR_RECV_PIN, IR_CAPTURE_BUFFER_SIZE, IR_TIMEOUT, true );
decode_results irResults;  


void setup() {
  pinMode( IR_RECV_PIN, INPUT );

  randomSeed( analogRead(0) );
  
  Serial.begin( 115200, SERIAL_8N1, SERIAL_TX_ONLY );
  while ( !Serial ) delay( 50 );
  Serial.println(); 
  Serial.println();

  WiFi.mode( WIFI_STA );
  WiFi.setAutoConnect( true );
  WiFi.begin( WIFI_SSID, WIFI_PASS );
  wifiReconnect();
  
  mqtt.setServer( MQTT_SERVER, MQTT_PORT );
  mqttReconnect();

  irrecv.setUnknownThreshold( IR_MIN_UNKNOWN_SIZE );
  irrecv.enableIRIn();
}


bool wifiReconnect(){
  if( WiFi.status() == WL_CONNECTED )
    return true;

  Serial.print( "Conectando a la WiFi: ." );
  Serial.flush();
  for( int i=0; i<10; i++ ){
    if( WiFi.status() == WL_CONNECTED ){
      Serial.println( " Ok" );
      Serial.flush();
      return true;
    }
        
    Serial.print( "." );
    Serial.flush();
    delay( 500 );
  }
  Serial.println( "Timeout" );
  Serial.flush();
  return false;
}


bool mqttReconnect (){
  if( mqtt.connected() )
    return true;

  char clientID[ 128 ];
  sprintf( clientID, "Node_%04X_%08X", random( 4096 ), ESP.getChipId() );

  Serial.print( "Conectando a MQTT: ." );
  Serial.flush();
  for( int i=0; i<10; i++ ){
    if( mqtt.connect( clientID ) ){
      Serial.println( " Ok" );
      Serial.flush();
      return true;
    }
        
    Serial.print( "." );
    Serial.flush();
    delay( 500 );
  }
  Serial.println( "Timeout" );
  Serial.flush();
  return false;
}

 
void loop() {
  if( !wifiReconnect() )
    return;

  if( !mqttReconnect() )
    return;
    
  if( !irrecv.decode( &irResults) )
    return;
    
  if ( irResults.overflow ){
    Serial.println( "WARNING IR code too big!!!" );
    //Serial.flush();
  }

  sendToMQTTServer( &irResults );
  yield();
}


void sendToMQTTServer( const decode_results *results ){
  StaticJsonBuffer<1024> jsonBuffer;
  JsonObject& json = jsonBuffer.createObject();

  json["id"] = uint64ToString(results->value, 16 );
  
  JsonArray& data = json.createNestedArray( "data" );
  for ( uint16_t j=0, i = 1; i < results->rawlen; i++ ){
    uint32_t usecs;
    for ( usecs = results->rawbuf[i] * kRawTick; usecs > UINT16_MAX;usecs -= UINT16_MAX )
      data.add( UINT16_MAX );
    data.add( usecs );
  }

  if( mqtt.connected() ){
    char buffer[1024];
    json.printTo( buffer );
    mqtt.publish( MQTT_TOPIC, buffer );
    mqtt.flush();
    mqtt.loop();
    net.flush();

    json.printTo( Serial );
    Serial.println();
    Serial.flush();  
}
}
