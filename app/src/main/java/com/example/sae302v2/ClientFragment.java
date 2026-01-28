package com.example.sae302v2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import android.content.res.ColorStateList;
import android.graphics.Color;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientFragment extends Fragment {
    private EditText editIp, editPort, editUser, editMsg;
    private TextView txtHistory;
    private Button btnConnect, btnSend;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DatagramSocket udpSocket; // Variable pour le mode UDP
    private boolean isConnected = false;
    private RadioGroup radioGroupProto;
    private RadioButton radioTcp, radioUdp;

    private int getThemeColor(int attrResId) {android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client, container, false);

        editIp = view.findViewById(R.id.edit_ip);
        editPort = view.findViewById(R.id.edit_port);
        editUser = view.findViewById(R.id.edit_user);
        editMsg = view.findViewById(R.id.edit_message);
        txtHistory = view.findViewById(R.id.txt_history_client);
        btnConnect = view.findViewById(R.id.btn_connect);
        btnSend = view.findViewById(R.id.btn_send);
        radioGroupProto = view.findViewById(R.id.radio_group_proto);
        radioTcp = view.findViewById(R.id.radio_tcp);
        radioUdp = view.findViewById(R.id.radio_udp);

        btnConnect.setOnClickListener(v -> {
            if (!isConnected) connect();
            else disconnect();
        });

        btnSend.setOnClickListener(v -> sendMessage());
        btnConnect.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#388E3C")));
        return view;
    }

    private void connect() {
        String ip = editIp.getText().toString();
        String portStr = editPort.getText().toString();
        String pseudo = editUser.getText().toString();

        if (ip.isEmpty() || portStr.isEmpty() || pseudo.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);
        setFieldsEnabled(false);
        btnConnect.setEnabled(false);
        btnConnect.setText("Connexion...");

        new Thread(() -> {
            try {
                // Résolution de l'adresse IP (doit être dans le thread)
                InetAddress addr = InetAddress.getByName(ip);

                if (radioTcp.isChecked()) {
                    // --- LOGIQUE TCP ---
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(addr, port), 10000);
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out.println(pseudo);

                    isConnected = true;
                    listenForMessages();
                } else {
                    // --- LOGIQUE UDP ---
                    udpSocket = new DatagramSocket();
                    // Important : ne pas limiter la socket à une adresse pour pouvoir recevoir

                    String hello = "HELLO:" + pseudo;
                    byte[] buf = hello.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, port);

                    // Envoi du premier paquet
                    udpSocket.send(packet);

                    isConnected = true;
                    listenForUdpMessages();
                }

                // Mise à jour de l'UI en cas de succès
                new Handler(Looper.getMainLooper()).post(() -> {
                    updateUI("Connecté via " + (radioTcp.isChecked() ? "TCP" : "UDP"));
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Déconnexion");
                    btnConnect.setBackgroundTintList(ColorStateList.valueOf(getThemeColor(R.attr.colorRed)));
                });

            } catch (Exception e) {
                // Capture toutes les exceptions (IOException, UnknownHostException, etc.)
                new Handler(Looper.getMainLooper()).post(() -> {
                    updateUI("Erreur connexion : " + e.getMessage());
                    setFieldsEnabled(true);
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Connexion");
                    btnConnect.setBackgroundTintList(ColorStateList.valueOf(getThemeColor(R.attr.colorGreen)));
                });
            }
        }).start();
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                String line;
                // Si readLine() renvoie null, cela signifie que le serveur a fermé la connexion
                while (isConnected && in != null && (line = in.readLine()) != null) {
                    updateUI(line);
                }
            } catch (IOException e) {
                if (isConnected) updateUI("Connexion perdue avec le serveur.");
            } finally {
                // Si on sort de la boucle, on force la déconnexion de l'interface
                new Handler(Looper.getMainLooper()).post(this::disconnect);
            }
        }).start();
    }

    private void listenForUdpMessages() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[2048];
                while (isConnected) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet); // Attends un message du serveur
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    updateUI(msg);
                }
            } catch (IOException e) {
                if (isConnected) updateUI("Lien UDP fermé.");
            }
        }).start();
    }

    private void sendMessage() {
        String msgContent = editMsg.getText().toString();
        String pseudo = editUser.getText().toString();
        if (msgContent.isEmpty()) return;

        new Thread(() -> {
            try {
                // CRÉATION DE LA STRUCTURE COMPLEXE
                org.json.JSONObject json = new org.json.JSONObject();
                json.put("pseudo", pseudo);
                json.put("message", msgContent);
                json.put("timestamp", System.currentTimeMillis());
                json.put("proto", radioTcp.isChecked() ? "TCP" : "UDP");

                String jsonString = json.toString(); // Conversion en texte pour l'envoi

                if (radioTcp.isChecked()) {
                    if (out != null) out.println(jsonString);
                } else {
                    byte[] data = jsonString.getBytes();
                    InetAddress addr = InetAddress.getByName(editIp.getText().toString());
                    int port = Integer.parseInt(editPort.getText().toString());
                    DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
                    udpSocket.send(packet);
                    updateUI("Moi (UDP/JSON): " + msgContent);
                }

                new Handler(Looper.getMainLooper()).post(() -> editMsg.setText(""));
            } catch (Exception e) {
                updateUI("Erreur d'envoi JSON");
            }
        }).start();
    }

    private void disconnect() {
        if (!isConnected) return; // Évite de déconnecter deux fois

        isConnected = false;
        new Thread(() -> {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
                if (udpSocket != null) udpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // UI Update
        new Handler(Looper.getMainLooper()).post(() -> {
            setFieldsEnabled(true);
            btnConnect.setText("Connexion");
            btnConnect.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#388E3C")));
            updateUI("Vous avez été déconnecté.");
        });
    }

    private void updateUI(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            // On récupère l'historique actuel
            String oldHistory = txtHistory.getText().toString();
            // On insère le nouveau message au début
            txtHistory.setText(message + "\n" + oldHistory);
        });
    }

    // Petite méthode utilitaire pour éviter la répétition
    private void setFieldsEnabled(boolean enabled) {
        editIp.setEnabled(enabled);
        editPort.setEnabled(enabled);
        editUser.setEnabled(enabled);
        radioTcp.setEnabled(enabled);
        radioUdp.setEnabled(enabled);
    }
}