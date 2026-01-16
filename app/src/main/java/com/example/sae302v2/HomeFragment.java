package com.example.sae302v2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // On gonfle le layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Affichage de la date
        TextView textDate = view.findViewById(R.id.text_date);
        String currentDate = new SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE).format(new Date());
        textDate.setText("Nous sommes le " + currentDate);

        // Gestion du lien GitHub
        Button btnGithub = view.findViewById(R.id.btn_github);
        btnGithub.setOnClickListener(v -> {
            String url = "https://github.com/Tomthefarmer/SAE3.02-v2";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        return view;
    }
}