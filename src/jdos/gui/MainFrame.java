package jdos.gui;

import jdos.sdl.GUI;
import jdos.Dosbox;
import jdos.ints.Mouse;
import jdos.ints.Int10_modes;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.event.*;
import java.awt.image.MemoryImageSource;
import java.awt.image.BufferedImage;
import java.security.AccessControlException;

public class MainFrame implements GUI {
    int[] pixels = new int[16 * 16];
    Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
    Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");

    static Robot robot;
    static private boolean eatNextMouseMove = false;
    static private int last_x;
    static private int last_y;

    public void showProgress(String msg, int percent) {
        
    }

    static public void robotMouse(MouseEvent e, Point rel) {
         if (eatNextMouseMove) {
            last_x = e.getX();
            last_y = e.getY();
            eatNextMouseMove = false;
            return;
        }
        if (!Main.mouse_locked || robot == null)
            Main.addEvent(e);
        else {
            int rel_x = e.getX() - last_x;
            int rel_y = e.getY() - last_y;
            float abs_x = (Mouse.mouse.x+rel_x*Main.mouse_sensitivity/100.0f)/(Mouse.mouse.max_x);
            float abs_y = (Mouse.mouse.y+rel_y*Main.mouse_sensitivity/100.0f)/(Mouse.mouse.max_y);
            Main.addEvent(new Main.MouseEvent2(e,rel_x, rel_y, abs_x, abs_y));
            robotCenter(rel);
        }
    }
    static public void robotCenter(Point rel) {
        eatNextMouseMove = true;
        robot.mouseMove(rel.x+200, rel.y+200);
    }

    public void captureMouse(boolean on) {
        if (robot != null) {
            robotCenter(panel.getLocationOnScreen());
        }
    }
    public void showCursor(boolean on) {
        if (on)
            frame.setCursor(Cursor.getDefaultCursor());
        else
            frame.setCursor(transparentCursor);
    }
    static private boolean fullscreen = false;
    static private int fullscreen_cx = 0;
    static private int fullscreen_cy = 0;
    static private int fullscreen_cx_offset = 0;
    static private int cx = 0;
    static private int cy = 0;
    public void fullScreenToggle() {
        if (fullscreen) {
            frame.setVisible(false);
            fullscreen = false;
            frame.setUndecorated(false);
            frame.setExtendedState(Frame.NORMAL);
            setSize(cx, cy);
        } else {
            Toolkit tk = Toolkit.getDefaultToolkit();
            fullscreen_cx = ((int) tk.getScreenSize().getWidth());
            fullscreen_cy = ((int) tk.getScreenSize().getHeight());
            frame.setVisible(false);
            frame.setUndecorated(true);
            frame.setResizable(false);
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            setSize(fullscreen_cx, fullscreen_cy);
            fullscreen_cx_offset = 0;
            if ((float)fullscreen_cx/fullscreen_cy > 4.0/3.0) {
                int new_fullscreen_cx = fullscreen_cy*4/3;
                fullscreen_cx_offset = (fullscreen_cx - new_fullscreen_cx)/2;
                fullscreen_cx = new_fullscreen_cx;
            }
            fullscreen = true;
        }
    }

    public void setSize(int width, int height) {
        cx = width;
        cy = height;
        if (fullscreen)
            return;
        panel.setMinimumSize(new Dimension(width, height));
        panel.setMaximumSize(new Dimension(width, height));
        panel.setPreferredSize(new Dimension(width, height));
        frame.getContentPane().doLayout();
        frame.pack();
        if (!frame.isVisible())
            frame.setVisible(true);
    }
    public void dopaint() {
        panel.repaint();
    }
    public void setTitle(String title) {
        if (frame != null)
            frame.setTitle(title);
    }

