package rest.o.gram.data;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.leanengine.server.LeanException;
import com.leanengine.server.appengine.DatastoreUtils;
import com.leanengine.server.appengine.datastore.PutBatchOperation;
import com.leanengine.server.appengine.datastore.PutUpdateStrategy;
import com.leanengine.server.entity.LeanQuery;
import com.leanengine.server.entity.QueryFilter;
import com.leanengine.server.entity.QueryResult;
import com.leanengine.server.entity.QuerySort;
import org.apache.commons.lang3.StringUtils;
import rest.o.gram.ApisAccessManager;
import rest.o.gram.DataStoreConverters;
import rest.o.gram.Defs;
import rest.o.gram.entities.Kinds;
import rest.o.gram.entities.Props;
import rest.o.gram.entities.RestogramPhoto;
import rest.o.gram.results.PhotosResult;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Or
 * Date: 6/10/13
 */
public final class DataManager {

    // NON-AUTH

    public static Map<String,Boolean> getPhotoToRuleMapping(final String... ids) throws LeanException {
        final Collection<Entity> photoEntities =
                DatastoreUtils.getPublicEntities(Kinds.PHOTO, ids);
        final Map<String,Boolean> result = new HashMap<>(photoEntities.size());
        for (final Entity currPhotoEntity : photoEntities) {
            final String currPhotoId = currPhotoEntity.getKey().getName();
            if (!currPhotoEntity.hasProperty(Props.Photo.APPROVED)) {
                continue;
            }
            final boolean currApproval = (boolean) currPhotoEntity.getProperty(Props.Photo.APPROVED);
            result.put(currPhotoId, currApproval);
        }
        return result;
    }

    public static boolean savePhotoToRuleMapping(final Map<String,Boolean> photoIdToRuleMapping) {
        final PutBatchOperation putOp = DatastoreUtils.startPutBatch();
        for (final Map.Entry<String,Boolean> currEntry :  photoIdToRuleMapping.entrySet())
        {
            final String currName = currEntry.getKey();
            putOp.addEntity(Kinds.PHOTO, currName);
            putOp.addEntityProperty(currName, Props.Photo.APPROVED, currEntry.getValue());
        }
        return putOp.execute(new PutUpdateStrategy());
    }

    public static PhotosResult fetchPhotosFromCache(final String venueId, final String token) {
        final LeanQuery query = new LeanQuery(Kinds.PHOTO);
        query.addFilter(Props.Photo.ORIGIN_VENUE_ID, QueryFilter.FilterOperator.EQUAL, venueId);
        query.addFilter(Props.Photo.APPROVED, QueryFilter.FilterOperator.EQUAL, true);
        query.addSort(Props.Photo.YUMMIES, QuerySort.SortDirection.DESCENDING);
        if (StringUtils.isNotBlank(token))
            query.setCursor(Cursor.fromWebSafeString(token));
        QueryResult result = null;
        try
        {
            result = DatastoreUtils.queryEntityPublic(query);
        } catch (LeanException e)
        {
            e.printStackTrace();
            log.severe("fetching photos from cache has failed. venue: " + venueId);
        }

        return createPhotosResultFromQueryResult(result);
    }

