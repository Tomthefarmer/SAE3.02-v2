package com.example.sae302v2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

// MQTT
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class CarteFragment extends Fragment {

    private MapView map = null;
    private TextView tvCoordinates;
    private GeoPoint lastKnownLocation = null;
    private MqttClient mqttClient;

    // Configuration MQTT
    // Broker URL (Public address)
    private final String BROKER_URL = "tcp://eu1.cloud.thethings.network:1883";

    // Username
    private final String APP_ID = "thomas-clement-handy-gps";

    // Device ID
    private final String DEVICE_ID = "seeeduino-lorawan-gps";

    // Password
    private final String API_KEY = "NNSXS.S5GAUQ7LLDVHBXBI6K6PPHTRN64TH6NO5ULRA5Y.NDULIYEJXOCMKWTCLWQA2C2VDARTJ43OFLOVPCLPMZRE5ID2YZFQ";

    // Le topic standard TTN v3 pour s'abonner aux messages montants (uplink)
    private final String TOPIC = "v3/" + APP_ID + "@ttn/devices/" + DEVICE_ID + "/up";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Initialisation d'OSM droid (Gestion des tuiles de la carte)
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));

        View v = inflater.inflate(R.layout.fragment_carte, container, false);
        map = v.findViewById(R.id.map);
        tvCoordinates = v.findViewById(R.id.tv_coordinates);
        map.setMultiTouchControls(true);

        // Bouton de recentrage sur le dernier marqueur reçu
        View btnCenter = v.findViewById(R.id.btn_center);
        btnCenter.setOnClickListener(view -> {
            if (lastKnownLocation != null) {
                map.getController().animateTo(lastKnownLocation);
                map.getController().setZoom(18.0);
            }
        });

        // Position par défaut au démarrage (IUT de Roanne)
        GeoPoint startPoint = new GeoPoint(46.0445, 4.0728);
        map.getController().setZoom(15.0);
        map.getController().setCenter(startPoint);

        // Lancement de la connexion client MQTT
        connecterMQTT();

        return v;
    }

    private void connecterMQTT() {
        try {
            String clientId = MqttClient.generateClientId();
            mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(APP_ID + "@ttn"); // Username TTN
            options.setPassword(API_KEY.toCharArray()); // Password (API KEY)
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT_TTN", "Connexion perdue : " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d("MQTT_TTN", "Message reçu : " + payload);

                    // Analyse du JSON reçu de TTN pour extraire la latitude et la longitude
                    JSONObject json = new JSONObject(payload);
                    JSONObject decoded = json.getJSONObject("uplink_message")
                            .getJSONObject("decoded_payload");

                    if (decoded.has("latitude") && decoded.has("longitude")) {
                        double lat = decoded.getDouble("latitude");
                        double lng = decoded.getDouble("longitude");

                        // Mise à jour de l'interface graphique sur le thread principal (UI Thread)
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> ajouterMarqueur(lat, lng));
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });

            mqttClient.connect(options);
            mqttClient.subscribe(TOPIC);
            Log.d("MQTT_TTN", "Succès : Connecté au broker et abonné au topic");

        } catch (MqttException e) {
            Log.e("MQTT_TTN", "Erreur lors de la connexion MQTT : " + e.getMessage());
        }
    }

    private void ajouterMarqueur(double lat, double lng) {
        if (map == null) return;

        lastKnownLocation = new GeoPoint(lat, lng);
        tvCoordinates.setText(String.format(Locale.FRANCE, "Latitude: %.6f\nLongitude: %.6f", lat, lng));

        Marker marker = new Marker(map);
        marker.setPosition(lastKnownLocation);

        // Récupère l'image depuis les ressources
        android.graphics.drawable.Drawable customIcon = androidx.core.content.res.ResourcesCompat.getDrawable(getResources(), R.drawable.ic_marqueur_xml, null);
        marker.setIcon(customIcon);
        // Ajuste l'ancrage pour que la pointe de l'image soit sur les coordonnées
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setTitle("Position Capteur");

        // Nettoie l'ancien marqueur et affiche le nouveau
        map.getOverlays().clear();
        map.getOverlays().add(marker);
        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            // Fermeture propre de la connexion pour libérer les ressources
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}