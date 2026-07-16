package com.mt.friotrackapi.mqtt.service;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Service
public class MqttSubscriberService {

    private static final Logger log = LoggerFactory.getLogger(MqttSubscriberService.class);

    private final MqttTelemetryIngestionService ingestionService;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String topic;
    private final String clientId;
    private MqttClient client;

    public MqttSubscriberService(
            MqttTelemetryIngestionService ingestionService,
            @Value("${friotrack.mqtt.enabled:true}") boolean enabled,
            @Value("${friotrack.mqtt.host:127.0.0.1}") String host,
            @Value("${friotrack.mqtt.port:1885}") int port,
            @Value("${friotrack.mqtt.username:friotrack_device}") String username,
            @Value("${friotrack.mqtt.password:}") String password,
            @Value("${friotrack.mqtt.topic:vehiculo/+}") String topic,
            @Value("${friotrack.mqtt.client-id:friotrack-api-subscriber}") String clientId
    ) {
        this.ingestionService = ingestionService;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.topic = topic;
        this.clientId = clientId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!enabled) {
            log.info("MQTT subscriber deshabilitado");
            return;
        }

        if (password == null || password.isBlank()) {
            log.warn("MQTT subscriber no iniciado: friotrack.mqtt.password esta vacio");
            return;
        }

        try {
            String brokerUri = "tcp://" + host + ":" + port;
            client = new MqttClient(brokerUri, clientId, new MemoryPersistence());
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    subscribe();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Conexion MQTT perdida: {}", cause == null ? "sin detalle" : cause.getMessage());
                }

                @Override
                public void messageArrived(String incomingTopic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    try {
                        ingestionService.ingest(incomingTopic, payload);
                        log.info("Mensaje MQTT procesado topic={} retained={} qos={}", incomingTopic, message.isRetained(), message.getQos());
                    } catch (Exception ex) {
                        log.warn("No se pudo procesar mensaje MQTT topic={}: {}", incomingTopic, ex.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Subscriber only.
                }
            });

            client.connect(connectOptions());
            log.info("MQTT subscriber conectado a {} topic={}", brokerUri, topic);
        } catch (MqttException ex) {
            log.warn("No se pudo iniciar MQTT subscriber: {}", ex.getMessage());
        }
    }

    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        return options;
    }

    private void subscribe() {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic, 1);
                log.info("MQTT subscriber suscrito a {}", topic);
            }
        } catch (MqttException ex) {
            log.warn("No se pudo suscribir a MQTT topic {}: {}", topic, ex.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        if (client == null) {
            return;
        }

        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException ex) {
            log.warn("No se pudo cerrar MQTT subscriber: {}", ex.getMessage());
        }
    }
}
