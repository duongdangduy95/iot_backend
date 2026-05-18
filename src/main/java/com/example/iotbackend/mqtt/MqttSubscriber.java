package com.example.iotbackend.mqtt;

import com.example.iotbackend.service.PairingService;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MqttSubscriber implements MqttCallback {

    private final MqttClient mqttClient;
    private final PairingService pairingService;

    @jakarta.annotation.PostConstruct
    public void init() throws Exception {

        mqttClient.setCallback(this);

        mqttClient.subscribe("devices/pair/request");

        mqttClient.subscribe("devices/+/register");

        mqttClient.subscribe("devices/+/state");

        mqttClient.subscribe("devices/+/online");
    }

    @Override
    public void connectionLost(Throwable cause) {

        System.out.println("MQTT disconnected");
    }

    @Override
    public void messageArrived(
            String topic,
            MqttMessage message
    ) {

        String payload =
                new String(message.getPayload());

        System.out.println(topic);
        System.out.println(payload);

        //
        // PAIR REQUEST
        //
        if (topic.equals("devices/pair/request")) {

            pairingService.handlePairing(payload);
        }
    }

    @Override
    public void deliveryComplete(
            IMqttDeliveryToken token
    ) {

    }
}