package Graphics;
import Graphics.GUI.*;
import java.awt.*;
import java.util.ArrayList;

class Line {
    int x1, y1, x2, y2, t;
    Color c;
}

class Point {
    int x, y, r;
    Color c;
}

public class Renderer {
    public static Renderer instance;

    Window win;
    public GUI gui;
    float scale = 0.001f; /* meters per pixel */
    float offset_x = 0.05f, offset_y = 0.6f;

    volatile ArrayList<Line> lines;
    volatile ArrayList<Point> points;

    public Renderer(Window w, GUI g) {
        win = w;
        gui = g;
        lines = new ArrayList<Line>();
        points = new ArrayList<Point>();

        instance = this;
       
        System.out.println(g);

        gui.queueText("Start", 0.05f, 0.05f, 0.1f, 0.05f);
        gui.applyQueue();
    }

    volatile boolean rendering, writing;

    void zoomOut(){
        scale *= 1.1f;
    }
    void zoomIn(){
        scale *= 0.9f;
    }
    void move(int dir){
        switch(dir){
            case 0 -> offset_y += scale * 25.0f;
            case 1 -> offset_x += scale * 25.0f;
            case 2 -> offset_y -= scale * 25.0f;
            case 3 -> offset_x -= scale * 25.0f;
        }
    }    

    void Render(Graphics2D g){
        while(writing);

        rendering = true;
        for(Line l : lines){
            g.setColor(l.c);
            g.setStroke(new BasicStroke(l.t));
            g.drawLine(l.x1, l.y1, l.x2, l.y2);
        }

        for(Point p : points){
            g.setColor(p.c);
            g.setStroke(new BasicStroke(p.r * 2));
            g.drawLine(p.x - p.r, p.y, p.x - p.r, p.y);
        }

        rendering = false;
    }

    public void ClearLines(){
        writing = true;
        while(rendering);
        lines.clear();
        writing = false;
    }

    public void AddLine(float x1, float y1, float x2, float y2, float thickness, Color c){
        writing = true;
        Line l = new Line();
        
        l.x1 = (int)(x1 / scale) + (int)(offset_x / scale);
        l.x2 = (int)(x2 / scale) + (int)(offset_x / scale);
        l.y1 = (int)(y1 / scale) + (int)(offset_y / scale);
        l.y2 = (int)(y2 / scale) + (int)(offset_y / scale);
        l.t = (int)(thickness / scale);
        l.c = c;

        while(rendering);
        lines.add(l); 
        writing = false;
    }

    public void ClearPoints(){
        writing = true;
        while(rendering);
        points.clear();
        writing = false;
    }

    public void AddPoint(float x, float y, float r, Color c){
        writing = true;
        Point p = new Point();

        p.x = (int)(x / scale) + (int)(offset_x / scale);
        p.y = (int)(y / scale) + (int)(offset_y / scale);
        p.r = (int)(r/scale);
        p.c = c;

        while(rendering);
        points.add(p);
        writing = false;
    }
}
