package com.example.isla_beta.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.isla_beta.databinding.ActivityAccountBinding;
import com.example.isla_beta.utilities.Constants;
import com.example.isla_beta.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AccountActivity extends AppCompatActivity {
    private ActivityAccountBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    AtomicBoolean isPasswordVisible;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        encodedImage = preferenceManager.getString(Constants.KEY_IMAGE);

        isPasswordVisible =  new AtomicBoolean(false);

        fetchCurrentUserDetails();
        setListener();
    }

    private void setListener() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        binding.toggleViewPassword.setOnClickListener(v -> togglePassword());

        binding.saveButton.setOnClickListener(v -> saveAccount());
    }

    private void fetchCurrentUserDetails() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String image = documentSnapshot.getString(Constants.KEY_IMAGE);
                        String name = documentSnapshot.getString(Constants.KEY_NAME);
                        String email = documentSnapshot.getString(Constants.KEY_EMAIL);
                        String password = documentSnapshot.getString(Constants.KEY_PASSWORD);

                        byte[] bytes = Base64.decode(image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.imageProfile.setImageBitmap(bitmap);
                        binding.inputName.setText(name);
                        binding.inputEmail.setText(email);
                        binding.inputPassword.setText(password);
                    }
                })
                .addOnFailureListener(e -> showToast(e.getMessage()));
    }

    private void saveAccount() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        DocumentReference userRef = database.collection(Constants.KEY_COLLECTION_USERS).document(userId);

        HashMap<String, Object> updatedData = new HashMap<>();
        updatedData.put(Constants.KEY_IMAGE, encodedImage);
        updatedData.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        updatedData.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        updatedData.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());

        userRef.update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent i = new Intent(getApplicationContext(), AccountActivity.class);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> showToast("Gagal menyimpan perubahan data akun"));
    }



    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData()  != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
    );

    private void togglePassword() {
        isPasswordVisible.set(!isPasswordVisible.get());
        if (isPasswordVisible.get()) {
            binding.inputPassword.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            binding.inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.saveButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.saveButton.setVisibility(View.VISIBLE);
        }
    }
}