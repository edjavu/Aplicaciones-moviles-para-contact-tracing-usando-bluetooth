package com.espol.proyecto.asi_simulation.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

//import com.espol.proyecto.asi_simulation.BuildConfig;
import com.espol.proyecto.asi_simulation.R;
import com.espol.proyecto.asi_simulation.contacts.ContactsFragment;
import com.espol.proyecto.asi_simulation.debug.DebugFragment;
import com.espol.proyecto.asi_simulation.main.model.NotificationState;
import com.espol.proyecto.asi_simulation.main.model.NotificationStateError;
import com.espol.proyecto.asi_simulation.main.views.HeaderView;
import com.espol.proyecto.asi_simulation.reports.ReportsFragment;
import com.espol.proyecto.asi_simulation.storage.SecureStorage;
import com.espol.proyecto.asi_simulation.util.NotificationStateHelper;
import com.espol.proyecto.asi_simulation.util.NotificationUtil;
import com.espol.proyecto.asi_simulation.util.NotificatonErrorStateHelper;
import com.espol.proyecto.asi_simulation.util.TracingErrorStateHelper;
import com.espol.proyecto.asi_simulation.viewmodel.TracingViewModel;
import com.espol.proyecto.asi_simulation.whattodo.WtdPositiveTestFragment;
import com.espol.proyecto.asi_simulation.whattodo.WtdSymptomsFragment;
import com.espol.proyecto.asi_simulation.util.OtpGenerator;
import org.dpppt.android.sdk.TracingStatus;


import com.espol.proyecto.asi_simulation.triage.TriageActivity;

import java.security.SecureRandom;

import static android.view.View.VISIBLE;

public class HomeFragment extends Fragment {

    private TracingViewModel tracingViewModel;
    private HeaderView headerView;
    private ScrollView scrollView;

    private View infobox;
    private View tracingCard;
    private View cardNotifications;
    private View reportStatusBubble;
    private View reportStatusView;
    private View reportErrorView;
    private View cardTestFrame;
    private View cardTest;
    private View loadingView;

    //Opción "Monitor de Síntomas"
    private View cardTriageAccessFrame;
    private View cardTriageAccess;

    //Opción "Qué hacer si tienes Síntomas de la enfermedad"
    private View cardSymptomsFrame;
    private View cardSymptoms;

    private SecureStorage secureStorage;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    public static HomeFragment newInstance() {
        Bundle args = new Bundle();
        HomeFragment fragment = new HomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        secureStorage = SecureStorage.getInstance(getContext());

        tracingViewModel = new ViewModelProvider(requireActivity()).get(TracingViewModel.class);

        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.status_container, TracingBoxFragment.newInstance(true))
                .commit();

        //Genera el código COVID de autorización
        if (secureStorage.getLastInformCode() == null) {
            OtpGenerator gen = new OtpGenerator(new SecureRandom());
            secureStorage.setLastInformCode(gen.nextOtpCode());
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        infobox = view.findViewById(R.id.card_infobox);
        tracingCard = view.findViewById(R.id.card_tracing);
        cardNotifications = view.findViewById(R.id.card_notifications);
        reportStatusBubble = view.findViewById(R.id.report_status_bubble);
        reportStatusView = reportStatusBubble.findViewById(R.id.report_status);
        reportErrorView = reportStatusBubble.findViewById(R.id.report_errors);
        headerView = view.findViewById(R.id.home_header_view);
        scrollView = view.findViewById(R.id.home_scroll_view);
        cardTest = view.findViewById(R.id.card_what_to_do_test);
        cardTestFrame = view.findViewById(R.id.frame_card_test);
        loadingView = view.findViewById(R.id.loading_view);

        //Opción "Monitor de Síntomas"
        cardTriageAccessFrame = view.findViewById(R.id.frame_card_triage);
        cardTriageAccess = view.findViewById(R.id.card_goto_triage);

        //Opción "Qué hacer si tienes síntomas de la enfermedad"
        cardSymptomsFrame = view.findViewById(R.id.frame_card_symptoms);
        cardSymptoms = view.findViewById(R.id.card_what_to_do_symptoms);

        setupHeader();
        setupInfobox();
        setupTracingView();
        setupNotification();
        setupWhatToDo();
        //setupDebugButton();
        setupScrollBehavior();
    }