    public static GraphicsConfiguration getDefaultConfiguration() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.getDefaultConfiguration();
    }

    static BufferedImage tmpImage;

    public static BufferedImage resizeImage(BufferedImage source, int destWidth, int destHeight, Object interpolation) {
        if (source == null)
            throw new NullPointerException("source image is NULL!");
        if (destWidth <= 0 && destHeight <= 0)
            throw new IllegalArgumentException("destination width & height are both <=0!");
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        double xScale = ((double) destWidth) / (double) sourceWidth;
        double yScale = ((double) destHeight) / (double) sourceHeight;
        if (destWidth <= 0) {
            xScale = yScale;
            destWidth = (int) Math.rint(xScale * sourceWidth);
        }
        if (destHeight <= 0) {
            yScale = xScale;
            destHeight = (int) Math.rint(yScale * sourceHeight);
        }
        if (tmpImage == null) {
            try {
                GraphicsConfiguration gc = getDefaultConfiguration();
                tmpImage = gc.createCompatibleImage(destWidth, destHeight, source.getColorModel().getTransparency());
            } catch (Throwable e) {
                tmpImage = new BufferedImage(destWidth, destHeight, source.getColorModel().getTransparency());
            }
        }
        Graphics2D g2d = null;
        try {
            g2d = tmpImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
            g2d.drawRenderedImage(source, at);
        } finally {
            if (g2d != null)
                g2d.dispose();
        }
        return tmpImage;
    }

    public static void main(String[] args) {
        try {robot = new Robot();} catch (Throwable e) {System.out.println("Applet is not signed, mouse capture will not work");}

        frame = new MyFrame();
        frame.setFocusTraversalKeysEnabled(false);
        frame.addFocusListener(new FocusListener() {
                private final KeyEventDispatcher altDisabler = new KeyEventDispatcher() {
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        if (e.getKeyCode() == 18) {
                            Main.addEvent(e);
                            return true;
                        }
                        return false;
                    }
                };

                public void focusGained(FocusEvent e) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(altDisabler);
                }

                public void focusLost(FocusEvent e) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(altDisabler);
                }
            });

        panel = new JPanel() {
            public void paint(Graphics g) {
                if (Main.buffer != null) {
                    if (fullscreen) {
                        g.drawImage(Main.buffer, fullscreen_cx_offset, 0, fullscreen_cx+fullscreen_cx_offset,  fullscreen_cy, 0, 0, Main.buffer_width, Main.buffer_height, null);
                    } else {
                        if (Render.render.aspect && (Main.screen_height % Main.buffer_height)!=0) {
                            BufferedImage resized = resizeImage(Main.buffer,Main.screen_width,Main.screen_height,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g.drawImage(resized, 0, 0, Main.screen_width,  Main.screen_height, 0, 0, Main.screen_width, Main.screen_height, null);
                        } else {
                            g.drawImage(Main.buffer, 0, 0, Main.screen_width,  Main.screen_height, 0, 0, Main.buffer_width, Main.buffer_height, null);
                        }
                    }
                }
            }
        };
        panel.addMouseMotionListener((MyFrame)frame);
        panel.addMouseListener((MyFrame)frame);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (!Dosbox.applet)
                    System.exit(0);
            }
        });
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.PAGE_START);
        Main.main(new MainFrame(), args);
    }

    private static JFrame frame;
    private static JPanel panel;

    private static class MyFrame extends JFrame implements KeyListener, WindowFocusListener, WindowListener, MouseListener, MouseMotionListener {
        public MyFrame() {
            addKeyListener(this);
            addWindowFocusListener(this);
            addWindowListener(this);
        }
        public void keyTyped(KeyEvent e) {

        }

        /** Handle the key pressed event from the text field. */
        public void keyPressed(KeyEvent e) {
            Main.addEvent(e);
        }

        /** Handle the key released event from the text field. */
        public void keyReleased(KeyEvent e) {
            Main.addEvent(e);
        }

        public void windowGainedFocus(WindowEvent e) {
            Main.addEvent(new Main.FocusChangeEvent(true));
            synchronized (Main.pauseMutex) {
                Main.pauseMutex.notify();
            }
        }

        public void windowLostFocus(WindowEvent e) {
            Main.addEvent(new Main.FocusChangeEvent(false));
        }

        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            synchronized (Main.pauseMutex) {
                Main.pauseMutex.notify();
            }
            Main.addEvent(null);
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            Main.addEvent(e);
        }

        public void mouseReleased(MouseEvent e) {
            Main.addEvent(e);
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount()==2 && e.getButton() == MouseEvent.BUTTON3) {
                Main.GFX_CaptureMouse();
            }
        }

        public void mouseMoved(MouseEvent e) {
            robotMouse(e, panel.getLocationOnScreen());
        }

        public void mouseDragged(MouseEvent e) {
            robotMouse(e, panel.getLocationOnScreen());
        }

    }
}
