package com.example.sae302v2;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServeurFragment extends Fragment {
    private TextView txtIp, txtLogs;
    private EditText editPort;
    private Button btnToggle;
    private ServerSocket serverSocket;
    private DatagramSocket udpServerSocket;
    private boolean isRunning = false;
    private List<ClientHandler> clients = new ArrayList<>();

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private int getThemeColor(int attrResId) {android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_serveur, container, false);

        txtIp = view.findViewById(R.id.txt_server_ip);
        txtLogs = view.findViewById(R.id.txt_history_server);
        editPort = view.findViewById(R.id.edit_server_port);
        btnToggle = view.findViewById(R.id.btn_toggle_server);

        txtIp.setText("IP: " + getLocalIpAddress());

        btnToggle.setOnClickListener(v -> {
            if (!isRunning) startServer();
            else stopServer();
        });

        return view;
    }

    private void startServer() {
        String portStr = editPort.getText().toString();
        if (portStr.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez saisir un port", Toast.LENGTH_SHORT).show();
            return;
        }

        editPort.setEnabled(false);
        int port = Integer.parseInt(portStr);
        isRunning = true;
        btnToggle.setText("Éteindre le serveur");
        btnToggle.setBackgroundTintList(ColorStateList.valueOf(getThemeColor(R.attr.colorRed)));

        // Thread TCP
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Serveur démarré sur le port " + port);
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                log("Serveur arrêté.");
            }
        }).start();

        // Thread UDP
        new Thread(() -> {
            try {
                udpServerSocket = new DatagramSocket(port);
                log("Serveur UDP démarré sur " + port);
                byte[] buffer = new byte[1024];
                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpServerSocket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    // Ici vous devriez loguer : "Message reçu via UDP"
                    log("(UDP) " + packet.getAddress() + " : " + received);

                    // Pour répondre en UDP, il faut renvoyer au packet.getAddress() et packet.getPort()
                }
            } catch (IOException e) { log("Arrêt UDP."); }
        }).start();


    }

    private void stopServer() {
        isRunning = false;
        editPort.setEnabled(true);
        // Mise à jour de l'UI immédiate (Redevient vert)
        btnToggle.setText("Démarrer Serveur");
        btnToggle.setBackgroundTintList(ColorStateList.valueOf(getThemeColor(R.attr.colorGreen)));

        new Thread(() -> {
            // 1. Notifier tous les clients et fermer leurs connexions
            broadcast("[SYSTÈME] Le serveur s'éteint. Déconnexion...");

            List<ClientHandler> copyClients = new ArrayList<>(clients);
            for (ClientHandler client : copyClients) {
                client.close();
            }
            clients.clear(); // Vide la liste des clients

            // 2. Fermer le socket principal du serveur
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                if (udpServerSocket != null) udpServerSocket.close();
            } catch (IOException e) { e.printStackTrace(); }

            log("Serveur arrêté proprement.");
        }).start();
    }

    private void log(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            String oldLogs = txtLogs.getText().toString();
            txtLogs.setText(msg + "\n" + oldLogs);

            // Force le ScrollView à remonter tout en haut pour voir le dernier message
            View parent = (View) txtLogs.getParent();
            if (parent instanceof ScrollView) {
                ((ScrollView) parent).fullScroll(View.FOCUS_UP);
            }
        });
    }

    private String getLocalIpAddress() {
        try {
            for (java.util.Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (java.util.Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return "127.0.0.1";
    }

    // Classe interne pour gérer chaque client (Multithreading)
    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientPseudo = "Inconnu";

        public ClientHandler(Socket s) {
            this.socket = s;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) { e.printStackTrace(); }
        }

        public void sendMessage(String msg) {
            new Thread(() -> {
                if (out != null) out.println(msg);
            }).start();
        }

        public void close() {
            try { socket.close(); } catch (IOException e) {}
        }

        @Override
        public void run() {    try {
            clientPseudo = in.readLine();
            log("Nouveau client connecté : " + clientPseudo + " (via TCP)");
            broadcast("[Système] " + clientPseudo + " a rejoint la discussion.");

            String input;
            while ((input = in.readLine()) != null) {
                try {
                    // On tente de lire le message comme un objet JSON
                    org.json.JSONObject json = new org.json.JSONObject(input);

                    // Extraction des données de la structure complexe
                    String pseudo = json.getString("pseudo");
                    String message = json.getString("message");
                    String proto = json.getString("proto");

                    // Affichage formaté dans les logs du serveur
                    String logMsg = "(" + proto + ") [" + pseudo + "] : " + message;
                    log(logMsg);

                    // On renvoie la structure JSON complète à tous les autres clients
                    broadcast(input);

                } catch (org.json.JSONException e) {
                    // Si ce n'est pas du JSON (texte brut), on traite normalement
                    String formattedMsg = clientPseudo + ": " + input;
                    log(formattedMsg);
                    broadcast(formattedMsg);
                }
            }
        } catch (IOException e) {
            // Le client est parti (déconnexion sauvage ou normale)
        } finally {
            // On nettoie proprement
            log(clientPseudo + " s'est déconnecté.");
            clients.remove(this);
            broadcast("[Système] " + clientPseudo + " a quitté la discussion.");
            close();
        }
        }
    }
}