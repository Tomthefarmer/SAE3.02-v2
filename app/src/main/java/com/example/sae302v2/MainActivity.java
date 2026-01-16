package com.example.sae302v2;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment; // Ajouté

import com.google.android.material.bottomnavigation.BottomNavigationView; // Ajouté

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Gestion des marges pour l'affichage plein écran (EdgeToEdge)
        // On l'applique sur le conteneur principal (id: main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- DEBUT DE LA LOGIQUE DE NAVIGATION ---

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Permet de voir tes PNG avec leurs vraies couleurs
        bottomNav.setItemIconTintList(null);

        // Charger l'accueil par défaut au premier lancement
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Ecouteur de clics sur la barre de navigation
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_calc) {
                selectedFragment = new CalcFragment();
            } else if (itemId == R.id.nav_network) {
                selectedFragment = new NetworkFragment();
            } else if (itemId == R.id.nav_carte) {
                selectedFragment = new CarteFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // --- FIN DE LA LOGIQUE DE NAVIGATION ---
    }
}