package rest.o.gram.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import rest.o.gram.R;
import rest.o.gram.activities.visitors.IActivityVisitor;
import rest.o.gram.client.RestogramClient;
import rest.o.gram.common.Defs;
import rest.o.gram.common.Utils;
import rest.o.gram.entities.RestogramVenue;
import rest.o.gram.filters.BitmapFilterInitCallback;
import rest.o.gram.filters.IBitmapFilter;
import rest.o.gram.location.ILocationObserver;
import rest.o.gram.location.ILocationTracker;
import rest.o.gram.network.INetworkStateProvider;
import rest.o.gram.tasks.results.GetNearbyResult;

/**
 * Created with IntelliJ IDEA.
 * User: Roi
 * Date: 16/04/13
 */
public class HomeActivity extends RestogramActivity implements ILocationObserver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restart application (if needed)
        if(Utils.restartIfNeeded(this))
            return;

        setContentView(R.layout.home);

        if (!Defs.Filtering.JavaCV.USE_OPENCV_MANAGER_INIT)
                trackLocation();
        // if uses openCV manager, has to init bitmap filter here
        // TODO: refactor init through openCV manager
        else {
            final BitmapFilterInitCallback callback = new BitmapFilterInitCallback() {
                @Override
                public void onBitmapFilterInit(final IBitmapFilter filter) {
                    trackLocation();
                }
            };

            RestogramClient.getInstance().initializeBitmapFilterAsync(this, callback);
        }
    }

    private void trackLocation() {
        // Get location tracker
        tracker = RestogramClient.getInstance().getLocationTracker();
        if (tracker == null)
            Log.e("REST-O-GRAM", "no location tracker has been loaded");
        else
        {
            if (!tracker.canDetectLocation())
                dialogManager.showLocationTrackingAlert(this);
            else {
                updateStatus(R.string.location_search);
                tracker.setObserver(this);
                tracker.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tracker != null)
            tracker.stop();
        cancelProgress();
    }

    @Override
    protected void onDestroy() { // Activity exiting
        super.onDestroy();

        if(tracker != null)
            tracker.stop();
    }

    @Override
    public void onLocationUpdated(double latitude, double longitude, int accuracy) {
        if (gotLocation)
            return;
        gotLocation = true;

        if(tracker != null) {
            tracker.stop();

            if(RestogramClient.getInstance().isDebuggable()) {
                Log.d("REST-O-GRAM", "Location tracking finished!");
                Log.d("REST-O-GRAM", "Tracker = " + tracker.getClass() +
                                     " Latitude = " + latitude +
                                     " Longitude = " + longitude +
                                     " Accuracy = " + accuracy);
            }
        }

        final INetworkStateProvider netStateProvider =
                RestogramClient.getInstance().getNetworkStateProvider();
        if (netStateProvider != null)
        {
            if (!netStateProvider.isOnline())
            {
                dialogManager.showNetworkStateAlert(this);
                return;
            }
        }
        else {
            // TODO: implementation
            return;
        }

        updateStatus(R.string.venue_search);

        // Send get nearby request
        RestogramClient.getInstance().getNearby(latitude, longitude, Defs.Location.DEFAULT_NEARBY_RADIUS, this);
    }

    @Override
    public void onTrackingTimedOut() {
        if(tracker != null)
            tracker.stop();
        dialogManager.showLocationTrackingAlert(this);
    }

    @Override
    public void onFinished(GetNearbyResult result) {
        super.onFinished(result);

        resetStatus();

        if(result == null) {
            if(RestogramClient.getInstance().isDebuggable()) {
                Log.d("REST-O-GRAM", "an error occurred while searching for venues");
                onError();
            }
            return;
        }

        final RestogramVenue[] venues = result.getVenues();
        if(venues == null || venues.length == 0) {
            if (RestogramClient.getInstance().isDebuggable()) {
                if (venues == null)
                    Log.d("REST-O-GRAM", "an error occurred while searching for venues");
                else
                    Log.d("REST-O-GRAM", "no venues found");
            }

            isFoundVenues = false;
        }
        else {
            isFoundVenues = true;
        }

        if(Defs.Flow.WELCOME_SCREENS_ENABLED) {
            if(Utils.isShowWelcomeScreen(this)) {
                setContentView(R.layout.welcome);
                return;
            }
        }

        start();
    }

    @Override
    public void onCanceled() {
        super.onCanceled();

        if(tracker == null)
            return;

        if(isFinishing())
            return;

        Toast.makeText(this, R.string.venue_search_failed, Toast.LENGTH_LONG).show();

        // Retry - send get nearby request
        RestogramClient.getInstance().getNearby(tracker.getLatitude(), tracker.getLongitude(), Defs.Location.DEFAULT_NEARBY_RADIUS, this);
    }

    @Override
    public void accept(IActivityVisitor visitor) {
        visitor.visit(this);
    }

    public void onOkClicked(View view) {
        Utils.setIsShowWelcomeScreen(this, false);
        start();
    }

    private void start() {
        if(!isFoundVenues) {
            // Show error dialog and switch to map after "ok"
            dialogManager.showNoVenuesAlert(this, true);
        }
        else {
            if(RestogramClient.getInstance().getAuthenticationProvider().isUserLoggedIn())
                onUserLoggedIn();
            // Switch to "ExploreActivity" with no parameters
            Intent intent = new Intent(this, ExploreActivity.class);
            Utils.changeActivity(this, intent, Defs.RequestCodes.RC_EXPLORE, true);
        }
    }

    private void cancelProgress() {
        View v = findViewById(R.id.pbLoading);
        if(v == null)
            return;

        ProgressBar pb = (ProgressBar)v;
        pb.setVisibility(View.GONE);
    }

    private void updateStatus(int status) {
        Utils.updateTextView((TextView)findViewById(R.id.tvStatus), status);
    }

    private void resetStatus() {
        Utils.updateTextView((TextView)findViewById(R.id.tvStatus), "");
    }

    private ILocationTracker tracker; // Location tracker
    private boolean gotLocation;
    private boolean isFoundVenues = false;
}