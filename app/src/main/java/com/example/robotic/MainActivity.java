package com.example.robotic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Button connect, disconnect, publish, subscribe, unsubscribe;
    private EditText C_host, C_port, P_topic, P_data, S_topic, S_QOS;
    private ListView ListFull;
    private IMqttToken token;
    private MqttAndroidClient client;
    private String clientId, uri;
    private boolean connectSuccess;
    private ArrayList<String> listItems;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect = findViewById(R.id.Connect_button);
        disconnect = findViewById(R.id.Disconnect_button);
        subscribe = findViewById(R.id.Subscribe_button);
        unsubscribe = findViewById(R.id.Unsubscribe_button);
        publish = findViewById(R.id.Publish_button);
        ListFull = findViewById(R.id.MainList);
        C_host = findViewById(R.id.Host_Connect);
        C_port = findViewById(R.id.Port_Connect);
        P_topic = findViewById(R.id.Topic_Publish);
        P_data = findViewById(R.id.Data_Publish);
        S_topic = findViewById(R.id.Topic_Subscribe);
        S_QOS = findViewById(R.id.QOS_Subscribe);

        connectSuccess = false;

        clientId = MqttClient.generateClientId();

        listItems = new ArrayList<String>();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        ListFull.setAdapter(adapter);

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (internet_connection()){
                    try {
                        uri = "tcp://" + C_host.getText().toString() + ":"+ C_port.getText().toString();
                        client = new MqttAndroidClient(MainActivity.this, uri, clientId);
                        token = client.connect();
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                connectSuccess = true;
                                Toast.makeText(MainActivity.this, "Connected to WiFi", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Toast.makeText(MainActivity.this, "Your connection failure",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Your connection failed, please check host and port",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                if (!internet_connection()) {
                    Toast.makeText(MainActivity.this, "No network connection!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(internet_connection() && (connectSuccess)) {
                    try {
                        token = client.unsubscribe(String.valueOf(S_topic.getText()));
                        token = client.disconnect();
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                connectSuccess = false;
                                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                                S_topic.setText("");
                                S_QOS.setText("");
                                listItems.clear();
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Toast.makeText(MainActivity.this, "Problem occurred! probably disconnected", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please connect first!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (internet_connection() && (connectSuccess)) {
                        if (!(P_topic.getText().length() == 0)) {
                            client.publish(String.valueOf(P_topic.getText()), P_data.getText().toString().getBytes(),
                                    0,false);
                        } else {
                            Toast.makeText(MainActivity.this, "Add a topic", Toast.LENGTH_SHORT).show();
                        }
                    } else{
                        Toast.makeText(MainActivity.this, "Connect to a broker", Toast.LENGTH_SHORT).show();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (internet_connection() && (connectSuccess) && (S_topic.getText().length() > 0) && S_QOS.getText().length() > 0) {
                        int i = Integer.parseInt(String.valueOf(S_QOS.getText()));
                        IMqttToken subToken = client.subscribe(String.valueOf(S_topic.getText()), i);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                client.setCallback(new MqttCallback() {
                                    @Override
                                    public void connectionLost(Throwable cause) {
                                        Toast.makeText(MainActivity.this, "Connection lost", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                                        if ((!new String(message.getPayload()).equals("the payload"))) {
                                            String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
                                            adapter.insert(currentDateTimeString + "\n" + topic + ": " + new String(message.getPayload()), 0);
                                        }
                                    }

                                    @Override
                                    public void deliveryComplete(IMqttDeliveryToken token) {
                                    }
                                });
                            }
                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Toast.makeText(MainActivity.this, "Subscription failed: " + asyncActionToken, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Subscription problem. Check again", Toast.LENGTH_SHORT).show();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        unsubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (internet_connection() && (connectSuccess)) {
                        token = client.unsubscribe(String.valueOf(S_topic.getText()));
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Toast.makeText(MainActivity.this, "Unsubscribed", Toast.LENGTH_SHORT).show();
                                S_topic.setText("");
                                S_QOS.setText("");
                                listItems.clear();
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            }
                        });
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    boolean internet_connection(){
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}