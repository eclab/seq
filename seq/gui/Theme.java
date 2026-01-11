/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import seq.util.*;

// For images
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Image;
import javax.swing.*;

public class Theme {

    public static final Color ELECTRIC_BLUE = new Color(0,194,238);
    public static final Color BLUE = new Color(0,94,238);
    
    public static final Color SOFT_BLUE_20 = new Color(39,50,66);
    public static final Color SOFT_BLUE_25 = new Color(42,60,87);
    public static final Color SOFT_BLUE_30 = new Color(51,75,111);
    public static final Color SOFT_BLUE_40 = new Color(52,103,179);
    public static final Color SOFT_BLUE = new Color(63,107,171);

    public static final Color MUTED_BLUE_40 = new Color(42,59,93);
    public static final Color MUTED_BLUE_60 = new Color(63,65,92);
    public static final Color DARK_BLUE = new Color(43,49,75);
    public static final Color BLUE_ACCENT = new Color(42,59,93);
    //public static final Color BLUE_3 = new Color(180,0,180);

    public static final Color DEEP_GREEN = new Color(67,164,32);
    public static final Color NEON_GREEN = new Color(189,227,0);

    public static final Color RED = new Color(225,60,65);
    public static final Color MUTED_RED = new Color(129,62,63);
    public static final Color RED_ACCENT = new Color(109,49,49);

    public static final Color ORANGE = new Color(255,169,46);
    public static final Color DARK_ORANGE = new Color(126,67,35);

    public static final Color LIGHTEN = new Color(255,255,240,155);

    //public static final Color GRAY_TRANSPARENT = new Color(40,40,45,0);
    public static final Color BLACK = new Color(12,12,12);
    public static final Color GRAY_40 = new Color(40,40,45);
    public static final Color GRAY_50 = new Color(50,50,55);
    public static final Color GRAY_60 = new Color(60,60,65);
    public static final Color GRAY_70 = new Color(70,70,75);
    public static final Color GRAY_80 = new Color(80,80,85);
    public static final Color GRAY_90 = new Color(90,90,95);
    public static final Color GRAY_120 = new Color(120,120,125);
    public static final Color GRAY_140 = new Color(140,140,145);
    public static final Color WHITE_190 = new Color(190,190,190);
    public static final Color WHITE_210 = new Color(210,210,210);
    public static final Color WHITE_240 = new Color(240,240,240);


    private static boolean dark;

    static {
        dark = Prefs.getLastBoolean("Theme.dark", false);
        }

    public static boolean isDark() {
        // You could override this to just return TRUE
        return dark;
        }

    public static void setDarkNextTime(boolean val) {
        Prefs.setLastBoolean("Theme.dark", val);
        }
    
    public static void setDark(boolean val) {
        dark = val;
        setDarkNextTime(val);
        }
    

    public static BufferedImage makeBufferedImage(Image image)
        {
        image = new ImageIcon(image).getImage(); 
        BufferedImage buffered = new BufferedImage(
            image.getWidth(null),
            image.getHeight(null),
            BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = buffered.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return buffered;
        }

    private static void invertBackground(BufferedImage image)
        {

        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        // Inverts the alpha channel of the image
        for(int i = 0; i < pixels.length; i++)
            {
            int p = pixels[i];              // This is in the form ARGB, where A (Alpha) is the high bits
            int a = (p >>> 24) & 255;
            //int r = (p >>> 16) & 255;
            //int g = (p >>> 8) & 255;
            //int b = (p >>> 0) & 255;
            int rgb = p & 0xffffff;

            //pixels[i] = ((255-a) << 24) | ((r) << 16) | ((g) << 8) | ((b) << 0);
            pixels[i] = ((255-a) << 24) | rgb;
            }

        image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        }

    private static void invertForeground(BufferedImage image)
        {

        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        
        //This inverts the color of the image
        int max = 200;
        for(int i = 0; i < pixels.length; i++)
            {
            int p = pixels[i];              // This is in the form ARGB, where A (Alpha) is the high bits
            int a = (p >>> 24) & 255;
            int r = (p >>> 16) & 255;
            int g = (p >>> 8) & 255;
            int b = (p >>> 0) & 255;

            if(r>max) r = max;
            if(g>max) g = max;
            if(b>max) b = max;

            pixels[i] = (a << 24) | ((max - r) << 16) | ((max - g) << 8) | ((max - b) << 0);
            }

        image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        }

    private static void setColor(BufferedImage image, int r, int g, int b)
        {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        //This keeps the alpha values but overwrites the color of the image to a flat color
        for(int i = 0; i < pixels.length; i++)
            {
            int p = pixels[i];              // This is in the form ARGB, where A (Alpha) is the high bits
            int a = (p >>> 24) & 255;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | (b << 0);
            }

        image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        }

    public static Image paintImage(Image image, int r, int g, int b)
        {
        if (dark)
            {
            BufferedImage bufferedImage = makeBufferedImage(image);
            setColor(bufferedImage, r, g, b);
            return (Image) bufferedImage;
            }
        return image;
        }

    public static Image invertBG(Image image)
        {
        if (dark)
            {
            BufferedImage bufferedImage = makeBufferedImage(image);
            invertBackground(bufferedImage);
            return (Image) bufferedImage;
            }
        return image;
        }

    public static Image invertImage(Image image)
        {
        if (dark)
            {
            BufferedImage bufferedImage = makeBufferedImage(image);
            invertForeground(bufferedImage);
            return (Image) bufferedImage;
            }
        return image;
        }

    }
