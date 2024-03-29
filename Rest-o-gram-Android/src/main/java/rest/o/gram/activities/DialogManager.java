package rest.o.gram.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.leanengine.LoginDialog;
import com.leanengine.LoginListener;
import rest.o.gram.R;
import rest.o.gram.client.RestogramClient;
import rest.o.gram.common.Defs;
import rest.o.gram.common.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Or
 * Date: 5/9/13
 */
public final class DialogManager {
    public void showLocationTrackingAlert(final Activity activity) {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                showErrorAlert(activity, R.string.cannot_track_location_err_msg,
                               Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            }
        }, 500);
    }

    public void showNetworkStateAlert(final Activity activity) {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                showErrorAlert(activity, R.string.no_connectivity_err_msg,
                        Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            }
        }, 500);
    }

    public void showNoVenuesAlert(final Activity activity, final boolean switchToMap) {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                showErrorAlert(activity, R.string.no_venues_err_msg, switchToMap);
            }
        }, 500);
    }

    public void showNoPhotosAlert(final Activity activity) {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                showErrorAlert(activity, R.string.no_photos_err_msg, false);
            }
        }, 500);
    }

    public void showLoginDialog(final Activity activity, LoginListener loginListener) {
        if(!Utils.isActivityValid(activity))
            return;

        Uri loginUri = RestogramClient.getInstance().getAuthenticationProvider().getFacebookLoginUri();

        final LoginDialog fbDialog =
                new LoginDialog(activity, loginUri.toString(), loginListener);

        dialogs.add(fbDialog);
        fbDialog.show();
    }

    public void showExitAlert(final Activity activity) {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!Utils.isActivityValid(activity))
                    return;

                if(isExitDialogShown)
                    return;

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

                // Setting Dialog Title
                alertDialog.setTitle(R.string.restogram_title);

                // Setting Dialog Message
                alertDialog.setMessage(R.string.exit_msg);

                // On pressing yes button
                alertDialog.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                isExitDialogShown = false;
                                dialog.cancel();
                                dialogs.remove(dialog);
                                activity.finish();
                            }
                        });

                // On pressing no button
                alertDialog.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                isExitDialogShown = false;
                                dialog.cancel();
                                dialogs.remove(dialog);
                            }
                        });

                // On dismiss
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        isExitDialogShown = false;
                    }
                });

                dialogs.add(alertDialog.show());
                isExitDialogShown = true;
            }
        }, 100);
    }

    public void showConnectionErrorAlert(final Activity activity) {
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!Utils.isActivityValid(activity))
                    return;

                if(isConnectionErrorDialogShown)
                    return;

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

                // Setting Dialog Title
                alertDialog.setTitle(R.string.restogram_title);

                // Setting Dialog Message
                alertDialog.setMessage(R.string.connection_error);

                // On pressing exit button
                alertDialog.setPositiveButton("Exit",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                isConnectionErrorDialogShown = false;
                                dialog.cancel();
                                dialogs.remove(dialog);

                                // Shutdown the application
                                RestogramClient.getInstance().getApplication().shutdown();
                            }
                        });

                // On pressing restart button
                alertDialog.setNegativeButton("Restart",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                isConnectionErrorDialogShown = false;
                                dialog.cancel();
                                dialogs.remove(dialog);

                                // Dispose client
                                RestogramClient.getInstance().dispose();

                                // Restart the application
                                RestogramClient.getInstance().getApplication().restart();

                                // Switch to "HomeActivity", finish current activity
                                Intent intent = new Intent(activity, HomeActivity.class);
                                Utils.changeActivity(activity, intent, Defs.RequestCodes.RC_HOME, true);
                            }
                        });

                // On dismiss
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        isConnectionErrorDialogShown = false;
                    }
                });

                dialogs.add(alertDialog.show());
                isConnectionErrorDialogShown = true;
            }
        }, 100);
    }

    public void clear() {
        for (DialogInterface diag : dialogs)
            diag.cancel();
        dialogs.clear();

        isExitDialogShown = false;
        isConnectionErrorDialogShown = false;
    }

    private void showErrorAlert(final Activity activity, final int message, final String action) {
        if(!Utils.isActivityValid(activity))
            return;

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

        // Setting Dialog Title
        alertDialog.setTitle(R.string.restogram_error_title);

        // Setting Dialog Message
        alertDialog.setMessage(message);

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(action);
                        activity.startActivity(intent);
                    }
                });

        // On pressing cancel button
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        dialogs.remove(dialog);
                        activity.finish();
                    }
                });

        dialogs.add(alertDialog.show());
    }

    private void showErrorAlert(final Activity activity, final int message, final boolean switchToMap) {
        if(!Utils.isActivityValid(activity))
            return;

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

        // Setting Dialog Title
        alertDialog.setTitle(R.string.restogram_error_title);

        // Setting Dialog Message
        alertDialog.setMessage(message);

        // On pressing ok button
        alertDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        dialogs.remove(dialog);

                        if(switchToMap) {
                            // Switch to "MapActivity" with no parameters
                            Intent intent = new Intent(activity, MapActivity.class);
                            Utils.changeActivity(activity, intent, Defs.RequestCodes.RC_MAP, true);
                        }
                    }
                });

        dialogs.add(alertDialog.show());
    }

    private final List<DialogInterface> dialogs = new ArrayList<>();
    private boolean isExitDialogShown = false;
    private boolean isConnectionErrorDialogShown = false;
}