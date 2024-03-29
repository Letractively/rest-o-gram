package rest.o.gram;

import fi.foyt.foursquare.api.entities.*;
import org.jinstagram.entity.common.Images;
import org.jinstagram.entity.media.MediaInfoFeed;
import org.jinstagram.entity.users.feed.MediaFeed;
import org.jinstagram.entity.users.feed.MediaFeedData;
import rest.o.gram.entities.RestogramPhoto;
import rest.o.gram.entities.RestogramVenue;
import rest.o.gram.service.InstagramServices.Entities.EmptyRestogramPhoto;
import rest.o.gram.service.InstagramServices.Entities.EmptyRestogramPhotos;
import rest.o.gram.service.InstagramServices.Entities.RestogramPhotos;
import rest.o.gram.utils.InstagramUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Or
 * Date: 6/16/13
 */
public final class ApisConverters {

    public static RestogramPhotos convertToRestogramPhotos(final MediaFeed mediaFeed) {
        return convertToRestogramPhotos(mediaFeed, "");
    }

    public static RestogramPhotos convertToRestogramPhotos(final MediaFeed mediaFeed, final String originVenueId) {
        if (InstagramUtils.isNullOrEmpty(mediaFeed))
            return new EmptyRestogramPhotos();

        final List<RestogramPhoto> photos = new ArrayList<>(mediaFeed.getData().size());
        for (final MediaFeedData currMediaFeedData : mediaFeed.getData())
            photos.add(convertToRestogramPhoto(currMediaFeedData, originVenueId));
        return new RestogramPhotos(photos, mediaFeed.getPagination());
    }

    public static RestogramPhoto convertToRestogramPhoto(final MediaInfoFeed mediaInfoFeed) {
        if (InstagramUtils.isNullOrEmpty(mediaInfoFeed))
            return new EmptyRestogramPhoto();

        return convertToRestogramPhoto(mediaInfoFeed.getData(), "");
    }

    public static RestogramPhoto convertToRestogramPhoto(final MediaFeedData media, String originVenueId) {
        String caption = "";
        if (media.getCaption() != null)
            caption = media.getCaption().getText();

        String user = "";
        if (media.getUser() != null)
            user = media.getUser().getUserName();
        final Images images = media.getImages();
        final String thumbnail = images.getThumbnail().getImageUrl();
        final String standardResolution = images.getStandardResolution().getImageUrl();
        return new RestogramPhoto(caption, media.getCreatedTime(), media.getId(),
                media.getImageFilter(), thumbnail, standardResolution,
                media.getLikes().getCount(), media.getLink(),
                media.getType(), user, originVenueId, media.getTags(), 0).encodeStrings();
    }

    /**
     * Converts compact venue foursquare object to restogram venue
     */
    public static RestogramVenue convertToRestogramVenue(CompactVenue venue) {
        fi.foyt.foursquare.api.entities.Location location = venue.getLocation();

        Contact contact = venue.getContact();
        String phone = "";
        if (contact != null)
            phone = contact.getPhone();

        // TODO: calculate distance using LocationUtils

        return new RestogramVenue(venue.getId(),
                venue.getName(),
                location.getAddress(),
                location.getCity(),
                location.getState(),
                location.getPostalCode(),
                location.getCountry(),
                location.getLat(),
                location.getLng(),
                location.getDistance(),
                venue.getUrl(),
                phone).encodeStrings();
    }

    /**
     * Converts complete venue foursquare object to restogram venue
     */
    public static RestogramVenue convertToRestogramVenue(CompleteVenue venue) {
        RestogramVenue result = null;
        String photoUrl;

        try {
            fi.foyt.foursquare.api.entities.Location location = venue.getLocation();

            Contact contact = venue.getContact();
            String phone = "";
            if (contact != null)
                phone = contact.getPhone();

            result = new RestogramVenue(venue.getId(),
                    venue.getName(),
                    location.getAddress(),
                    location.getCity(),
                    location.getState(),
                    location.getPostalCode(),
                    location.getCountry(),
                    location.getLat(),
                    location.getLng(),
                    0.0, // Cannot use location.getDistance()
                    venue.getUrl(),
                    phone);

            Photos photos = venue.getPhotos();
            if (photos == null)
                return result;

            PhotoGroup[] groups = photos.getGroups();
            if (groups == null || groups.length < 2)
                return result;

            PhotoGroup group = groups[1];
            Photo[] items = group.getItems();
            if (items == null || items.length == 0)
                return result;

            photoUrl = items[0].getUrlOriginal();

            result.setDescription(venue.getDescription());
            result.setImageUrl(photoUrl);
        } catch (Exception e) {
            log.severe("venue object conversion failed");
            return result;
        }

        return result.encodeStrings();
    }

    private static final Logger log = Logger.getLogger(ApisConverters.class.getName());
}
