#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import paho.mqtt.client as paho
import json
import time
import uuid
import unicodedata
try:
    import Queue
except:
    import queue as Queue

MQTT_SERVER = 'test.mosquitto.org'
MQTT_PORT = 1883
TOPIC_RCONTROL = 'rcr/rcontrol'
TOPIC_IRCODE = 'rcr/ircontrol'

messages = Queue.Queue( 1 )

# My TV remote control codes
rcontrol = {}
rcontrol[ 'pwr'] = '{"id":"28A8","data":[8340,4074,564,494,540,502,540,1526,562,480,562,1526,562,472,562,472,564,470,562,4104,562,1526,562,480,562,1526,564,482,560,1526,564,472,560,472,562,470,562]}'
rcontrol[ '1' ] = '{"id":"2880","data":[8342,4074,566,494,540,482,560,1524,564,504,538,1550,538,496,540,470,562,470,562,4106,562,1550,538,474,562,470,564,496,538,470,564,496,538,474,562,468,564]}'
rcontrol[ '2' ] = '{"id":"2840","data":[8292,4126,512,522,512,530,512,1578,512,532,510,1578,512,522,512,522,512,520,512,4148,512,532,512,1578,512,522,512,522,510,524,510,522,512,522,510,520,512]}'
rcontrol[ '3' ] = '{"id":"28C0","data":[8294,4128,512,522,512,532,512,1578,512,532,512,1578,512,524,512,522,512,520,512,4156,512,1586,512,1578,512,522,512,522,512,522,512,522,512,522,512,520,512]}'
rcontrol[ '4' ] = '{"id":"2820","data":[8344,4076,562,472,560,484,560,1550,538,480,562,1526,564,472,562,472,562,468,564,4120,538,472,560,482,562,1528,562,468,564,470,562,472,564,470,564,470,562]}'
rcontrol[ '5' ] = '{"id":"28A0","data":[8292,4128,510,524,512,532,510,1578,510,532,512,1578,512,522,512,522,512,520,512,4158,512,1578,512,532,510,1578,510,524,512,522,512,522,510,522,512,520,512]}'
rcontrol[ '6' ] = '{"id":"2860","data":[8344,4074,562,472,562,482,562,1526,564,480,562,1526,562,472,562,472,562,470,562,4098,562,478,564,1534,564,1526,564,470,564,470,564,470,564,470,564,470,564]}'
rcontrol[ '7' ] = '{"id":"28E0","data":[8294,4126,512,522,512,532,512,1578,512,530,512,1578,512,522,512,522,512,520,512,4156,512,1586,512,1586,512,1578,512,524,510,522,512,524,510,522,512,520,512]}'
rcontrol[ '8' ] = '"id":"2810","data":[8292,4128,510,524,512,530,512,1578,512,532,512,1578,512,522,510,524,512,520,512,4148,512,522,512,522,512,532,510,1578,512,524,512,522,512,522,512,520,512]}'
rcontrol[ '9' ] = '{"id":"2890","data":[8294,4126,512,522,512,532,510,1578,512,532,512,1578,512,522,512,522,514,520,510,4156,512,1578,512,522,512,532,512,1576,514,522,512,522,512,522,512,520,512]}'
rcontrol[ '0' ] = '{"id":"2800","data":[8294,4126,512,522,512,532,510,1578,512,532,512,1578,512,522,512,522,512,520,512,4148,512,522,512,522,512,522,512,522,512,522,512,522,512,522,512,520,512]}'
rcontrol[ 'vol+' ] = '{"id":"28C8","data":[8314,4126,512,522,512,532,510,1578,512,532,512,1578,510,524,510,522,512,520,512,4156,512,1586,512,1578,510,522,514,530,512,1578,512,522,512,522,512,522,512]}'
rcontrol[ 'vol-' ] = '{"id":"2828","data":[8292,4126,512,522,512,530,512,1578,512,532,510,1578,512,522,512,522,512,520,512,4150,510,524,510,532,512,1578,512,532,510,1576,512,522,512,522,512,520,512]}'
rcontrol[ 'ch+' ] = '{"id":"28E8","data":[8292,4128,512,522,512,532,510,1578,510,532,510,1578,512,524,510,526,508,522,510,4158,510,1586,512,1586,512,1578,512,530,512,1578,510,524,512,522,512,522,510]}'
rcontrol[ 'ch-' ] = '{"id":"2818","data":[8292,4138,500,522,512,532,512,1578,512,530,514,1576,512,522,512,522,512,522,512,4146,512,522,512,522,510,532,512,1586,512,1578,510,524,510,522,510,522,512]}'


def mqtt_on_connect( mqtt_client, userdata, flags, rc ):
    global MQTT_SERVER, MQTT_PORT, TOPIC_RCONTROL

    if( rc == 0 ):
        mqtt_client.subscribe( TOPIC_RCONTROL )
        print( "[Test] Esperando en mqtt://%s:%s - %s" % ( MQTT_SERVER, MQTT_PORT, TOPIC_RCONTROL ) )
    else:
        print( "[Test] Sin conexión con mqtt://%s:%s" % ( MQTT_SERVER, MQTT_PORT ) )


def mqtt_on_disconnect( mqtt_client, userdata, rc ):
    global MQTT_SERVER, MQTT_PORT

    print( "[Test] Desconectado de mqtt://%s:%s" % ( MQTT_SERVER, MQTT_PORT ) )


def mqtt_on_message( mqtt_client, userdata, message ):
    global messages

    # si no se ha procesado el ultimo mensaje lo eliminamos
    try:
        messages.get_nowait()
    except Queue.Empty:
        pass

    # agregamos el mensaje
    try:
        messages.put_nowait( message )
    except Queue.Full:
            pass




def main():
    global MQTT_SERVER, MQTT_PORT, TOPIC_IRCODE, messages

    print( '[Test] Iniciando sistema' )

    mqtt_client = paho.Client( 'Test-' + uuid.uuid4().hex )
    mqtt_client.on_connect = mqtt_on_connect
    mqtt_client.on_disconnect = mqtt_on_disconnect
    mqtt_client.on_message = mqtt_on_message
    mqtt_client.connect( MQTT_SERVER, MQTT_PORT )
    mqtt_client.loop_start()
    abort = False
    while( not abort ):
        message = messages.get()

        # hacemos el manejo del payload que viene en utf-8 (se supone)
        # la idea es cambiar tildes y otros caracteres especiales
        # y llevar todo a minuscula
        cmd = message.payload.decode('utf-8').lower()
        cmd = ''.join((c for c in unicodedata.normalize('NFD', cmd) if unicodedata.category(c) != 'Mn'))
        print( "[Test] Mensaje recibido:", message.payload, "<<" + cmd + ">>" )

        # comandos recibidos
        if( cmd == 'salir' ):
            abort = True
        else:
            try:
                code = rcontrol[cmd]
                print( code )
                mqtt_client.publish( TOPIC_IRCODE, code )
            except:
                pass

    mqtt_client.loop_stop()
    print( '[Test] Finalizando' )


#--
main()