    @Override
    public void onStart() {
        super.onStart();
        tracingViewModel.invalidateTracingStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        headerView.stopAnimation();
    }

    private void setupHeader() {
        tracingViewModel.getAppStatusLiveData()
                .observe(getViewLifecycleOwner(), headerView::setState);
    }

    private void setupInfobox() {

        secureStorage.getInfoBoxLiveData().observe(getViewLifecycleOwner(), hasInfobox -> {
            hasInfobox = hasInfobox && secureStorage.getHasInfobox();

            if (!hasInfobox) {
                infobox.setVisibility(View.GONE);
                return;
            }
            infobox.setVisibility(VISIBLE);

            String title = secureStorage.getInfoboxTitle();
            TextView titleView = infobox.findViewById(R.id.infobox_title);
            if (title != null) {
                titleView.setText(title);
                titleView.setVisibility(VISIBLE);
            } else {
                titleView.setVisibility(View.GONE);
            }

            String text = secureStorage.getInfoboxText();
            TextView textView = infobox.findViewById(R.id.infobox_text);
            if (text != null) {
                textView.setText(text);
                textView.setVisibility(VISIBLE);
            } else {
                textView.setVisibility(View.GONE);
            }

            String url = secureStorage.getInfoboxLinkUrl();
            String urlTitle = secureStorage.getInfoboxLinkTitle();
            View linkGroup = infobox.findViewById(R.id.infobox_link_group);
            TextView linkView = infobox.findViewById(R.id.infobox_link_text);
            if (url != null) {
                linkView.setText(urlTitle != null ? urlTitle : url);
                linkGroup.setOnClickListener(v -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                });
                linkGroup.setVisibility(VISIBLE);
            } else {
                linkGroup.setVisibility(View.GONE);
            }
        });
    }

    private void setupTracingView() {

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true);
        tracingCard.setForeground(getContext().getDrawable(outValue.resourceId));

        tracingViewModel.getAppStatusLiveData().observe(getViewLifecycleOwner(), tracingStatusInterface -> {
            if (tracingStatusInterface.isReportedAsInfected()) {
                cardTestFrame.setVisibility(View.GONE);
                tracingCard.findViewById(R.id.contacs_chevron).setVisibility(View.GONE);
                tracingCard.setOnClickListener(null);
                tracingCard.setForeground(null);

                //new
                cardTriageAccessFrame.setVisibility(View.GONE);
                cardTriageAccess.setOnClickListener(null);

                cardSymptomsFrame.setVisibility(View.GONE);

            } else {
                cardTestFrame.setVisibility(VISIBLE);
                tracingCard.findViewById(R.id.contacs_chevron).setVisibility(VISIBLE);
                tracingCard.setOnClickListener(v -> showContactsFragment());

                //new
                cardTriageAccessFrame.setVisibility(VISIBLE);
                cardTriageAccess.setOnClickListener(v -> {
                    Intent i = new Intent(getActivity(), TriageActivity.class);
                    startActivity(i);
                });

                cardSymptomsFrame.setVisibility(VISIBLE);
            }
        });
    }

    private void showContactsFragment() {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
                .replace(R.id.main_fragment_container, ContactsFragment.newInstance())
                .addToBackStack(ContactsFragment.class.getCanonicalName())
                .commit();
    }

    private void setupNotification() {
        cardNotifications.setOnClickListener(
                v -> getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
                        .replace(R.id.main_fragment_container, ReportsFragment.newInstance())
                        .addToBackStack(ReportsFragment.class.getCanonicalName())
                        .commit());

        tracingViewModel.getAppStatusLiveData().observe(getViewLifecycleOwner(), tracingStatusInterface -> {
            //update status view
            if (loadingView.getVisibility() == VISIBLE) {
                loadingView.animate()
                        .setStartDelay(getResources().getInteger(android.R.integer.config_mediumAnimTime))
                        .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime))
                        .alpha(0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                loadingView.setVisibility(View.GONE);
                            }
                        });
            } else {
                loadingView.setVisibility(View.GONE);
            }
            if (tracingStatusInterface.isReportedAsInfected()) {
                NotificationStateHelper.updateStatusView(reportStatusView, NotificationState.POSITIVE_TESTED);
            } else if (tracingStatusInterface.wasContactReportedAsExposed()) {
                long daysSinceExposure = tracingStatusInterface.getDaysSinceExposure();
                NotificationStateHelper.updateStatusView(reportStatusView, NotificationState.EXPOSED, daysSinceExposure);
            } else {
                NotificationStateHelper.updateStatusView(reportStatusView, NotificationState.NO_REPORTS);
            }

            //Este bloque muestra un error de sincronización en la interfaz principal si no puede comunicarse con el servidor
            TracingStatus.ErrorState errorState = tracingStatusInterface.getReportErrorState();
            if (errorState != null) {
                TracingErrorStateHelper
                        .updateErrorView(reportErrorView, errorState);
                reportErrorView.findViewById(R.id.error_status_button).setOnClickListener(v -> {
                    loadingView.animate()
                            .alpha(1f)
                            .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime))
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    loadingView.setVisibility(VISIBLE);
                                    tracingViewModel.sync();
                                }
                            });
                });
            } else if (!isNotificationChannelEnabled(getContext(), NotificationUtil.NOTIFICATION_CHANNEL_ID)) {
                NotificatonErrorStateHelper
                        .updateNotificationErrorView(reportErrorView, NotificationStateError.NOTIFICATION_STATE_ERROR);
                reportErrorView.findViewById(R.id.error_status_button).setOnClickListener(v -> {
                    openChannelSettings(NotificationUtil.NOTIFICATION_CHANNEL_ID);
                });
            } else {
                //hide errorview
                TracingErrorStateHelper.updateErrorView(reportErrorView, null);
            }
        });
    }

    private void openChannelSettings(String channelId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().getPackageName());
            startActivity(intent);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
            startActivity(intent);
        }
    }

    private boolean isNotificationChannelEnabled(Context context, @Nullable String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!TextUtils.isEmpty(channelId)) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(channelId);
                if (channel == null) {
                    return true;
                }
                return channel.getImportance() != NotificationManager.IMPORTANCE_NONE &&
                        !(!manager.areNotificationsEnabled() &&
                                channel.getImportance() == NotificationManager.IMPORTANCE_DEFAULT) &&
                        manager.areNotificationsEnabled();
            }
            return true;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }

    private void setupWhatToDo() {
        cardSymptoms.setOnClickListener(
                v -> getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
                        .replace(R.id.main_fragment_container, WtdSymptomsFragment.newInstance())
                        .addToBackStack(WtdSymptomsFragment.class.getCanonicalName())
                        .commit());

        cardTest.setOnClickListener(
                v -> getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
                        .replace(R.id.main_fragment_container, WtdPositiveTestFragment.newInstance())
                        .addToBackStack(WtdPositiveTestFragment.class.getCanonicalName())
                        .commit());
    }

    /*private void setupDebugButton() {
        View debugButton = getView().findViewById(R.id.main_button_debug);
        if (BuildConfig.IS_DEV) {
            debugButton.setVisibility(VISIBLE);
            debugButton.setOnClickListener(v -> DebugFragment.startDebugFragment(getParentFragmentManager()));
        } else {
            debugButton.setVisibility(View.GONE);
        }
    }*/

    private void setupScrollBehavior() {

        int scrollRangePx = getResources().getDimensionPixelSize(R.dimen.top_item_padding);
        int translationRangePx = -getResources().getDimensionPixelSize(R.dimen.spacing_huge);
        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float progress = computeScrollAnimProgress(scrollY, scrollRangePx);
            headerView.setAlpha(1 - progress);
            headerView.setTranslationY(progress * translationRangePx);
        });
        scrollView.post(() -> {
            float progress = computeScrollAnimProgress(scrollView.getScrollY(), scrollRangePx);
            headerView.setAlpha(1 - progress);
            headerView.setTranslationY(progress * translationRangePx);
        });
    }

    private float computeScrollAnimProgress(int scrollY, int scrollRange) {
        return Math.min(scrollY, scrollRange) / (float) scrollRange;
    }

}
