package com.example.isla_beta.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.isla_beta.R;
import com.example.isla_beta.adapters.ChatAdapter;
import com.example.isla_beta.databinding.ActivityMainBinding;
import com.example.isla_beta.functions.DownloadMusicFunction;
import com.example.isla_beta.functions.PlayMusicFunction;
import com.example.isla_beta.functions.SearchImageFunction;
import com.example.isla_beta.models.ChatMessage;
import com.example.isla_beta.models.PromptMessage;
import com.example.isla_beta.utilities.Constants;
import com.example.isla_beta.utilities.PreferenceManager;
import com.example.isla_beta.utilities.SearchImage;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private String api_openai, api_prodia;
    private String encodedImage;
    List<PromptMessage> userConversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        String userConversationJson = preferenceManager.getString(Constants.KEY_USER_CONVERSATION);
        if(!TextUtils.isEmpty(userConversationJson)) {
            try {
                JSONArray jsonArray = new JSONArray(userConversationJson);
                userConversation = new ArrayList<>();
                for(int i=0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String role = jsonObject.getString("role");
                    String content = jsonObject.getString("content");
                    userConversation.add(new PromptMessage(role, content));
                }
                saveUserConversation();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            String aboutAI = readAboutAI();
            userConversation = new ArrayList<>();
            userConversation.add(new PromptMessage("system", aboutAI));
            saveUserConversation();
        }

        loadUserDetails();
        getToken();
        setListeners();
        init();
        listenMessages();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        if(encodedImage != null && binding.inputMessage.getText().toString().equals("")) {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, "Isla");
            message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_IMAGE);
            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
            message.put(Constants.KEY_ENCODED_IMAGE, encodedImage);
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            binding.inputMessage.setText(null);
            binding.imagePreview.setVisibility(View.GONE);
            encodedImage = null;
        } else if(encodedImage != null && !binding.inputMessage.getText().toString().equals("")){
            String question = binding.inputMessage.getText().toString();
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, "Isla");
            message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_IMAGE);
            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
            message.put(Constants.KEY_ENCODED_IMAGE, encodedImage);
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            binding.inputMessage.setText(null);
            binding.imagePreview.setVisibility(View.GONE);
            callIsla(question);
            encodedImage = null;
        }else if(!binding.inputMessage.getText().toString().equals("")) {
            String question = binding.inputMessage.getText().toString();
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, "Isla");
            message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_TEXT);
            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            binding.inputMessage.setText(null);
            callIsla(question);
        }

    }

    private void sendAnswer(String answer) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, "Isla");
        message.put(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_TEXT);
        message.put(Constants.KEY_MESSAGE, answer);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
    }

    private void sendImage(String encodedImage) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, "Isla");
        message.put(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_IMAGE);
        message.put(Constants.KEY_MESSAGE, "");
        message.put(Constants.KEY_ENCODED_IMAGE, encodedImage);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
    }

    private void sendVideo(String videoUrl) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, "Isla");
        message.put(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_VIDEO);
        message.put(Constants.KEY_MESSAGE, "");
        message.put(Constants.KEY_VIDEO_URL, videoUrl);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 1000;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private static Bitmap getBitmapFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
                            binding.imagePreview.setImageBitmap(bitmap);
                            binding.imagePreview.setVisibility(View.VISIBLE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
    );

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, "Isla")
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, "Isla")
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null) {
            return;
        }
        if(value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if(documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.messageType = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);
                    if (documentChange.getDocument().contains(Constants.KEY_ENCODED_IMAGE)) {
                        chatMessage.encodeImage = documentChange.getDocument().getString(Constants.KEY_ENCODED_IMAGE);
                    }
                    if (documentChange.getDocument().contains(Constants.KEY_VIDEO_URL)) {
                        chatMessage.videoUrl = documentChange.getDocument().getString(Constants.KEY_VIDEO_URL);
                    }
                    chatMessages.add(chatMessage);
                }
            }
            chatMessages.sort(Comparator.comparing(obj -> obj.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    };

    private void setListeners() {
        binding.imageProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.layoutSendImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);

        api_openai = preferenceManager.getString(Constants.KEY_OPENAI);
        api_prodia = preferenceManager.getString(Constants.KEY_PRODIA);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private String readAboutAI() {
        Resources resources = getResources();
        InputStream inputStream = resources.openRawResource(R.raw.about);
        StringBuilder aboutAI = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = br.readLine()) != null) {
                aboutAI.append(line);
                aboutAI.append("\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aboutAI.toString();
    }

    private void saveUserConversation() {
        JSONArray jsonArray = new JSONArray();
        for(PromptMessage message : userConversation) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("role", message.getRole());
                jsonObject.put("content", message.getContent());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        String json = jsonArray.toString();
        preferenceManager.putString(Constants.KEY_USER_CONVERSATION, json);
    }

    void checkAndRemoveMessages() {
        int totalCharacters = 0;
        for (PromptMessage message : userConversation) {
            totalCharacters += message.getContent().length();
        }
        if (totalCharacters > 4000 && userConversation.size() > 1) {
            userConversation.remove(1);
            saveUserConversation();
        }
    }

    private void callIsla(String question) {
        //okhttp
        JSONObject jsonBody;
        try {
            jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-3.5-turbo-0613");
            JSONArray messagesArray = new JSONArray();
            for (PromptMessage message : userConversation) {
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", message.getRole());
                userMessage.put("content", message.getContent());
                messagesArray.put(userMessage);
            }
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messagesArray.put(userMessage);
            jsonBody.put("messages", messagesArray);

            JSONObject playMusicFunction = PlayMusicFunction.getFunctionDefinition();
            JSONObject downloadMusicFunction = DownloadMusicFunction.getFunctionDefinition();
            JSONObject searchImageFunction = SearchImageFunction.getFunctionDefinition();
            JSONArray functionsArray = new JSONArray();
            functionsArray.put(playMusicFunction);
            functionsArray.put(downloadMusicFunction);
            functionsArray.put(searchImageFunction);
            jsonBody.put("functions", functionsArray);

            jsonBody.put("function_call", "auto");
            jsonBody.put("max_tokens", 300);
            jsonBody.put("top_p", 1);
            jsonBody.put("presence_penalty", 1);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + api_openai)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    JSONObject jsonObject;
                    try {
                        assert response.body() != null;
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        JSONObject responseMessage = jsonArray.getJSONObject(0).getJSONObject("message");
                        String responseContent = responseMessage.getString("content");

                        if(responseMessage.has("function_call")) {
                            JSONObject functionCall = responseMessage.getJSONObject("function_call");

                            String functionName = functionCall.getString("name");
                            String functionArguments = functionCall.getString("arguments");
                            JSONObject argumentsJson = new JSONObject(functionArguments);

                            if(functionName.equals("search_image")) {
                                try {
                                    String title = argumentsJson.getString("title");
                                    String imageUrl = SearchImage.searchImage(title);
                                    if(imageUrl != null) {
                                        Bitmap bitmap = getBitmapFromUrl(imageUrl);
                                        assert bitmap != null;
                                        String encodedImage = encodeImage(bitmap);
                                        sendImage(encodedImage);
                                    }
                                } catch(Exception e) {
                                    sendAnswer("Gagal mengirim gambar");
                                }

                            }

                        } else {
                            sendAnswer(responseContent.trim());
                            userConversation.add(new PromptMessage("assistant", responseContent.trim()));
                            saveUserConversation();
                            checkAndRemoveMessages();
                        }

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    if (response.code() == 401) {
                        sendAnswer("Terjadi kesalahan karena API yang dimasukkan salah");
                    } else {
                        assert response.body() != null;
                        sendAnswer("Terjadi kesalahan karena " + response.body().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                sendAnswer("Terjadi kesalahan karena " + e.getMessage());
            }

        });
    }
}