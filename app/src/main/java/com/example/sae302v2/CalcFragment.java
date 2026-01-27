package com.example.sae302v2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class CalcFragment extends Fragment implements View.OnClickListener {

    private TextView tvDisplay;
    private String currentInput = "";
    private boolean isResult = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calc, container, false);

        tvDisplay = view.findViewById(R.id.tv_display);

        // Liste des IDs des boutons
        int[] buttonIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_dot, R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div,
                R.id.btn_clear, R.id.btn_del, R.id.btn_equal
        };

        for (int id : buttonIds) {
            view.findViewById(id).setOnClickListener(this);
        }

        return view;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_clear) {
            currentInput = "";
            isResult = false;
        }
        else if (id == R.id.btn_del) {
            // AMÉLIORATION : Si c'est un résultat final, DEL efface tout
            // Sinon, on efface juste le dernier caractère
            if (isResult || currentInput.length() <= 1) {
                currentInput = "";
            } else {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
            }
            isResult = false;
        }
        else if (id == R.id.btn_equal) {
            calculateResult();
            return;
        }
        else {
            MaterialButton btn = (MaterialButton) v;
            String btnText = btn.getText().toString();

            // 1. EMPÊCHER DE COMMENCER PAR UN OPÉRATEUR (sauf si c'est un chiffre)
            if (currentInput.isEmpty() && !isNumber(btnText)) {
                return; // On ne fait rien si l'utilisateur commence par +, -, *, / ou .
            }

            // 2. GESTION DU POINT (Empêcher les points multiples dans un même nombre)
            if (btnText.equals(".")) {
                if (canAddDot()) {
                    currentInput += ".";
                }
            }
            // 3. GESTION DU RESET APRÈS RÉSULTAT
            else if (isResult && isNumber(btnText)) {
                currentInput = btnText; // Nouveau chiffre remplace le résultat
                isResult = false;
            }
            // 4. EMPÊCHER LES OPÉRATEURS DOUBLES (ex: "++" ou "+*")
            else if (!isNumber(btnText) && !isNumber(String.valueOf(currentInput.charAt(currentInput.length()-1)))) {
                // Si on clique sur un opérateur alors que le dernier caractère est déjà un opérateur,
                // on remplace l'ancien par le nouveau
                currentInput = currentInput.substring(0, currentInput.length() - 1) + btnText;
            }
            else {
                currentInput += btnText;
                isResult = false;
            }
        }
        updateDisplay();
    }

    // Méthode pour vérifier si le dernier nombre saisi contient déjà un point
    private boolean canAddDot() {
        if (currentInput.isEmpty()) return true;

        // On découpe la chaîne par les opérateurs pour isoler le dernier nombre
        String[] parts = currentInput.split("[+\\-*/]");
        if (parts.length == 0) return true;

        String lastNumber = parts[parts.length - 1];
        return !lastNumber.contains(".");
    }

    private boolean isNumber(String str) {
        return str.matches("[0-9]");
    }

    private void updateDisplay() {
        tvDisplay.setText(currentInput.isEmpty() ? "0" : currentInput);
    }

    private void calculateResult() {
        if (currentInput.isEmpty()) return;

        try {// Nettoyage : si l'utilisateur finit par un opérateur (ex: "5+"), on le retire
            if (currentInput.endsWith("+") || currentInput.endsWith("-") ||
                    currentInput.endsWith("*") || currentInput.endsWith("/")) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
            }

            Expression e = new ExpressionBuilder(currentInput).build();
            double result = e.evaluate();

            if (result == (long) result) {
                currentInput = String.valueOf((long) result);
            } else {
                // Optionnel : Arrondir à 6 décimales pour éviter les 0.000000000004
                currentInput = String.valueOf(Math.round(result * 1000000.0) / 1000000.0);
            }

            isResult = true;

        } catch (Exception e) {
            currentInput = "";
            tvDisplay.setText("Erreur");
            isResult = false;
            return;
        }
        updateDisplay();
    }
}