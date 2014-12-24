package csbslovenia.com.DInkan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.print.PrintHelper;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class displayBitmap extends Activity {
    private final String TAG = displayBitmap.class.getSimpleName();
    static Bitmap bitmap;
    static File file;

    // Bitmap size in milimeters (A4 format)
    final int A4PORTRAIT = 297;         // in milimeters
    final int A4LANDSCAPE = 210;        // in milimeters

    // Set DPI (because nobody knows how to use dots per milimeter)
    final int DPI = 300;                // min 150, max 350. 350 makes a bitmap 4095 high, which is Android maximum

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_bitmap);

        // Get document and signature from previous activity
        Intent previousIntent = getIntent();
        String B64docUnsigned = previousIntent.getExtras().getString("B64docUnsigned");
        String B64signature = previousIntent.getExtras().getString("B64signature");
        String STdocumentContent = previousIntent.getExtras().getString("documentContent");

        /**Building the Bitmap:**/
        // Bitmap works with pixels, not milimeters...
        final int pxBitmapHeight = millimetersToPixels(A4PORTRAIT);
        final int pxBitmapWidth = millimetersToPixels(A4LANDSCAPE);

        // Create Bitmap, Canvas and Paint in bitmapCreator
        BitmapCreator bitmapCreator = new BitmapCreator();
        bitmapCreator.createBitmapCanvasPaint(pxBitmapWidth,pxBitmapHeight,DPI,getAssets());

        int textHeight;

        int yTop = 20;
        int textSize = 16;
        float textSpacing = (float) 1;
        int xLeft =15;
        int xRight = A4LANDSCAPE-xLeft;
        int textWidth = xRight-xLeft;

        // Title
        bitmapCreator.drawText(getResources().getString(R.string.bitmapTitle),xLeft,yTop,textSize);

        // SubTitle
        yTop += 3;                                  // millimeters under Title
        textSize = 12;
        bitmapCreator.drawJustifiedTextMetric(getResources().getString(R.string.bitmapSubTitle),textSize,textSpacing,xLeft,xRight,yTop,false);

        // QR digital document
        yTop += 20;                                 // millimeters under Subtitle. Not entirely true, since green line is set to go around QR code.
        int qrWidth = 80;
        int qrHeight = qrWidth;
        int qrPosLeft = A4LANDSCAPE/2-qrWidth/2;     // in the middle
        int color = Color.GREEN;

            // Green background for QR code
        bitmapCreator.greenBackground(yTop,qrPosLeft,qrHeight,qrWidth, color);
            // Document
        bitmapCreator.putQRtoBitmap(B64docUnsigned,yTop,qrPosLeft,qrHeight);

        // Tex Under QRcode
        yTop += qrHeight + 5;
        bitmapCreator.drawText(getResources().getString(R.string.bitmapUnderQRDoc), qrPosLeft, yTop, textSize);

        // Contents of the documents in Human readable
        yTop += 10;
        color = Color.argb(20,0,255,0);
            // Try because it might not fit to the bitmap.
        try {
            textHeight = (int) bitmapCreator.drawJustifiedTextMetric(STdocumentContent,textSize,textSpacing,xLeft,xRight,yTop,false);
            bitmapCreator.greenBackground(yTop,xLeft,textHeight,textWidth, color);
            yTop += textHeight;
        } catch (Exception e) {
            customToast(getResources().getString(R.string.tooMuchText));
            Log.e(TAG,"Too much Text");
            finish();
        }

        // Signature
        int qrSignWidth = 25;
        int qrSignHeight = qrSignWidth;
        int qrSignPosLeft = xLeft;
        int qrSignPosTop = yTop+10;

        // Places a green square behind the signatrue
        try {
            // If the signature can not fit the page, i.e. to many lines of text.
            if ((qrSignPosTop+qrSignHeight+xLeft) > A4PORTRAIT) {
                throw new Exception();
            }
            color = Color.GREEN;
            bitmapCreator.greenBackground(qrSignPosTop, qrSignPosLeft, qrSignHeight, qrSignWidth, color);
        } catch (Exception e){
            customToast(getResources().getString(R.string.tooMuchText));
            Log.e(TAG,"Too much Text");
            finish();
        }

        // Place a signature only if there is one, otherwise a white empty square (coz otherwise itll all be green)
        if (!B64signature.equals("")) {
            bitmapCreator.putQRtoBitmap(B64signature, qrSignPosTop, qrSignPosLeft, qrSignWidth);
        } else {
            int lineWidth = 2; //mm
            color = Color.WHITE;
            bitmapCreator.greenBackground(qrSignPosTop+lineWidth,qrSignPosLeft+lineWidth,qrSignHeight-2*lineWidth,qrSignWidth-2*lineWidth, color);
        }

        // Under Signature
        yTop = qrSignPosTop+qrSignHeight+5;
        bitmapCreator.drawText(getResources().getString(R.string.bitmapSignature),qrSignPosLeft,yTop,textSize);

        // Right from signature
        int xLeftTemp = qrSignPosLeft+qrSignWidth+5;
        yTop = qrSignPosTop + qrSignHeight/3;
        int lineWidth = 2;
        int lineLenght = 45;
        bitmapCreator.drawImeInPriimek(getResources().getString(R.string.bitmapFullName), xLeftTemp,yTop,lineLenght,lineWidth);


        /**Show the bitmap**/
        // get Bitmap from bitmapCreator
        bitmap = bitmapCreator.getBitmap();

        // Set bitmap to fill 90% of the screen size (different for each device)
        ImageView myImage = (ImageView) findViewById(R.id.result);
        setImageViewSize(myImage,60, myImage.getHeight(), myImage.getWidth());

        myImage.setImageBitmap(bitmap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_bitmap, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            // When a user clicks back button to fix the text - bitmap must be destroyed to avoid OutOfMemory Error
            bitmap.recycle();
            bitmap=null;
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    // converts milimeters to Pixels, acoarding to the DPI of this file
    private int millimetersToPixels(int milimeters) {
        try {
            float pixels = (float) DPI / (float) 25.4 * (float) milimeters;             // 1 inch  = 2,54 cm = 25,4mm
            return (int) pixels;
        } catch (Exception e) {
            Log.e("Milimeter "," to pixels. Error here.");
        }
        return 0;
    }

    // Pixels to Milimeters
    private int pixelsToMilimeters(int pixels) {
        try {
            float milimetrs = (float) pixels / (float) DPI * (float) 25.4;
            return (int) milimetrs;
        } catch (Exception e) {
            Log.e("Milimeter "," to pixels. Error here.");
        }
        return 0;
    }

    // Custom Toast.
    private void customToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    // Get device screen size inpixels
    private Map<String,Integer> screenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);          // gets size in pixels

        // Associative array in java (I'm a PHP programmer after all)
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("height", size.y);
        map.put("width",  size.y);

        return map;
    }

    // Set size of Imageview. How many % of the screen should it fit?
    private void setImageViewSize(ImageView imageView, int percentSize, int imageHeight, int imageWidth) {
        // Which is bigger, width or height?
        String whichIsBihher =  imageHeight >= imageWidth ? "imageHeight" : "imageWidth";

        // Get screen size in dp
        int screenHeight = screenSize().get("height");
        int screenWidth = screenSize().get("width");

        // Set new size of bitmap.
        int pxHeight;
        int pxWidth;
        // if bitmap is bigger in height
        if (whichIsBihher.equals(imageHeight)) {
            pxHeight = screenHeight * percentSize / 100;
            // Maintain aspect ratio
            pxWidth = pxHeight * A4LANDSCAPE / A4PORTRAIT;
        // if bitmap is bigger in width
        } else {
            pxWidth = screenWidth * percentSize / 100;
            // Maintain aspect ratio
            pxHeight = pxWidth * A4PORTRAIT / A4LANDSCAPE;
        }

        // Pixels to DP
        //pxWidth = pixelsToDp(pxWidth);
        //pxHeight = pixelsToDp(pxHeight);

        // Set bitmap size - according to the size device screen
        imageView.getLayoutParams().height = pxHeight;
        imageView.getLayoutParams().width = pxWidth;
    }

    // Transform pixels to dp - each device different
    private int pixelsToDp(int pixels) {
        float density = this.getResources().getDisplayMetrics().density;
        float dp = pixels / density;
        return (int) dp;
    }

    // Transform dp to pixels - each device different
    private int dpToPixels(int dp) {
        float density = this.getResources().getDisplayMetrics().density;
        float px = dp * density;
        return (int) px;
    }

    /**PRINT**/
    // Print the bitmap. Works great for printing with Photosmart, but doesnt seem to work with saving to PDF or drive.
    public void printBitmap(MenuItem mi) {
        if (!PrintHelper.systemSupportsPrint()) {
            //Toast.makeText(this, "Printing from this phone not supporeted.", Toast.LENGTH_SHORT).show();
            customToast("Printing from this phone not suppored.");
            return;
        }
        PrintHelper photoPrinter = new PrintHelper(this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        photoPrinter.setOrientation(PrintHelper.ORIENTATION_PORTRAIT);
        photoPrinter.setColorMode(PrintHelper.COLOR_MODE_COLOR);
        photoPrinter.printBitmap("PaperVault", bitmap);
    }

    /**SAVE PNG. Preferably to SD card**/
    public void saveBitmap(MenuItem mi) {
        customToast("Just a moment, please...");

        String folder = "/PaperVault/temp";
        if (mi.getItemId() == R.id.save) {
            folder = "/PaperVault";
        }

        // compress
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);

        // store
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/PaperVault");
        dir.mkdirs();

        //This file is local
        File file = new File(dir, "DigDoc" +"_"+System.currentTimeMillis()+".png");

        try {
            file.createNewFile();
            FileOutputStream fo = new FileOutputStream(file);
            fo.write(bytes.toByteArray());
            fo.flush();
            fo.close();

            // Putting to gallery doesnt work
            // MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(), file.getName(), file.getName());

            customToast("Image stored to:\n"+file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**SHARE to other apps. Due to the large size of the Bitmap, the file must be saved first. Not okay for security...**/
    public void shareBitmap(MenuItem mi) {
        // Toasts will have to be put in a new Tread, since they appear too late.
        customToast("Just a moment, please.");

        // I intend to send a PNG image
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");

        // Compress the bitmap to PNG
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);

        // Temporarily store the image to Flash
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/PaperVault/temp");
        dir.mkdirs();

        // This file is static - so I can delete it in the next method
        file = new File(dir, "temp.png");

        try {
            file.createNewFile();
            FileOutputStream fo = new FileOutputStream(file);
            fo.write(bytes.toByteArray());
            fo.flush();
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Log.d("Florjan: dir and file getname", dir.getAbsolutePath() + "/" + file.getName());
        //Log.d("Florjan: Current", file.getPath());
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + file.getPath()));

        //startActivity(Intent.createChooser(share, "Share Image"));
        startActivityForResult(Intent.createChooser(share,"Share Image"),1);

        //Delete Temporary file:
        // I need a better solution for this. I tried not saving it to flash in the first place, but that made the app crash.
        // file.delete();        // deletes it too fast
        file.deleteOnExit();     // sometimes works???


    }

}
