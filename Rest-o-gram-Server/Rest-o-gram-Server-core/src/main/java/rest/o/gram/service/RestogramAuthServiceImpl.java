package rest.o.gram.service;

import com.leanengine.server.LeanException;
import com.leanengine.server.appengine.DatastoreUtils;
import org.jinstagram.entity.media.MediaInfoFeed;
import rest.o.gram.ApisConverters;
import rest.o.gram.Defs;
import rest.o.gram.InstagramAccessManager;
import rest.o.gram.DataStoreConverters;
import rest.o.gram.data.DataManager;
import rest.o.gram.entities.Kinds;
import rest.o.gram.entities.RestogramPhoto;
import rest.o.gram.iservice.RestogramAuthService;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Or
 * Date: 5/19/13
 */
public class RestogramAuthServiceImpl implements RestogramAuthService {

    @Override
    public boolean addPhotoToFavorites(final String photoId, final String originVenueId) {
        if (!DataManager.updatePhotoReference(photoId, true))
        {
            log.severe("cannot update photo reference");
            return false;
        }

        if (DataManager.isPhotoInCache(photoId)) // in DS already
        {
            log.info("YUMMIES: already in DS");
            return DataManager.changePhotoYummiesCount(photoId, 1);
        }
        else if (DataManager.isPhotoPending(photoId)) //  pending photo
        {
            log.info("YUMMIES: photo is pending");
            final RestogramPhoto pendingPhoto = DataManager.getPendingPhoto(photoId);
            pendingPhoto.setYummies(1); // TODO: handle consistency if we care enough..
            try
            {
                DatastoreUtils.putPublicEntity(Kinds.PHOTO, pendingPhoto.getInstagram_id(),
                                                DataStoreConverters.photoToProps(pendingPhoto));
            } catch (LeanException e)
            {
                e.printStackTrace();
                log.severe("cannot write an updated photo(yummies counter) to cache");
                return false;
            }
        }
        else //  get from instagram
        {
            log.info("YUMMIES: getting from instagram");
            final InstagramAccessManager.PrepareRequest prepareRequest =
                    new InstagramAccessManager.PrepareRequest() {
                        @Override
                        public byte[] getPayload() {
                            return photoId.getBytes();
                        }
                    };
            final MediaInfoFeed mediaInfoFeed =
                    InstagramAccessManager.parallelFrontendInstagramRequest(Defs.Instagram.RequestType.GetPhoto,
                                                                            prepareRequest,
                                                                            MediaInfoFeed.class);
            if (mediaInfoFeed  == null)
                return false;

            final RestogramPhoto photo = ApisConverters.convertToRestogramPhoto(mediaInfoFeed.getData(), originVenueId);
            photo.setYummies(1);
            return DataManager.cachePhoto(photo);
        }
        return true;
    }

    @Override
    public boolean removePhotoFromFavorites(String photoId) {
        if (!DataManager.updatePhotoReference(photoId, false))
        {
            log.severe("cannot update photo reference");
            return false;
        }

        return DataManager.changePhotoYummiesCount(photoId, -1);
    }

    private static final Logger log = Logger.getLogger(RestogramAuthServiceImpl.class.getName());
}
