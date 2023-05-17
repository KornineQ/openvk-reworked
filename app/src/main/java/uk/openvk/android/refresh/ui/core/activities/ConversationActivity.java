package uk.openvk.android.refresh.ui.core.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.kieronquinn.monetcompat.app.MonetCompatActivity;
import com.kieronquinn.monetcompat.core.MonetCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import dev.kdrag0n.monet.theme.ColorScheme;
import uk.openvk.android.refresh.Global;
import uk.openvk.android.refresh.OvkApplication;
import uk.openvk.android.refresh.R;
import uk.openvk.android.refresh.api.enumerations.HandlerMessages;
import uk.openvk.android.refresh.api.models.Conversation;
import uk.openvk.android.refresh.api.wrappers.DownloadManager;
import uk.openvk.android.refresh.api.wrappers.OvkAPIWrapper;
import uk.openvk.android.refresh.ui.view.layouts.SendTextBottomPanel;
import uk.openvk.android.refresh.ui.list.adapters.MessagesAdapter;
import uk.openvk.android.refresh.ui.wrappers.LocaleContextWrapper;

public class ConversationActivity extends MonetCompatActivity {
    public OvkAPIWrapper ovk_api;
    public Conversation conversation;
    public Handler handler;
    public long peer_id;
    public String conv_title;
    public int peer_online;
    private ArrayList<uk.openvk.android.refresh.api.models.Message> history;
    private MessagesAdapter conversation_adapter;
    private RecyclerView messagesView;
    private LinearLayoutManager llm;
    private DownloadManager downloadManager;
    private SharedPreferences instance_prefs;
    private SendTextBottomPanel bottomPanel;
    private uk.openvk.android.refresh.api.models.Message last_sended_message;
    private SharedPreferences global_prefs;
    private boolean isDarkTheme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        global_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Global.setColorTheme(this, global_prefs.getString("theme_color", "blue"),
                getWindow());
        Global.setInterfaceFont(this);
        isDarkTheme = global_prefs.getBoolean("dark_theme", false);
        instance_prefs = getSharedPreferences("instance", 0);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                finish();
            } else {
                peer_id = extras.getLong("peer_id");
                conv_title = extras.getString("conv_title");
                peer_online = extras.getInt("online");
            }
        } else {
            peer_id = savedInstanceState.getInt("peer_id");
            conv_title = (String) savedInstanceState.getSerializable("conv_title");
            peer_online = savedInstanceState.getInt("online");
        }
        setContentView(R.layout.activity_conversation);
        setMonetTheme();
        history = new ArrayList<uk.openvk.android.refresh.api.models.Message>();
        setAPIWrapper();
        setAppBar();
        setBottomPanel();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale languageType = OvkApplication.getLocale(newBase);
        super.attachBaseContext(LocaleContextWrapper.wrap(newBase, languageType));
    }

    private void setMonetTheme() {
        if(Global.checkMonet(this)) {
            MaterialToolbar toolbar = findViewById(R.id.app_toolbar);
            if (!isDarkTheme) {
                toolbar.setBackgroundColor(
                        Global.getMonetIntColor(getMonet(), "accent", 600));
                getWindow().setStatusBarColor(Global.getMonetIntColor(getMonet(), "accent",
                        700));
            }
            int[] colors;
            int colorOnSurface = MaterialColors.getColor(this,
                    com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
            if(isDarkTheme) {
                colors = new int[]{
                        Objects.requireNonNull(getMonet()
                                .getMonetColors().getAccent1().get(200))
                                .toLinearSrgb().toSrgb().quantize8(),
                        Global.adjustAlpha(colorOnSurface, 0.6f)
                };
                Objects.requireNonNull(((TextInputEditText) findViewById(R.id.sendTextBottomPanel).
                                findViewById(R.id.send_text)))
                        .setHighlightColor(
                                Global.getMonetIntColor(getMonet(), "accent", 500));
            } else {
                colors = new int[]{
                        Global.getMonetIntColor(getMonet(), "accent", 500),
                        Global.adjustAlpha(colorOnSurface, 0.6f)
                };
                Objects.requireNonNull(((TextInputEditText) findViewById(R.id.sendTextBottomPanel)
                                .findViewById(R.id.send_text)))
                        .setHighlightColor(
                                Global.getMonetIntColor(getMonet(), "accent", 200));
            }
        }
    }

    private void setBottomPanel() {
        bottomPanel = (SendTextBottomPanel) findViewById(R.id.sendTextBottomPanel);
        bottomPanel.setOnSendButtonClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                if(bottomPanel.getText().length() > 0) {
                    try {
                        last_sended_message = new uk.openvk.android.refresh.api.models
                                .Message(1, 0, false, false,
                                (int)(System.currentTimeMillis() / 1000), bottomPanel.getText(),
                                ConversationActivity.this);
                        last_sended_message.sending = true;
                        last_sended_message.isError = false;
                        conversation.sendMessage(ovk_api, bottomPanel.getText());
                        if(history == null) {
                            history = new ArrayList<>();
                        }
                        history.add(last_sended_message);
                        if(conversation_adapter == null) {
                            conversation_adapter = new MessagesAdapter(
                                    ConversationActivity.this, history, peer_id);
                            messagesView.setAdapter(conversation_adapter);
                        } else {
                            conversation_adapter.notifyDataSetChanged();
                        }
                        bottomPanel.clearText();
                        messagesView.smoothScrollToPosition(history.size() -1);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        ((TextInputEditText) bottomPanel.findViewById(R.id.send_text)).addTextChangedListener(
                new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                AppCompatImageButton send_btn = bottomPanel.findViewById(R.id.send_btn);
                if(bottomPanel.getText().length() > 0) {
                    send_btn.setEnabled(true);
                } else {
                    send_btn.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(((TextInputEditText) bottomPanel.findViewById(R.id.send_text)).getLineCount() > 4) {
                    ((TextInputEditText) bottomPanel.findViewById(R.id.send_text)).setLines(4);
                } else {
                    ((TextInputEditText) bottomPanel.findViewById(R.id.send_text)).setLines(
                            ((TextInputEditText) bottomPanel.findViewById(R.id.send_text))
                                    .getLineCount());
                }
            }
        });
    }

    private void setAppBar() {
        MaterialToolbar toolbar = findViewById(R.id.app_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setTitle(conv_title);
        if(peer_online == 1) {
            toolbar.setSubtitle(R.string.online);
        } else {
            toolbar.setSubtitle(R.string.offline);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        if(!Global.checkMonet(this)) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            TypedValue typedValue = new TypedValue();
            boolean isDarkThemeEnabled = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (isDarkThemeEnabled) {
                getTheme().resolveAttribute(androidx.appcompat.R.attr.background, typedValue,
                        true);
            } else {
                getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimaryDark, typedValue,
                        true);
            }
            window.setStatusBarColor(typedValue.data);
        }
    }

    public void setAPIWrapper() {
        ovk_api = new OvkAPIWrapper(this);
        downloadManager = new DownloadManager(this);
        ovk_api.setServer(instance_prefs.getString("server", ""));
        ovk_api.setAccessToken(instance_prefs.getString("access_token", ""));
        conversation = new Conversation();
        handler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d("OpenVK", String.format("Handling API message: %s", msg.what));
                receiveState(msg.what, msg.getData());
            }
        };
        conversation.getHistory(ovk_api, peer_id);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void receiveState(int message, Bundle data) {
        if (message == HandlerMessages.MESSAGES_GET_HISTORY) {
            messagesView = findViewById(R.id.messages_view);
            history = conversation.parseHistory(this, data.getString("response"));
            conversation_adapter = new MessagesAdapter(this, history, peer_id);
            llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            llm.setStackFromEnd(true);
            messagesView.setLayoutManager(llm);
            messagesView.setAdapter(conversation_adapter);
        } else if (message == HandlerMessages.CHAT_DISABLED) {
            last_sended_message.sending = false;
            last_sended_message.isError = true;
            history.set(history.size() - 1, last_sended_message);
            conversation_adapter.notifyDataSetChanged();
        } else if(message == HandlerMessages.MESSAGES_SEND) {
            last_sended_message.sending = false;
            last_sended_message.getSendedId(data.getString("response"));
            history.set(history.size() - 1, last_sended_message);
            conversation_adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void recreate() {

    }

    @Override
    public void onMonetColorsChanged(@NonNull MonetCompat monet, @NonNull ColorScheme monetColors,
                                     boolean isInitialChange) {
        super.onMonetColorsChanged(monet, monetColors, isInitialChange);
        getMonet().updateMonetColors();
        setMonetTheme();
    }
}
