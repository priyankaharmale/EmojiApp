package com.makemoji.mojilib.gif;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

final class SimpleBitmapProvider implements GifDecoder.BitmapProvider {
    @NonNull @Override public Bitmap obtain(int width, int height, Bitmap.Config config) {
        return Bitmap.createBitmap(width, height, config);
    }

    @Override public void release(Bitmap bitmap) {
        bitmap.recycle();
    }

    /**
     * Returns a byte array used for decoding and generating the frame bitmap.
     *
     * @param size the size of the byte array to obtain
     */
    public byte[] obtainByteArray(int size){
        return new byte[size];
    }

    /**
     * Releases the given byte array back to the pool.
     */
    public void release(byte[] bytes){

    }

    /**
     * Returns an int array used for decoding/generating the frame bitmaps.
     */
    public int[] obtainIntArray(int size){
        return new int[size];
    }

    /**
     * Release the given array back to the pool.
     */
    public void release(int[] array){

    }

}