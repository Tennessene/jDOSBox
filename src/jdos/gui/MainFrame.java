package jdos.gui;

import jdos.sdl.GUI;
import jdos.Dosbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.MemoryImageSource;

public class MainFrame implements GUI {
    int[] pixels = new int[16 * 16];
    Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
    Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");

    public void showProgress(String msg, int percent) {
        
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

    public static void main(String[] args) {
        frame = new MyFrame();
        frame.setFocusTraversalKeysEnabled(false);
        panel = new JPanel() {
            public void paint(Graphics g) {
                if (Main.buffer != null) {
                    if (fullscreen) {
                        g.drawImage(Main.buffer, fullscreen_cx_offset, 0, fullscreen_cx+fullscreen_cx_offset,  fullscreen_cy, 0, 0, Main.buffer_width, Main.buffer_height, null);
                    } else {
                        g.drawImage(Main.buffer, 0, 0, Main.screen_width,  Main.screen_height, 0, 0, Main.buffer_width, Main.buffer_height, null);
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
        }

        public void mouseMoved(MouseEvent e) {
            Main.addEvent(e);
        }

        public void mouseDragged(MouseEvent e) {
            Main.addEvent(e);
        }

    }
}
