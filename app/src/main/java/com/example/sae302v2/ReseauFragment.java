package com.example.sae302v2;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class ReseauFragment extends Fragment {

    public ReseauFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reseau, container, false);

        Button btnClient = view.findViewById(R.id.btn_vers_client);
        Button btnServeur = view.findViewById(R.id.btn_vers_serveur);

        btnClient.setOnClickListener(v -> replaceFragment(new ClientFragment()));
        btnServeur.setOnClickListener(v -> replaceFragment(new ServeurFragment()));

        return view;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); // Assurez-vous que l'ID correspond à votre container dans activity_main.xml
        transaction.addToBackStack(null);
        transaction.commit();
    }
}