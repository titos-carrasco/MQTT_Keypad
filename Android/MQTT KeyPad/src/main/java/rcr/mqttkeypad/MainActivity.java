package rcr.mqttkeypad;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
//import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
//import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

//public class MainActivity extends AppCompatActivity implements View.OnClickListener {
public class MainActivity extends Activity implements View.OnClickListener {
    private EditText txtServer;
    private EditText txtTopic;
    private TextView txtStatus;
    private MqttAndroidClient mqttAndroidClient = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        //Log.d( "onCreate", "Begin" );

        txtServer = findViewById( R.id.txtServer );
        txtTopic  = findViewById( R.id.txtTopic );
        txtStatus  = findViewById( R.id.txtStatus );

        txtStatus.setText( getString( R.string.Disconnected ) );

        findViewById( R.id.btnK_Power ).setOnClickListener( this );
        findViewById( R.id.btnK_1 ).setOnClickListener( this );
        findViewById( R.id.btnK_2).setOnClickListener( this );
        findViewById( R.id.btnK_3 ).setOnClickListener( this );
        findViewById( R.id.btnK_4 ).setOnClickListener( this );
        findViewById( R.id.btnK_5 ).setOnClickListener( this );
        findViewById( R.id.btnK_6 ).setOnClickListener( this );
        findViewById( R.id.btnK_7 ).setOnClickListener( this );
        findViewById( R.id.btnK_8 ).setOnClickListener( this );
        findViewById( R.id.btnK_9 ).setOnClickListener( this );
        findViewById( R.id.btnK_0 ).setOnClickListener( this );
        findViewById( R.id.btnK_VOL_UP ).setOnClickListener( this );
        findViewById( R.id.btnK_VOL_DOWN ).setOnClickListener( this );
        findViewById( R.id.btnK_CH_UP ).setOnClickListener( this );
        findViewById( R.id.btnK_CH_DOWN ).setOnClickListener( this );
        findViewById( R.id.btnK_Connect ).setOnClickListener( this );

        //Log.d( "onCreate", "End" );
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Log.d( "onStart", "Begin" );
        //Log.d( "onStart", "End" );
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Log.d( "onResume", "Begin" );

        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        String savedServer = sharedPref.getString( getString( R.string.SavedServerKey ), getString( R.string.DefaultServer ) );
        String savedTopic = sharedPref.getString( getString( R.string.SavedTopicKey ), getString( R.string.DefaultTopic ) );
        txtServer.setText( savedServer );
        txtTopic.setText( savedTopic );

        //Log.d( "onResume", "End" );
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Log.d( "onPause", "Begin" );

        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString( getString( R.string.SavedServerKey ), txtServer.getText().toString() );
        editor.putString( getString( R.string.SavedTopicKey ), txtTopic.getText().toString() );
        editor.apply();

        //Log.d( "onPause", "End" );
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Log.d( "onStop", "Begin" );

        mqttDisconnect();

        //Log.d( "onStop", "End" );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Log.d( "onDestroy", "Begin" );
        //Log.d( "onDestroy", "End" );
    }

    @Override
    public void onClick( View v ) {
        //Log.d( "onClick", "Begin" );
        int id = v.getId();
        if (id == R.id.btnK_Power || id == R.id.btnK_1 || id == R.id.btnK_2 || id == R.id.btnK_3 || id == R.id.btnK_4 || id == R.id.btnK_5 || id == R.id.btnK_6 || id == R.id.btnK_7 || id == R.id.btnK_8 || id == R.id.btnK_9 || id == R.id.btnK_0 || id == R.id.btnK_VOL_UP || id == R.id.btnK_VOL_DOWN || id == R.id.btnK_CH_UP || id == R.id.btnK_CH_DOWN) {
            String topic = ((Button) v).getText().toString();
            mqttPublish(txtTopic.getText().toString(), topic);
        } else if (id == R.id.btnK_Connect) {
            mqttConnect();
        }
        //Log.d( "onClick", "End" );
    }

    private void mqttConnect() {
        //Log.d( "mqttConnect", "Begin" );

        String serverUri = "tcp://" + txtServer.getText().toString();
        if( mqttAndroidClient != null ) {
            if( !mqttAndroidClient.isConnected() || !mqttAndroidClient.getServerURI().equals( serverUri ) ) {
                mqttDisconnect();
            }
        }

        if( mqttAndroidClient == null ) {
            txtStatus.setText( getString( R.string.Connecting ) );

            String clientId = "VAM-" + UUID.randomUUID();
            mqttAndroidClient = new MqttAndroidClient( getApplicationContext(), serverUri, clientId );
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete( boolean b, String s ) {
                    txtStatus.setText( getString( R.string.Connected ) );
                    Toast.makeText( getApplicationContext(), getString( R.string.ConnectCompleted ), Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void connectionLost( Throwable throwable ) {
                    txtStatus.setText( getString( R.string.Disconnected ) );
                    Toast.makeText( getApplicationContext(), getString( R.string.ConnectionLost ), Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void messageArrived( String topic, MqttMessage mqttMessage ) {
                    Toast.makeText( getApplicationContext(), getString( R.string.MessageArrived ), Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void deliveryComplete( IMqttDeliveryToken iMqttDeliveryToken ) {
                    Toast.makeText( getApplicationContext(), getString( R.string.DeliveryCompleted ), Toast.LENGTH_SHORT ).show();
                }
            });

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect( false ) ;
            mqttConnectOptions.setCleanSession( true );
            try {
                mqttAndroidClient.connect( mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess( IMqttToken asyncActionToken ) {
                        txtStatus.setText( getString( R.string.Connected ) );
                        Toast.makeText( getApplicationContext(), getString( R.string.ConnectSuccess ), Toast.LENGTH_SHORT ).show();
                    }

                    @Override
                    public void onFailure( IMqttToken asyncActionToken, Throwable exception ) {
                        txtStatus.setText( getString( R.string.Disconnected ) );
                        Toast.makeText( getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT ).show();
                    }
                });
            } catch ( MqttException e ) {
                txtStatus.setText( e.getMessage() );
                //Log.d("mqttConnect", e.getMessage() );
            }
        }

        //Log.d( "mqttConnect", "End" );
    }

    private void mqttDisconnect() {
        //Log.d("mqttDisconnect", "Begin" );

        if( mqttAndroidClient != null ) {
            mqttAndroidClient.close();
            mqttAndroidClient = null;
            txtStatus.setText( getString( R.string.Disconnected ) );
        }

        //Log.d("mqttDisconnect", "End" );
    }

    private void mqttPublish( String topic, String payload ) {
        //Log.d("mqttPublish", "Begin");

        if( mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setPayload(payload.getBytes());
            try {
                mqttAndroidClient.publish( topic, message );
                //Log.d( "mqttPublish", topic );
                //Log.d( "mqttPublish", payload );
                //Log.d( "mqttPublish", getString( R.string.Published ) );
            } catch ( MqttException e ) {
                //Log.d("mqttPublish", e.getMessage() );
            }
        }

        //Log.d("mqttPublish", "End");
    }

}
