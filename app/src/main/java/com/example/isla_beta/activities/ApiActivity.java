package com.example.isla_beta.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import com.example.isla_beta.databinding.ActivityApiBinding;
import com.example.isla_beta.utilities.Constants;
import com.example.isla_beta.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicBoolean;

public class ApiActivity extends AppCompatActivity {

    private ActivityApiBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    AtomicBoolean isOpenAIVisible;
    AtomicBoolean isProdiaVisible;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
        binding = ActivityApiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.editTextOpenAI.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.editTextProdia.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        binding.editTextOpenAI.setText(preferenceManager.getString(Constants.KEY_OPENAI));
        binding.editTextProdia.setText(preferenceManager.getString(Constants.KEY_PRODIA));

        init();
        setListeners();
    }

    private void init() {
        isOpenAIVisible = new AtomicBoolean(false);
        isProdiaVisible = new AtomicBoolean(false);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        });
        binding.saveButton.setOnClickListener(v -> save());
        binding.toggleViewOpenAI.setOnClickListener(v -> toggleOpenAI());
        binding.toggleViewProdia.setOnClickListener(v -> toggleProdia());
    }

    private void toggleOpenAI() {
        isOpenAIVisible.set(!isOpenAIVisible.get());
        if (isOpenAIVisible.get()) {
            binding.editTextOpenAI.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            binding.editTextOpenAI.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private void toggleProdia() {
        isProdiaVisible.set(!isProdiaVisible.get());
        if (isProdiaVisible.get()) {
            binding.editTextProdia.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            binding.editTextProdia.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private void save() {
        if(!binding.editTextOpenAI.getText().toString().isEmpty()) {
            preferenceManager.putString(Constants.KEY_OPENAI, binding.editTextOpenAI.getText().toString());
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_USERS).document(
                            preferenceManager.getString(Constants.KEY_USER_ID)
                    );
            documentReference.update(Constants.KEY_OPENAI, binding.editTextOpenAI.getText().toString());
        } else {
            preferenceManager.putString(Constants.KEY_OPENAI, null);
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_USERS).document(
                            preferenceManager.getString(Constants.KEY_USER_ID)
                    );
            documentReference.update(Constants.KEY_OPENAI, null);
        }

        if(!binding.editTextProdia.getText().toString().isEmpty()) {
            preferenceManager.putString(Constants.KEY_PRODIA, binding.editTextProdia.getText().toString());
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_USERS).document(
                            preferenceManager.getString(Constants.KEY_USER_ID)
                    );
            documentReference.update(Constants.KEY_PRODIA, binding.editTextProdia.getText().toString());
        } else {
            preferenceManager.putString(Constants.KEY_PRODIA, null);
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_USERS).document(
                            preferenceManager.getString(Constants.KEY_USER_ID)
                    );
            documentReference.update(Constants.KEY_PRODIA, null);
        }
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(intent);
    }
}