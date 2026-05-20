package imui.jiguang.cn.imuisample.messages;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chatai.aiinteract.AiConfig;
import com.chatai.aiinteract.AiInteract;
import com.chatai.aiinteract.ApiPreset;

import java.util.List;

import imui.jiguang.cn.imuisample.R;

public class ApiProviderActivity extends AppCompatActivity {

    private LinearLayout providerContainer;
    private EditText etApiKey;
    private EditText etEndpoint;
    private EditText etCustomModel;
    private EditText etAiName;
    private Spinner spinnerModel;
    private Switch switchVoice;
    private Switch switchVideo;
    private Button btnGetApiKey;
    private Button btnSave;
    private ImageButton btnBack;
    private LinearLayout customFields;

    private ApiPreset selectedPreset = ApiPreset.OPENAI;
    private List<ApiPreset> presets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_provider);

        providerContainer = findViewById(R.id.provider_container);
        etApiKey = findViewById(R.id.et_api_key);
        etEndpoint = findViewById(R.id.et_endpoint);
        etCustomModel = findViewById(R.id.et_custom_model);
        etAiName = findViewById(R.id.et_ai_name);
        spinnerModel = findViewById(R.id.spinner_model);
        switchVoice = findViewById(R.id.switch_voice);
        switchVideo = findViewById(R.id.switch_video);
        btnGetApiKey = findViewById(R.id.btn_get_api_key);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);
        customFields = findViewById(R.id.custom_fields);

        presets = AiInteract.getAvailablePresets();

        btnBack.setOnClickListener(v -> finish());

        btnGetApiKey.setOnClickListener(v -> {
            String url = selectedPreset.getApiKeyUrl();
            if (!TextUtils.isEmpty(url)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } else {
                Toast.makeText(this, "No API key URL for this provider", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> saveConfig());

        buildProviderCards();
        selectPreset(selectedPreset);
    }

    private void buildProviderCards() {
        providerContainer.removeAllViews();
        for (ApiPreset preset : presets) {
            View card = getLayoutInflater().inflate(R.layout.item_provider_card, providerContainer, false);

            TextView tvName = card.findViewById(R.id.tv_provider_name);
            TextView tvDesc = card.findViewById(R.id.tv_provider_desc);
            TextView tvModels = card.findViewById(R.id.tv_provider_models);
            ImageView ivCheck = card.findViewById(R.id.iv_check);

            tvName.setText(preset.getDisplayName());
            tvDesc.setText(preset.getDescription());

            List<String> models = preset.getModels();
            if (models != null && !models.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < models.size(); i++) {
                    if (i > 0) sb.append(" · ");
                    sb.append(models.get(i));
                }
                tvModels.setText(sb.toString());
            } else {
                tvModels.setVisibility(View.GONE);
            }

            card.setOnClickListener(v -> selectPreset(preset));
            card.setTag(preset);

            providerContainer.addView(card);
        }
    }

    private void selectPreset(ApiPreset preset) {
        selectedPreset = preset;

        // Update check icons
        for (int i = 0; i < providerContainer.getChildCount(); i++) {
            View card = providerContainer.getChildAt(i);
            ImageView ivCheck = card.findViewById(R.id.iv_check);
            ApiPreset cardPreset = (ApiPreset) card.getTag();
            if (cardPreset == preset) {
                ivCheck.setImageResource(android.R.drawable.radiobutton_on_background);
            } else {
                ivCheck.setImageResource(android.R.drawable.radiobutton_off_background);
            }
        }

        // Show/hide custom fields
        if (preset == ApiPreset.CUSTOM) {
            customFields.setVisibility(View.VISIBLE);
        } else {
            customFields.setVisibility(View.GONE);
        }

        // Update model spinner
        List<String> models = preset.getModels();
        if (models != null && !models.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, models);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerModel.setAdapter(adapter);
            spinnerModel.setSelection(0);
            spinnerModel.setEnabled(true);
        } else {
            // CUSTOM: no models, user types freely
            spinnerModel.setEnabled(false);
        }

        // Show API key URL button
        if (!TextUtils.isEmpty(preset.getApiKeyUrl())) {
            btnGetApiKey.setVisibility(View.VISIBLE);
        } else {
            btnGetApiKey.setVisibility(View.GONE);
        }

        // Pre-fill endpoint for CUSTOM
        if (preset == ApiPreset.CUSTOM) {
            etEndpoint.setText("");
            etCustomModel.setText("");
        }
    }

    private void saveConfig() {
        String apiKey = etApiKey.getText().toString().trim();
        if (TextUtils.isEmpty(apiKey)) {
            etApiKey.setError("API Key is required");
            return;
        }

        String aiName = etAiName.getText().toString().trim();
        if (TextUtils.isEmpty(aiName)) {
            aiName = "AI Assistant";
        }

        AiConfig config;
        if (selectedPreset == ApiPreset.CUSTOM) {
            String endpoint = etEndpoint.getText().toString().trim();
            String model = etCustomModel.getText().toString().trim();

            if (TextUtils.isEmpty(endpoint)) {
                etEndpoint.setError("Endpoint is required");
                return;
            }
            if (TextUtils.isEmpty(model)) {
                model = "gpt-4";
            }

            config = new AiConfig.Builder(endpoint, apiKey)
                    .model(model)
                    .aiUserName(aiName)
                    .voiceEnabled(switchVoice.isChecked())
                    .videoEnabled(switchVideo.isChecked())
                    .preset(ApiPreset.CUSTOM)
                    .build();
        } else {
            String model = (String) spinnerModel.getSelectedItem();
            if (model == null) model = selectedPreset.getDefaultModel();

            config = AiConfig.fromPreset(selectedPreset, apiKey)
                    .model(model)
                    .aiUserName(aiName)
                    .voiceEnabled(switchVoice.isChecked())
                    .videoEnabled(switchVideo.isChecked())
                    .build();
        }

        // Initialize or switch
        AiInteract.getInstance().updateConfig(config);

        Toast.makeText(this,
                "Connected to " + selectedPreset.getDisplayName() + " (" + config.getModel() + ")",
                Toast.LENGTH_SHORT).show();

        finish();
    }
}
