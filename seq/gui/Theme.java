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
    static boolean dark;

    static {
        dark = Prefs.getLastBoolean("Theme.dark", true);
    }

    public static boolean isDark() {
        // You could override this to just return TRUE
        return dark;
    }

    public static void setDark(boolean val) {
        dark = val;
        Prefs.setLastBoolean("Theme.dark", val);
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

    public static void invertBackground(BufferedImage image)
        {

        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        // this just inverts the color, but we might do something cooler like making them not quite so white when inverted...
        for(int i = 0; i < pixels.length; i++)
                {
                int p = pixels[i];              // This is in the form ARGB, where A (Alpha) is the high bits
                int a = (p >>> 24) & 255;
                int r = (p >>> 16) & 255;
                int g = (p >>> 8) & 255;
                int b = (p >>> 0) & 255;
                
                pixels[i] = ((255-a) << 24) | ((250-r) << 16) | ((250-g) << 8) | ((250-b) << 0);
                }

        image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        }

    public static void invertForeground(BufferedImage image)
        {

        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        // this just inverts the color, but we might do something cooler like making them not quite so white when inverted...
        for(int i = 0; i < pixels.length; i++)
                {
                int p = pixels[i];              // This is in the form ARGB, where A (Alpha) is the high bits
                int a = (p >>> 24) & 255;
                int r = (p >>> 16) & 255;
                int g = (p >>> 8) & 255;
                int b = (p >>> 0) & 255;
                pixels[i] = (a << 24) | ((200 - r) << 16) | ((200 - g) << 8) | ((200 - b) << 0);
                }

        image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
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
