package com.tengen.XmlParser;

/**
 * User: pgajjar Date: 8/26/13 Time: 11:19 AM
 */
@SuppressWarnings("unused")
public class MZMCDXmlTagName {
    public enum MovieTag {
        MOVIE_TAG_NAME("movie"),
        CLOCK_TAG_NAME("clock"),
        CODECS_ATTR_NAME("codecs"),
        TYPE_ATTR_NAME("type"),
        POSTER_TIME_ATTR_NAME("poster_time"),
        PREVIEW_DURATION_ATTR_NAME("preview_duration"),
        PREVIEW_TIME_ATTR_NAME("preview_time"),
        TIME_SCALE_ATTR_NAME("time_scale"),
        TOTAL_DURATION_ATTR_NAME("total_duration"),
        MATRIX_NAME("matrix"),
        IDENTITY_ATTR_NAME("identity"),
        METADATA_ATOMS("metadata_atoms"),
        FORMAT_ATTR_NAME("format"),
        LOCALE_ATTR_NAME("locale"),
        TRACKS_NAME("tracks");

        private final String tagName;

        MovieTag(String tagName) {
            this.tagName = tagName;
        }

        public String getTag() {
            return tagName;
        }
    }

    public enum MovieTrackType {
        VIDEO_TRACK("video"),
        SOUND_TRACK("sound"),
        TIMECODE_TRACK("timecode"),
        CHAPTER_TRACK("chapter"),
        CHAPTER_IMAGE_TRACK("chapter_image"),
        UNKNOWN_TRACK("unknown");

        private final String trackType;

        MovieTrackType(String trackType) {
            this.trackType = trackType;
        }

        public String getTrackType() {
            return trackType;
        }
    }
}