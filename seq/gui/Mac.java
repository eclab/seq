/***
    Copyright 2025 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package seq.gui;

import java.lang.reflect.*;

// Code for handling MacOS X specific About Menus.  Maybe also we'll handle Preferences later.
//
// Inspired by https://stackoverflow.com/questions/7256230/in-order-to-macify-a-java-app-to-catch-the-about-event-do-i-have-to-implement
//
// I used the reflection version so it compiles cleanly on linux and windows as well.

public class Mac
    {
    public static void setup(SeqUI ui)
        {
        if (System.getProperty("os.name").contains("Mac")) 
            {
			// System.setProperty("apple.awt.application.appearance", "system" );		// not working?  The point was to change the window title bar
            System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            try
                {
                java.awt.Desktop.getDesktop().setAboutHandler(new java.awt.desktop.AboutHandler()
                    {
                    public void handleAbout(java.awt.desktop.AboutEvent e)
                        {
                        ui.doAbout();
                        }
                    });

                java.awt.Desktop.getDesktop().setQuitHandler(new java.awt.desktop.QuitHandler()
                    {
                    public void handleQuitRequestWith(java.awt.desktop.QuitEvent e, java.awt.desktop.QuitResponse response)
                        {
                        if (ui.doQuit())
                            {
                            response.performQuit();
                            }
                        else
                            {
                            response.cancelQuit();
                            }
                        }
                    });
                }
            catch (Exception e)
                {
                //fail quietly
                }

            try 
                {
                Object app = Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null);

                Object al = Proxy.newProxyInstance(
                    Class.forName("com.apple.eawt.AboutHandler").getClassLoader(),
                    new Class[]{Class.forName("com.apple.eawt.AboutHandler")},
                    new AboutListener(ui));

                app.getClass().getMethod("setAboutHandler", Class.forName("com.apple.eawt.AboutHandler")).invoke(app, al);

                al = Proxy.newProxyInstance(
                    Class.forName("com.apple.eawt.QuitHandler").getClassLoader(),
                    new Class[]{Class.forName("com.apple.eawt.QuitHandler")},
                    new QuitListener(ui));

                app.getClass().getMethod("setQuitHandler", Class.forName("com.apple.eawt.QuitHandler")).invoke(app, al);
                }
            catch (Exception e) 
                {
                //fail quietly
                }
            }       
        }
    }
        
class AboutListener implements InvocationHandler 
    {
    SeqUI ui;
    public AboutListener(SeqUI ui) { this.ui = ui; }
    public Object invoke(Object proxy, Method method, Object[] args) 
        {
        ui.doAbout();
        return null;
        }
    }

class QuitListener implements InvocationHandler 
    {
    SeqUI ui;
    public QuitListener(SeqUI ui) { this.ui = ui; }
    public Object invoke(Object proxy, Method method, Object[] args) 
        {
        ui.doQuit();
        return null;
        }
    }
