package csbslovenia.com.DInkan;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Cveto on 23.8.2014.
 */
public class BitmapCreator {
    // document size and DPI
    private int DPI;
    private int bitmapWidth;
    private int bitmapHeight;

    // Bitmap, Canvas, Paint
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;

    // Assets (so the different FONT work outside of an activity - access to Assets)
    private AssetManager asset;

    // Create size of bitmap, new canvas new paint, get assets
    public void createBitmapCanvasPaint(int bitmapWidth, int bitmapHeight,int DPI, AssetManager asset) {
        this.DPI = DPI;

        // Set bitmap size
        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;

        // Bitmap, Canvas, Paint
        this.bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(bitmap);
        this.paint = new Paint();

        //asset
        this.asset = asset;

        //background
        this.paint.setColor(Color.WHITE);
        this.canvas.drawRect(0,0,bitmapWidth,bitmapHeight,paint);
    }

    // Puts background around QR code - METRIC
    public void greenBackground(int top, int left, int height, int width, int color) {
        top = milimetersToPixels(top);
        left = milimetersToPixels(left);
        width = milimetersToPixels(width);
        height = milimetersToPixels(height);

        // The green background is in fact for LineWidth wider than the size we gave it. Why? Because I'm BATMAN!
        int lineWidth = milimetersToPixels(2);      // 2 millimeters

        // Where to put the green square?
        int qrPosTop = top;
        int qrPosBottom = top + height;
        int qrPosLeft = left;
        int qrPosRight = left + width;

        // Set color and make it for lineWidth bigger on all sizes.
        paint.setColor(color);
        canvas.drawRect(qrPosLeft - lineWidth, qrPosTop - lineWidth, qrPosRight + lineWidth, qrPosBottom + lineWidth, paint);
    }

    // Takes text, creates QR code and puts it on the bitmap. (calls setPixels method)
    public void putQRtoBitmap(String data, int top, int left, int size) {
        top = milimetersToPixels(top);
        left = milimetersToPixels(left);
        size = milimetersToPixels(size);

        int qrPosLeft = left;
        int qrPosTop = top;
        int qrWidth = size;
        int qrHeight = size;

        int[] pixels = getQRpixels(data, qrWidth, qrHeight, "UTF-8");
        bitmap.setPixels(pixels, 0, qrWidth, qrPosLeft, qrPosTop, qrWidth, qrHeight);
    }

