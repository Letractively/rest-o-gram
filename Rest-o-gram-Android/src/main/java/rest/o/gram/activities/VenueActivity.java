package rest.o.gram.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.leanengine.LeanAccount;
import rest.o.gram.R;
import rest.o.gram.client.RestogramClient;
import rest.o.gram.common.Defs;
import rest.o.gram.common.LoginHelper;
import rest.o.gram.common.Utils;
import rest.o.gram.data_history.IDataHistoryManager;
import rest.o.gram.entities.RestogramPhoto;
import rest.o.gram.entities.RestogramVenue;
import rest.o.gram.filters.RestogramFilterType;
import rest.o.gram.tasks.ITaskObserver;
import rest.o.gram.tasks.results.*;
import rest.o.gram.view.PhotoViewAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Roi
 * Date: 16/04/13
 */
public class VenueActivity extends Activity implements ITaskObserver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.venue);

        // Get venue parameter
        RestogramVenue venue;
        try {
            Intent intent = getIntent();
            venue = (RestogramVenue)intent.getSerializableExtra("venue");
        }
        catch(Exception e) {
            // TODO: implementation
            return;
        }

        // Save venue if needed
        IDataHistoryManager dataHistoryManager = RestogramClient.getInstance().getDataHistoryManager();
        if(dataHistoryManager != null)
            dataHistoryManager.save(venue, Defs.Data.SortOrder.SortOrderLIFO);

        // Initialize login helper
        loginHelper = new LoginHelper(this);

        // Initialize using venue parameter
        initialize(venue);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (viewAdapter != null)
            viewAdapter.clear();
    }

    @Override
    public void onFinished(GetNearbyResult result) {
        // Empty
    }

    @Override
    public void onFinished(GetInfoResult result) {
        // Empty
    }

    @Override
    public void onFinished(GetPhotosResult result) {
        if(result == null)
            return;

        if(RestogramClient.getInstance().isDebuggable())
            Log.d("REST-O-GRAM", "Adding " + result.getPhotos().length + " photos");

        // Add new photos
        addPhotos(result.getPhotos());

        // Update last token
        lastToken = result.getToken();

        // Update request pending flag
        isRequestPending = false;
    }

    @Override
    public void onFinished(CachePhotoResult result) {
        // Empty
    }

    @Override
    public void onFinished(FetchPhotosFromCacheResult result) {
        // Empty
    }

    @Override
    public void onFinished(CacheVenueResult result) {
        // Empty
    }

    @Override
    public void onFinished(FetchVenuesFromCacheResult result) {
        // Empty
    }

    @Override
    public void onCanceled() {
        // Empty
    }

    public void onFavoriteClicked(View view) {
        if(!LeanAccount.isUserLoggedIn()) {
            loginHelper.login();
        }
        else {
            // TODO: Add\Remove favorite
        }
    }

    /**
     * Initializes using given venue
     */
    private void initialize(RestogramVenue venue) {
        this.venue = venue;

        // Init photo grid view
        GridView gv = (GridView)findViewById(R.id.gvPhotos);
        viewAdapter = new PhotoViewAdapter(this, Defs.Photos.THUMBNAIL_WIDTH, Defs.Photos.THUMBNAIL_HEIGHT);
        gv.setAdapter(viewAdapter);

        // Set scroll listener
        gv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Empty
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if(totalItemCount == 0)
                    return;

                // Check whether the last view is visible
                if(++firstVisibleItem + visibleItemCount > totalItemCount) {
                    onScrollBottom();
                }
            }
        });

        // Set UI with venue information
        Utils.updateTextView((TextView)findViewById(R.id.tvVenueName), venue.getName());
        if(venue.getAddress() != null)
            Utils.updateTextView((TextView)findViewById(R.id.tvVenueAddress), venue.getAddress());
        if(venue.getPhone() != null)
            Utils.updateTextView((TextView)findViewById(R.id.tvVenuePhone), venue.getPhone());

        // Set UI with venue image
        if(venue.getImageUrl() != null && !venue.getImageUrl().isEmpty()) {
            ImageView iv = (ImageView)findViewById(R.id.ivVenue);
            RestogramClient.getInstance().downloadImage(venue.getImageUrl(), iv, true, null);
        }

        // Send get photos request
        RestogramClient.getInstance().getPhotos(venue.getFoursquare_id(), RestogramFilterType.Simple, this);
    }

    private void addPhotos(RestogramPhoto[] photos) {
        // Traverse given photos
        for(final RestogramPhoto photo : photos) {
            // Download image
            RestogramClient.getInstance().downloadImage(photo.getThumbnail(), photo, viewAdapter, false, null);
        }
    }

    private void onScrollBottom() {
        if(isRequestPending)
            return;

        // if session is not yet over - request next photos
        if (lastToken != null) {
            isRequestPending = true;
            if(RestogramClient.getInstance().isDebuggable())
                Log.d("REST-O-GRAM", "Requesting more photos");

            RestogramClient.getInstance().getNextPhotos(lastToken, this);
        }
    }

    private RestogramVenue venue; // Venue object
    private PhotoViewAdapter viewAdapter; // View adapter
    private String lastToken = null; // Last token
    private boolean isRequestPending = false; // Request pending flag
    private LoginHelper loginHelper; // Login helper
}
