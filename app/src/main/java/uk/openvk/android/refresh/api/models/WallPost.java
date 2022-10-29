package uk.openvk.android.refresh.api.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import uk.openvk.android.refresh.api.attachments.Attachment;
import uk.openvk.android.refresh.api.counters.PostCounters;

public class WallPost implements Parcelable {

    private String avatar_url;
    public Bitmap avatar;
    public String name;
    public RepostInfo repost;
    public String info;
    public String text;
    public int owner_id;
    public int post_id;
    public PostCounters counters;
    public int author_id;
    public int dt_sec;
    public ArrayList<Attachment> attachments;

    public WallPost(String author, int dt_sec, RepostInfo repostInfo, String post_text, PostCounters nICI, String avatar_url, ArrayList<Attachment> attachments, int o_id, int p_id, Context ctx) {
        name = author;
        repost = repostInfo;
        counters = nICI;
        text = post_text;
        this.avatar_url = avatar_url;
        owner_id = o_id;
        post_id = p_id;
        this.dt_sec = dt_sec;
        this.attachments = attachments;
    }

    public WallPost() {

    }

    protected WallPost(Parcel in) {
        avatar_url = in.readString();
        avatar = in.readParcelable(Bitmap.class.getClassLoader());
        name = in.readString();
        info = in.readString();
        text = in.readString();
        owner_id = in.readInt();
        post_id = in.readInt();
        author_id = in.readInt();
    }

    public static final Creator<WallPost> CREATOR = new Creator<WallPost>() {
        @Override
        public WallPost createFromParcel(Parcel in) {
            return new WallPost(in);
        }

        @Override
        public WallPost[] newArray(int size) {
            return new WallPost[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(avatar_url);
        parcel.writeParcelable(avatar, i);
        parcel.writeString(name);
        parcel.writeString(info);
        parcel.writeString(text);
        parcel.writeInt(owner_id);
        parcel.writeInt(post_id);
        parcel.writeInt(author_id);
    }
}
