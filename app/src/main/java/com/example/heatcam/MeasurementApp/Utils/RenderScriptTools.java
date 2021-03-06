package com.example.heatcam.MeasurementApp.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;

import android.renderscript.*;


import com.xxxyyy.testcamera2.ScriptC_yuv42088;

import java.nio.ByteBuffer;

public class RenderScriptTools {
    private RenderScript rs;

    public RenderScriptTools(Context context) {
        rs = RenderScript.create(context);
    }

    public Bitmap YUV_420_888_toRGB(Image image, int width, int height, int rotation){
        // Get the three image planes
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] y = new byte[buffer.capacity()];
        buffer.get(y);

        buffer = planes[1].getBuffer();
        byte[] u = new byte[buffer.capacity()];
        buffer.get(u);

        buffer = planes[2].getBuffer();
        byte[] v = new byte[buffer.capacity()];
        buffer.get(v);

        // get the relevant RowStrides and PixelStrides
        // (we know from documentation that PixelStride is 1 for y)
        int yRowStride= planes[0].getRowStride();
        int uvRowStride= planes[1].getRowStride();  // we know from   documentation that RowStride is the same for u and v.
        int uvPixelStride= planes[1].getPixelStride();  // we know from   documentation that PixelStride is the same for u and v.


        ScriptC_yuv42088 mYuv420=new ScriptC_yuv42088 (rs);

        // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
        // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.
        Type.Builder typeUcharY = new Type.Builder(rs, Element.U8(rs));
        typeUcharY.setX(yRowStride).setY(height);
        Allocation yAlloc = Allocation.createTyped(rs, typeUcharY.create());
        yAlloc.copy1DRangeFrom(0, y.length, y);
        mYuv420.set_ypsIn(yAlloc);

        Type.Builder typeUcharUV = new Type.Builder(rs, Element.U8(rs));
        // note that the size of the u's and v's are as follows:
        //      (  (width/2)*PixelStride + padding  ) * (height/2)
        // =    (RowStride                          ) * (height/2)
        // but I noted that on the S7 it is 1 less...
        typeUcharUV.setX(u.length);
        Allocation uAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        uAlloc.copyFrom(u);
        mYuv420.set_uIn(uAlloc);

        Allocation vAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        vAlloc.copyFrom(v);
        mYuv420.set_vIn(vAlloc);

        // handover parameters
        mYuv420.set_picWidth(width);
        mYuv420.set_uvRowStride (uvRowStride);
        mYuv420.set_uvPixelStride (uvPixelStride);

        Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Allocation outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

        Script.LaunchOptions lo = new Script.LaunchOptions();
        lo.setX(0, width);  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, height);

        mYuv420.forEach_doConvert(outAlloc,lo);
        outAlloc.copyTo(outBitmap);
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        matrix.preScale(1, 1);
        outBitmap = Bitmap.createBitmap(outBitmap, 0, 0, outBitmap.getWidth(), outBitmap.getHeight(), matrix, true);

        return outBitmap;
    }

     
}
