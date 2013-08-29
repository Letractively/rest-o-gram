package rest.o.gram.commands;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import rest.o.gram.cache.IBitmapCache;
import rest.o.gram.client.RestogramClient;
import rest.o.gram.entities.RestogramPhoto;
import rest.o.gram.filters.IBitmapFilter;
import rest.o.gram.view.IPhotoViewAdapter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Roi
 * Date: 06/05/13
 */
public class DownloadImageCommand extends AbstractRestogramCommand {

    public DownloadImageCommand(String url,
                                RestogramPhoto photo, IPhotoViewAdapter viewAdapter,
                                int width, int height) {
        this.url = url;
        this.photo = photo;
        this.width = width;
        this.height = height;
        this.viewAdapter = viewAdapter;
    }

    public DownloadImageCommand(Context context, String url,
                                String venueId, ImageView imageView,
                                int width, int height) {
        this.url = url;
        this.venueId = venueId;
        this.width = width;
        this.height = height;
        this.imageView = imageView;
    }

    @Override
    public boolean execute() {
        if(!super.execute())
            return false;

        if(imageView != null) {
            fetchDrawableOnThread(url, photo, imageView);
            return true;
        }

        if(viewAdapter != null) {
            fetchDrawableOnThread(url, photo.getInstagram_id(), viewAdapter);
            return true;
        }

        return false;
    }

    @Override
    public boolean cancel() {
        if(!super.cancel())
            return false;

        // Cancel
        isCanceled = true;
        return true;
    }

    private Bitmap fetchDrawable(String urlString, RestogramPhoto photo, int reqWidth, int reqHeight, boolean filter) {
        try {
            IBitmapCache cache = RestogramClient.getInstance().getBitmapCache();
            String filename = generateFilename(urlString, photo.getInstagram_id());
            Bitmap bitmap = cache.load(filename);

            if(bitmap == null) {
                // if a filter is defined and photo is not yet approved - applies filter
                if(filter && !photo.isApproved()) {
                    // Download full scale bitmap
                    bitmap = decodeBitmap(urlString);

                    // Get bitmap filter
                    final IBitmapFilter bitmapFilter = RestogramClient.getInstance().getBitmapFilter();

                    // Apply filter to bitmap
                    if(!bitmapFilter.accept(bitmap)) {
                        return null;
                    }

                    // Download scaled bitmap
                    bitmap = decodeBitmap(urlString, bitmap, reqWidth, reqHeight);
                }
                else {
                    if (filter && photo.isApproved())
                        Log.d("REST-O-GRAM", "photo is already approved!");

                    // Download scaled bitmap
                    bitmap = decodeBitmap(urlString, reqWidth, reqHeight);
                }

                // Save scaled bitmap to cache
                cache.save(filename, bitmap);
            }

            return bitmap;
        }
        catch (OutOfMemoryError e) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    private void fetchDrawableOnThread(final String urlString, final RestogramPhoto photo, final ImageView imageView) {

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                try {
                    if(isCanceled) {
                        notifyCanceled();
                        return;
                    }

                    final Bitmap bitmap = (Bitmap)message.obj;
                    if(bitmap == null) {
                        notifyError();
                        return;
                    }

                    imageView.setImageBitmap(bitmap);
                    notifyFinished();
                }
                catch(Exception e) {
                    notifyError();
                }
            }
        };

        Thread thread = new Thread() {
            @Override
            public void run() {
                Bitmap bitmap = fetchDrawable(urlString, photo, width, height, false);
                Message message = handler.obtainMessage(1, bitmap);
                handler.sendMessage(message);
            }
        };
        thread.start();
    }

    private void fetchDrawableOnThread(final String urlString, final String photoId, final IPhotoViewAdapter viewAdapter) {

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                try {
                    if(isCanceled) {
                        notifyCanceled();
                        return;
                    }

                    final Bitmap bitmap = (Bitmap)message.obj;
                    if(bitmap == null) {
                        notifyError();
                        return;
                    }

                    // Get bitmapId
                    String bitmapId = generateFilename(urlString, photoId);

                    // Add photo to view adapter
                    viewAdapter.addPhoto(photoId, bitmapId);
                    viewAdapter.refresh();

                    notifyFinished();
                }
                catch(Exception e) {
                    notifyError();
                }
            }
        };

        Thread thread = new Thread() {
            @Override
            public void run() {
                Bitmap bitmap = fetchDrawable(urlString, photo, width, height, true);
                Message message = handler.obtainMessage(1, bitmap);
                handler.sendMessage(message);
            }
        };
        thread.start();
    }

    private InputStream fetch(String urlString) throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet(urlString);
        HttpResponse response = httpClient.execute(request);
        return response.getEntity().getContent();
    }

    private String generateFilename(String urlString, String photoId) {
        return urlString.replaceAll("[^A-Za-z0-9]", "_");
    }

    private Bitmap decodeBitmap(String urlString, int reqWidth, int reqHeight) throws IOException {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream is = fetch(urlString);
        BitmapFactory.decodeStream(is, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap (new stream) with inSampleSize set
        options.inJustDecodeBounds = false;
        is = fetch(urlString);
        return BitmapFactory.decodeStream(is, null, options);
    }

    private Bitmap decodeBitmap(String urlString, final Bitmap source, int reqWidth, int reqHeight) throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = source.getHeight();
        options.outWidth = source.getWidth();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap (new stream) with inSampleSize set
        options.inJustDecodeBounds = false;
        InputStream is = fetch(urlString);
        return BitmapFactory.decodeStream(is, null, options);
    }

    private Bitmap decodeBitmap(String urlString) throws IOException {
        InputStream is = fetch(urlString);
        return BitmapFactory.decodeStream(is);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    private String url;
    private ImageView imageView;
    private RestogramPhoto photo;
    private String venueId;
    private int width;
    private int height;
    private IPhotoViewAdapter viewAdapter;
    private boolean isCanceled = false;
}
