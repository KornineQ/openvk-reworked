package uk.openvk.android.refresh.ui.core.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.kieronquinn.monetcompat.app.MonetCompatActivity;
import com.kieronquinn.monetcompat.core.MonetCompat;

import java.util.Locale;
import java.util.Objects;

import dev.kdrag0n.monet.theme.ColorScheme;
import uk.openvk.android.refresh.Global;
import uk.openvk.android.refresh.OvkApplication;
import uk.openvk.android.refresh.R;
import uk.openvk.android.refresh.api.Account;
import uk.openvk.android.refresh.api.Likes;
import uk.openvk.android.refresh.api.Users;
import uk.openvk.android.refresh.api.Wall;
import uk.openvk.android.refresh.api.enumerations.HandlerMessages;
import uk.openvk.android.refresh.api.models.User;
import uk.openvk.android.refresh.api.models.WallPost;
import uk.openvk.android.refresh.api.wrappers.DownloadManager;
import uk.openvk.android.refresh.api.wrappers.OvkAPIWrapper;
import uk.openvk.android.refresh.ui.core.fragments.app.AboutApplicationFragment;
import uk.openvk.android.refresh.ui.core.fragments.app.MainSettingsFragment;
import uk.openvk.android.refresh.ui.core.fragments.app.PersonalizationFragment;
import uk.openvk.android.refresh.ui.core.fragments.app.ProfileFragment;
import uk.openvk.android.refresh.ui.wrappers.LocaleContextWrapper;

public class MainSettingsActivity extends MonetCompatActivity {
    private SharedPreferences global_prefs;
    private SharedPreferences instance_prefs;
    public Handler handler;
    private OvkAPIWrapper ovk_api;
    private DownloadManager downloadManager;
    private Wall wall;
    private Account account;
    private Likes likes;
    private FragmentTransaction ft;
    private String args;
    private MaterialToolbar toolbar;
    private Users users;
    private User user;
    private boolean isDarkTheme;
    private MainSettingsFragment mainSettingsFragment;
    private PersonalizationFragment personalizationFragment;
    private AboutApplicationFragment aboutAppFragment;
    private Fragment selectedFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        global_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isDarkTheme = global_prefs.getBoolean("dark_theme", false);
        Global.setColorTheme(this, global_prefs.getString("theme_color", "blue"), getWindow());
        Global.setInterfaceFont(this);
        instance_prefs = getSharedPreferences("instance", 0);
        setContentView(R.layout.intent_view);
        setMonetTheme();
        setAppBar();
        createFragments();
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
                toolbar.setBackgroundColor(Objects.requireNonNull(getMonet().getMonetColors().getAccent1().get(600)).toLinearSrgb().toSrgb().quantize8());
                getWindow().setStatusBarColor(Objects.requireNonNull(getMonet().getMonetColors().getAccent1().get(700)).toLinearSrgb().toSrgb().quantize8());
            }
        }
    }

    private void createFragments() {
        mainSettingsFragment = new MainSettingsFragment();
        personalizationFragment = new PersonalizationFragment();
        aboutAppFragment = new AboutApplicationFragment();
        setAppBar();
        FragmentManager fm = getSupportFragmentManager();
        ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_screen, mainSettingsFragment, "settings");
        ft.add(R.id.fragment_screen, personalizationFragment, "personalization");
        ft.add(R.id.fragment_screen, aboutAppFragment, "about_app");
        ft.commit();
        ft = getSupportFragmentManager().beginTransaction();
        ft.hide(personalizationFragment);
        ft.hide(aboutAppFragment);
        ft.commit();
        ft = getSupportFragmentManager().beginTransaction();
        selectedFragment = getSupportFragmentManager().findFragmentByTag("settings");
    }

    private void setAppBar() {
        toolbar = findViewById(R.id.app_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setTitle(getResources().getString(R.string.nav_settings));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        if(!Global.checkMonet(this)) {
            TypedValue typedValue = new TypedValue();
            boolean isDarkThemeEnabled = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (isDarkThemeEnabled) {
                getTheme().resolveAttribute(androidx.appcompat.R.attr.background, typedValue, true);
            } else {
                getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimaryDark, typedValue, true);
            }
            getWindow().setStatusBarColor(typedValue.data);
        }
    }

    @Override
    public void onMonetColorsChanged(@NonNull MonetCompat monet, @NonNull ColorScheme monetColors, boolean isInitialChange) {
        super.onMonetColorsChanged(monet, monetColors, isInitialChange);
        getMonet().updateMonetColors();
        setMonetTheme();
    }

    public void switchFragment(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        ft = getSupportFragmentManager().beginTransaction();
        ft.hide(Objects.requireNonNull(fm.findFragmentByTag("settings")));
        ft.hide(Objects.requireNonNull(fm.findFragmentByTag("personalization")));
        ft.hide(Objects.requireNonNull(fm.findFragmentByTag("about_app")));
        if(selectedFragment == null) selectedFragment = getSupportFragmentManager().findFragmentByTag("settings");
        switch (tag) {
            case "settings":
                ft.hide(selectedFragment);
                selectedFragment = Objects.requireNonNull(fm.findFragmentByTag("settings"));
                ft.show(selectedFragment);
                ((MaterialToolbar) findViewById(R.id.app_toolbar)).setTitle(R.string.nav_settings);
                ((MaterialToolbar) findViewById(R.id.app_toolbar)).setNavigationIcon(R.drawable.ic_arrow_back);
                break;
            case "personalization":
                ft.hide(selectedFragment);
                selectedFragment = Objects.requireNonNull(fm.findFragmentByTag("personalization"));
                ft.show(selectedFragment);
                ((MaterialToolbar) findViewById(R.id.app_toolbar)).setTitle(R.string.pref_personalization);
                ((MaterialToolbar) findViewById(R.id.app_toolbar)).setNavigationIcon(R.drawable.ic_arrow_back);
                break;
            case "about_app":
                ft.hide(selectedFragment);
                selectedFragment = Objects.requireNonNull(fm.findFragmentByTag("about_app"));
                ft.show(selectedFragment);
                ((MaterialToolbar) findViewById(R.id.app_toolbar)).setTitle(R.string.pref_about_app);
                ((MaterialToolbar) findViewById(R.id.app_toolbar)).setNavigationIcon(R.drawable.ic_arrow_back);
                break;
        }
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        if(selectedFragment != null && selectedFragment != mainSettingsFragment) {
            switchFragment("settings");
        } else {
            super.onBackPressed();
        }
    }
}