    public static boolean isValidCursor(String token) {
        try
        {
            Cursor.fromWebSafeString(token);
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    // AUTH

    public static boolean updatePhotoReference(final String photoId, final boolean isFav) {
        final Map<String, Object> props = new HashMap<>();
        props.put(Props.PhotoRef.INSTAGRAM_ID, photoId);
        props.put(Props.PhotoRef.IS_FAVORITE, isFav);
        try {
            DatastoreUtils.putPublicEntity(Kinds.PHOTO_REFERENCE, photoId, props);
        } catch (LeanException e) {
            e.printStackTrace();
            log.severe("cannot add a photo to favorites");
            return false;
        }
        return true;
    }

    public static boolean changePhotoYummiesCount(final String photoId, final int delta) {
        int retries = 2;
        while (true)
        {
            final Transaction transaction = DatastoreUtils.buildTransaction();
            try
            {
                Entity photo  = null;
                try
                {
                    photo = DatastoreUtils.getPublicEntity(Kinds.PHOTO, photoId);
                } catch (LeanException e)
                {
                    e.printStackTrace();
                    log.warning("cannot get photo from DS");
                    transaction.rollback();
                    return false;
                }

                long yummies = 0;
                if (photo.hasProperty(Props.Photo.YUMMIES))
                    yummies = (Long)photo.getProperty(Props.Photo.YUMMIES);
                yummies += delta;
                photo.setProperty(Props.Photo.YUMMIES, yummies);
                try
                {
                    DatastoreUtils.putPublicEntity(Kinds.PHOTO, photoId, photo.getProperties());
                } catch (LeanException e)
                {
                    e.printStackTrace();
                    log.severe("cannot put photo in DS");
                    transaction.rollback();
                    return false;
                }
                transaction.commit();
                break;
            }
            catch (ConcurrentModificationException e)
            {
                if (retries == 0)
                {
                    log.severe("exceeded the number of allowed retries for yummies count update transaction");
                    return false;
                }

                --retries;
            } finally
            {
                if (transaction.isActive())
                    transaction.rollback();
            }
        }
        return true;
    }

    public static boolean isPhotoFavorite(final String photoId) {
        Entity photoEntity = null;
        try
        {
            DatastoreUtils.getPrivateEntity(Kinds.PHOTO_REFERENCE, photoId);
        } catch (LeanException e)
        {
            return false;  // no  entity-ref so it's not a favorite
        }

        return (boolean)photoEntity.getProperty(Props.PhotoRef.IS_FAVORITE);
    }

    public static boolean cachePhoto(RestogramPhoto photo) {

    try
    {
        DatastoreUtils.putPublicEntity(Kinds.PHOTO,
                photo.getInstagram_id(), DataStoreConverters.photoToProps(photo));
    }
    catch (LeanException e) {
        log.severe("caching the photo in DS has failed");
        e.printStackTrace();
        return false;
    }
    return true;
}

    public static boolean isPhotoInCache(final String photoId) {
        final LeanQuery query = new LeanQuery("Kind");
        query.addFilter(Entity.KEY_RESERVED_PROPERTY, QueryFilter.FilterOperator.EQUAL,
                        KeyFactory.createKey(Kinds.PHOTO, photoId));
        query.setKeysOnly();
        QueryResult result = null;
        try
        {
            result = DatastoreUtils.queryEntityPublic(query);
        } catch (LeanException e)
        {
            e.printStackTrace();
            log.severe("cannot query for entity existence");
        }
        return result != null  && result.getResult() != null &&
               !result.getResult().isEmpty();
    }

    /**
     * Has the photo been approved by filters and added to cache.
     */
    public static boolean isPhotoApproved(final String photoId) {
        Entity entity = null;
        try
        {
            entity =  DatastoreUtils.getPublicEntity(Kinds.PHOTO, photoId);
        } catch (LeanException e)
        {
            return false; // entity not in cache
        }

        // if 'approved' is not yet set - count it out
        if  (!entity.hasProperty(Props.Photo.APPROVED))
            return false;

        return true;
    }

    private static PhotosResult createPhotosResultFromQueryResult(QueryResult queryResult) {

        if (queryResult != null && queryResult.getResult() != null) {
            final Cursor cursor = queryResult.getCursor();
            String token = null;
            if (cursor == null) // no more results
                token = Defs.Tokens.FINISHED_FETCHING_FROM_CACHE;
            else // has more results
                token = cursor.toWebSafeString();
            final List<Entity> entities = queryResult.getResult();
            final RestogramPhoto[] result = new RestogramPhoto[entities.size()];
            int i = 0;
            for (final Entity currEntity : entities)
                result[i++] = (DataStoreConverters.entityToPhoto(currEntity));

            return new PhotosResult(result, token);
        }

        log.severe("cannot init photos query result");
        return null; // error
    }

    // mem-cache

    public static boolean isPhotoPending(final String photoId) {
        return getPendingPhoto(photoId) !=  null;
    }

    public static RestogramPhoto getPendingPhoto(final String photoId) {
        return (RestogramPhoto)getMemcacheService().get(photoId);
    }

    public static void addPendingPhoto(final RestogramPhoto pendingPhoto)  {
        // TODO: consider setting expiration...
        getMemcacheService().put(pendingPhoto.getInstagram_id(), pendingPhoto);
    }

    public static void removePendingPhoto(final String photoId) {
        getMemcacheService().delete(photoId);
    }

    private static MemcacheService getMemcacheService() {
        if (cache != null)
            return cache;
        cache = MemcacheServiceFactory.getMemcacheService();
        cache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        return  cache;
    }

    private static final Logger log = Logger.getLogger(DataManager.class.getName());
    private static MemcacheService cache = null;

}