    /** ZXING magic**/
    // Accepts String, calls the QRcode creater, returns QR code in int array form
    private int[] getQRpixels(String input, int width, int height, String encoding) {
        //Size of pixels array
        int[] pixels = new int[width * height];

        // set QR background and pixel color
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;

        Map<EncodeHintType, Object> hints = null;

        hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, encoding);

        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix result = writer.encode(input, BarcodeFormat.QR_CODE, width, height, hints);

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return pixels;
    }

    /** Text Justifyer MAGIC **/
    // draws simple Text.
    public void drawText(String text,Integer left, Integer top, Integer textSize) {
        left = milimetersToPixels(left);
        top = milimetersToPixels(top);
        textSize = (int) fontPoints(textSize);

        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);

        Typeface tf = Typeface.createFromAsset(asset, "fonts/URANIA_CZECH.ttf");   //Typeface tf = Typeface.create("monospace",Typeface.BOLD);
        paint.setTypeface(tf);

        canvas.drawText(text, left, top, paint);
    }

    // Draws Justified Multiline Text. Accepts measurement in millimeters. Florjan TM
    public float drawJustifiedTextMetric(
        String text,
        int textSize,
        float textSpacing,
        int xLeft,
        int xRight,
        int yTop,
        boolean justifyLastOrBrokenLines) {

        textSize = (int) fontPoints(textSize);
        xLeft = milimetersToPixels(xLeft);
        xRight = milimetersToPixels(xRight);
        yTop = milimetersToPixels(yTop);

        // draws text, and at the same time tells you how height it is. (depending on spacing, text size, text font...)
        int height = drawJustifiedText(text,textSize,textSpacing,xLeft,xRight,yTop,justifyLastOrBrokenLines);
        // returns height in millimeters.
        return pixelsToMilimeters(height);
    }

    // Draw Justified text. Accept measurements in raw pixels.
    private int drawJustifiedText(
            String text,
            int textSize,
            float textSpacing,
            int xLeft,
            int xRight,
            int yTop,
            boolean justifyLastOrBrokenLines) {

        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);
        Typeface tf = Typeface.createFromAsset(asset, "fonts/URANIA_CZECH.ttf");   //Typeface tf = Typeface.create("monospace",Typeface.BOLD);
        paint.setTypeface(tf);

        // Starting conditions
        String text_line = "";
        String text_rest = text;

        // Top part of text starts there, not bottom.
        yTop = yTop+textSize*3/4;

        //  How many fit?
        int maxTextOnLine = paint.breakText(text_rest, 0, text_rest.length(), true, xRight - xLeft, null);
        // if newLine character betwen

        // Counter for y position
        int i = 0;

        /**For text that doesn't fit in one line**/
        if (text_rest.length() > maxTextOnLine+1) {         //why + 1? It was putting half lines on new line. needs testing
            do {
                //How many characters fit on the line?
                maxTextOnLine = paint.breakText(text_rest, 0, text_rest.length(), true, xRight - xLeft, null);

                // Find where to remove partial word.
                while (text_rest.charAt(maxTextOnLine-1) != " ".charAt(0)) {
                    maxTextOnLine--;
                }

                // Is there a "\n" in the string?
                boolean foundNewLine = false;
                int newMaxTextOnLine = checkForNewLines(text_rest,maxTextOnLine);
                // was there a "\n" on th next printed line?
                if (newMaxTextOnLine != maxTextOnLine) {
                    foundNewLine = true;
                    maxTextOnLine = newMaxTextOnLine;
                }  //what if newTextOnLine = 0? if it is the next one!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                // Split string on two strings - Remove partial word - skip new line
                text_line = text_rest.substring(0, maxTextOnLine);
                if (foundNewLine) {
                    maxTextOnLine +=1;            // gets stuck in a loop otherwise  \n is only 1 character, not 2!
                    drawJustifiedOrNormalText(text_line,xLeft,xRight,yTop,textSize,textSpacing,paint,canvas,i,justifyLastOrBrokenLines);
                } else {
                    drawJustifiedOrNormalText(text_line,xLeft,xRight,yTop,textSize,textSpacing,paint,canvas,i,true);
                }
                text_rest = text_rest.substring(maxTextOnLine, text_rest.length());



                i++;
            }  while (text_rest.length() > text_line.length());
        }



        /** For Last or Only line. **/
        text_line = text_rest;
        // If you find a lineBreak (if breakPos is not -1, therefore 0 or more)
        for(int breakPos = text_line.indexOf("\n"); breakPos >= 0; ) {
            // Text to display is only up till the breakPosition
            text_line = text_rest.substring(0, breakPos);
            // Save rest of the text. Ommit the linebrak (breakPos+1)
            text_rest = text_rest.substring(breakPos+1, text_rest.length());

            drawJustifiedOrNormalText(text_line,xLeft,xRight,yTop,textSize,textSpacing,paint,canvas,i,justifyLastOrBrokenLines);
            // go to next line
            i++;
            // find new break postiion
            breakPos = text_rest.indexOf("\n");
        }

        drawJustifiedOrNormalText(text_rest,xLeft,xRight,yTop,textSize,textSpacing,paint,canvas,i,justifyLastOrBrokenLines);

        // returns height of message.
        return (int) ((i+1) * textSize * textSpacing);
    }

    // This one actually draws the justified Text.
    private void justifyCalculation(String text_line,int xLeft,int xRight,int yTop,int textSize,float textSpacing,Paint paint, Canvas canvas, int i) {
        paint.setTextSize(textSize);

        // Create array of words
        String[] words = createArrayOfWords(text_line);
        int numberOfWords = words.length;
        int numberOfSpaces = numberOfWords-1;

        // Length of the words without spaces;
        float lenWordsNoSpaces = measureLenOfStringsInStringArray(words,paint);

        // How much whitespace is there to fill?
        float emtpySpaceInLine = xRight-xLeft-lenWordsNoSpaces;

        // How many per space
        float emptySpacePerSpace = emtpySpaceInLine/numberOfSpaces;

        // Display data
        float cumulativeLength = 0;
        for (int j=0;j<numberOfWords;j++) {
            canvas.drawText(words[j],xLeft + cumulativeLength,yTop + i * textSize * textSpacing,paint);
            cumulativeLength += paint.measureText(words[j]) + emptySpacePerSpace;
        }

    }

    // A supporting method. Chooses between a justified line or non justified line.
    private void drawJustifiedOrNormalText(
            String text_line,
            int xLeft,
            int xRight,
            int yTop,
            int textSize,
            float textSpacing,
            Paint paint,
            Canvas canvas,
            int i,
            boolean justified) {

        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);
        Typeface tf = Typeface.createFromAsset(asset, "fonts/URANIA_CZECH.ttf");   //Typeface tf = Typeface.create("monospace",Typeface.BOLD);
        paint.setTypeface(tf);


        if (justified) {
            justifyCalculation(text_line,xLeft,xRight,yTop,textSize,textSpacing,paint,canvas,i);
        } else {
            canvas.drawText(text_line,0,text_line.length(), xLeft, yTop + i * textSize * textSpacing, paint);
        }

    }

    // Find if there is a \n in the first XX position of  String.
    private int checkForNewLines(String text, int maxChars) {
        // Search for nex \n
        int breakPos = text.indexOf("\n");

        // new max chars. If new line found and if its not too far.
        if (breakPos >= 0 && breakPos < maxChars) {
            return breakPos;
        }
        // old max chars (no new line found).
        return maxChars;
    }

    // Takes a String, and put its words in an array. (Needed for Justifying purposes).
    private String[] createArrayOfWords(String string) {
        String[] words = string.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].replaceAll(" ", "");
        }
        return words;
    }

    // Measures the length of the words without spaces - lenght in sence of how many pixels, based ont text font and size.
    private float measureLenOfStringsInStringArray(String[] stringArray, Paint paint) {
        float num = 0;
        for (int i = 0; i < stringArray.length; i++) {
            num += paint.measureText(stringArray[i]);
        }
        return num;
    }

    // Specific - Draws a line and under it "Full name".
    public void drawImeInPriimek(String text, int left,int top,int length, int width) {
        left = milimetersToPixels(left);
        length = milimetersToPixels(length);
        top = milimetersToPixels(top);

        int bottom = top+width;
        length = left+length;

        paint.setColor(Color.BLACK);
        paint.setTextSize(fontPoints(12));

        canvas.drawRect(left,top,length,bottom,paint);
        canvas.drawText(text,0,text.length(),left,top + milimetersToPixels(4),paint);
    }

    // Accepts pixels and returns measurement in millimeters. Needs DPI
    private int pixelsToMilimeters(int pixels) {
        try {
            float milimetrs = (float) pixels / (float) DPI * (float) 25.4;
            return (int) milimetrs;
        } catch (Exception e) {
            Log.e("Milimeter "," to pixels. Error here.");
        }
        return 0;
    }

    // Accepts milimeters and returns size in pixels. Needs DPI
    private int milimetersToPixels(int milimeters) {
        float pixels = (float) bitmapHeight / (float) 297 * (float) milimeters;
        return (int) pixels;
    }

    // Transforms point Text size to milimeters (NOT pixels).
    public float fontPoints(int points) {
        float sizeInches = (float)points / 72;
        float sizeMilimeters = sizeInches * 254 / 10;
        return milimetersToPixels((int) sizeMilimeters);
    }

    // Returns Bitmap from this class
    public Bitmap getBitmap() {
        return this.bitmap;
    }
}