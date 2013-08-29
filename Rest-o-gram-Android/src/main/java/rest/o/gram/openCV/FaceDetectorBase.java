package rest.o.gram.openCV;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;
import rest.o.gram.common.Defs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Or
 * Date: 8/28/13
 */
public abstract class FaceDetectorBase implements FaceDetector {
    public FaceDetectorBase(Context context) {
        loadOpenCVClassifier(context);
    }

    /**
     * Creates a new pixel matrix from given source bitmap
     */
    protected final Mat createPixelMatrix(Bitmap source) {
        final Mat target = new Mat();
        Utils.bitmapToMat(source, target);
        return target;
    }

    /**
     * Loads an OpenCv classifier according to the definitions.
     * @param context main activity context
     */
    protected final void loadOpenCVClassifier(Context context) {
        InputStream is = null;
        FileOutputStream os = null;
        try
        {
            // load cascade file from application resources
            is = context.getResources().openRawResource(Defs.Filtering.OpenCVDetector.CASCADE_CLASSIFIER_ID);
            final File cascadeDir =
                    context.getDir(Defs.Filtering.OpenCVDetector.CASCADE_CLASSIFIERS_DIRECTORY_NAME,
                                    Context.MODE_PRIVATE);
            final File cascadeFile = new File(cascadeDir, Defs.Filtering.OpenCVDetector.CASCADE_CLASSIFIER_FILE_NAME);
            os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1)
                os.write(buffer, 0, bytesRead);
            is.close();
            os.close();

            initOpenCVClassifier(cascadeFile);

            cascadeDir.delete();

        } catch (IOException e)
        {
            e.printStackTrace();
            Log.e("REST-O-GRAM", "Failed to load cascade. Exception thrown: " + e.getMessage());
        }
        finally
        {
            try
            {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            }
            catch (IOException e2)
            {
                Log.e("REST-O-GRAM", "Cannot dispose cascade resources. Exception thrown: " + e2.getMessage());
            }
        }
    }

    protected abstract void initOpenCVClassifier(File cascadeFile);
}
