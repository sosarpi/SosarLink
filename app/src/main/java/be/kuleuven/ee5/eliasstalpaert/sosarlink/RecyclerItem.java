package be.kuleuven.ee5.eliasstalpaert.sosarlink;

public class RecyclerItem {
    private int mImageResource;
    private String mText1, mText2;

    public RecyclerItem(int mImageResource, String mText1, String mText2) {
        this.mImageResource = mImageResource;
        this.mText1 = mText1;
        this.mText2 = mText2;
    }

    public int getImageResource() {
        return mImageResource;
    }

    public String getText1() {
        return mText1;
    }

    public String getText2() {
        return mText2;
    }
}
