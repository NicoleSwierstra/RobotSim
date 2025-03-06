package Graphics;

import Graphics.GUI.GUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Window {

    public class GraphicPanel extends JComponent implements Runnable {

        private static final long serialVersionUID = 1L;

        int panewidth = 1440, paneheight = 1080;

        Point mouse_loc;

        boolean renderGame;

        Renderer tr;
        GUI interact;

        GraphicPanel(Renderer r, GUI in) {
            tr = r;
            interact = in;
            setPreferredSize(new Dimension(panewidth, paneheight));
            JComponent pane = this;
            pane.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent componentEvent) {
                    paneheight = pane.getHeight();
                    panewidth = pane.getWidth();
                }
            });
            setFocusTraversalKeysEnabled(false);
            setFocusable(true);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0.05f, 0.05f, 0.1f));
            g.fillRect(0, 0, panewidth, paneheight);
            tr.Render((Graphics2D)g);
            java.awt.Point p = MouseInfo.getPointerInfo().getLocation();
            java.awt.Point o = this.getLocationOnScreen();
            float mousex = (float)(p.getX() - o.getX()) / panewidth, 
                mousey = (float)(p.getY() - o.getY()) / paneheight;
            interact.render(g, panewidth, paneheight, mousex, mousey);
        }

        public void run() {
            long end;
            while(true) {
                end = System.nanoTime() + 16666666;
                this.repaint();
                while(System.nanoTime() < end);
            }
        }
    }

    JFrame mainwindow;
    GraphicPanel g;
    public Renderer r;
    public GUI gui;
    long start = System.currentTimeMillis();

    public Window(){
        mainwindow = new JFrame("Poker");
        mainwindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui = new GUI();
        r = new Renderer(this, gui);
        g = new GraphicPanel(r, gui);
        setupInput();
        mainwindow.add(g);
        mainwindow.pack();
        mainwindow.setVisible(true);
    }

    public void start(){
        g.run();
    }

    void setupInput(){
        g.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                float x = e.getX() / (float)g.panewidth;
                float y = e.getY() / (float)g.paneheight;
                gui.onMouse(x, y);
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });
        g.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {
                gui.textInput(e);


                switch(e.getKeyCode()){
                    case KeyEvent.VK_EQUALS -> r.zoomIn();
                    case KeyEvent.VK_MINUS -> r.zoomOut();
                    case KeyEvent.VK_UP -> r.move(0);
                    case KeyEvent.VK_LEFT -> r.move(1);
                    case KeyEvent.VK_DOWN -> r.move(2);
                    case KeyEvent.VK_RIGHT -> r.move(3);
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }
}
