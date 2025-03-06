
import Graphics.Renderer;
import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

class MapPoint {
    float x, y;

    public MapPoint(float x, float y){
        this.x = x;
        this.y = y;
    }

    float distTo(float x2, float y2){
        float xo = x2 - x,
              yo = y2 - y;

        return (float)Math.sqrt(xo*xo + yo*yo);
    }

    float distTo(MapPoint mp){
        return distTo(mp.x, mp.y);
    }

    public String toString(){
        return "(" + x + ", " + y + ")";
    }
}

class treeNode {
    public ArrayList<Integer> mappoints;

    public treeNode() {
        mappoints = new ArrayList<Integer>();
    }
}

public class Map {
    float thickness; /* in meters */
    ArrayList<MapPoint> mp;
    treeNode[][] tree;

    public static final int TreeDiv = 16;
    public static final float TreeBounds = 3.0f;

    Map(float linewidth){
        thickness = linewidth;
        mp = new ArrayList<MapPoint>();
        tree = new treeNode[TreeDiv][TreeDiv];
    }

    Map(MapPoint[] points, float linewidth){
        thickness = linewidth;
        mp = new ArrayList<MapPoint>(Arrays.asList(points));
        tree = new treeNode[TreeDiv][TreeDiv];
    }

    public boolean sampleMap(float x, float y){
        ArrayList<Integer> points = sampleAt(x, y);

        for (int i : points){
            if(i == mp.size() - 1) continue;
            if(mp.get(i).distTo(x, y) > 5.0f * thickness) continue;
            
            float p1x = mp.get(i).x - x;
            float p1y = mp.get(i).y - y;
            float p2x = mp.get(i+1).x - x;
            float p2y = mp.get(i+1).y - y;
   
            float distbetween = (float)Math.sqrt((p2x - p1x) * (p2x - p1x) + (p2y - p1y) * (p2y - p1y));    
            float disttoline = Math.abs((p2x * p1y) - (p2y * p1x)) / distbetween;
            boolean bounds1 = (float)Math.sqrt(p1x * p1x + p1y * p1y - (disttoline * disttoline)) / distbetween < 1;
            boolean bounds2 = (float)Math.sqrt(p2x * p2x + p2y * p2y - (disttoline * disttoline)) / distbetween < 1;
            
            if(Math.abs(disttoline) <= (thickness / 2.0f) && bounds1 && bounds2) {
                return true;
            }
        }

        return false;
    }

    public float[] getDistAlongAndFrom(float x, float y){
        int mpi = -1;
        float dist1 = Float.POSITIVE_INFINITY;

        ArrayList<Integer> points = sampleAt(x, y);
       
        for (int i : points){
            float dist = mp.get(i).distTo(x, y);

            if(dist < dist1){
                mpi = i;
                dist1 = dist;
            }
        }

        MapPoint mp1 = mp.get(mpi); 
        MapPoint mp2;
        if(mpi == 0) mp2 = mp.get(mpi + 1);
        else if (mpi == mp.size() - 1) mp2 = mp.get(mpi - 1);
        else mp2 = mp.get(mpi + 1).distTo(mp1) < mp.get(mpi - 1).distTo(mp1) ? mp.get(mpi + 1) : mp.get(mpi - 1); 

        float p1x = mp1.x - x;
        float p1y = mp1.y - y;
        float p2x = mp2.x - x;
        float p2y = mp2.y - y;
    
        float distbetween = (float)Math.sqrt((p2x - p1x) * (p2x - p1x) + (p2y - p1y) * (p2y - p1y));    
        float disttoline = Math.abs((p2x * p1y) - (p2y * p1x)) / distbetween;

        float distalongline = (1.0f / (float)mp.size()) * (float)Math.sqrt(p1x * p1x + p1y * p1y - (disttoline * disttoline)) / distbetween;

        //System.out.println((1.0f / (float)mp.size()) + ", " + distalongline + ", ");

        return new float[]{((float)mpi / (float)mp.size()) + distalongline, disttoline};
    }

    public Pos2d getHead() {
        float x = mp.get(0).x;
        float y = mp.get(0).y;
        float angle = (float)Math.atan2(mp.get(1).y - y, mp.get(1).x - x);

        return new Pos2d(x, y, angle);
    }

    public ArrayList<Integer> sampleAt(float x, float y){
        int i_x = (int)(((x + (TreeBounds/2)) / TreeBounds) * TreeDiv);
        int i_y = (int)(((y + (TreeBounds/2)) / TreeBounds) * TreeDiv);

        ArrayList<Integer> nodes = new ArrayList<Integer>();

        nodes.addAll(tree[i_y][i_x].mappoints);

        if(i_x != TreeDiv && i_y < TreeDiv - 1) nodes.addAll(tree[i_y + 1][i_x + 1].mappoints);
        if(                  i_y < TreeDiv - 1) nodes.addAll(tree[i_y + 1][i_x    ].mappoints);
        if(i_x  >       0 && i_y < TreeDiv - 1) nodes.addAll(tree[i_y + 1][i_x - 1].mappoints);
        if(i_x != TreeDiv                     ) nodes.addAll(tree[i_y    ][i_x + 1].mappoints);
        if(i_x  >       0                     ) nodes.addAll(tree[i_y    ][i_x - 1].mappoints);
        if(i_x != TreeDiv && i_y >           0) nodes.addAll(tree[i_y - 1][i_x + 1].mappoints);
        if(                  i_y >           0) nodes.addAll(tree[i_y - 1][i_x    ].mappoints);
        if(i_x  >       0 && i_y >           0) nodes.addAll(tree[i_y - 1][i_x - 1].mappoints);

        return nodes;
    }

    public void initTree(){
        for(int i = 0; i < TreeDiv; i++){
            for(int j = 0; j < TreeDiv; j++){
                tree[i][j] = new treeNode();
            }
        }

        for(int i = 0; i < mp.size(); i++){
            int x = (int)(((mp.get(i).x + (TreeBounds/2)) / TreeBounds) * TreeDiv);
            int y = (int)(((mp.get(i).y + (TreeBounds/2)) / TreeBounds) * TreeDiv);

            tree[y][x].mappoints.add(i);
        }

        for(int i = 0; i < TreeDiv; i++){
            for(int j = 0; j < TreeDiv; j++){
                System.out.println(tree[j][i].mappoints);
            }
        }
    }

    public static Map fromOBJ(String fp, float thickness){
        Map m = new Map(thickness);

        try {
            File file = new File(fp);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()) {
                String str = scanner.nextLine();
                String[] st = str.split(" ");

                if(st[0].startsWith("v")){
                    m.mp.add(new MapPoint(Float.parseFloat(st[1]), Float.parseFloat(st[3]))); 
                }
            }
            scanner.close();
        } catch (Exception e) {
        }

        m.initTree();
        return m;
    }

    void Render(){
        Renderer.instance.ClearLines();

        for(int i = 0; i < mp.size() - 1; i++){
            Renderer.instance.AddLine(mp.get(i).x, mp.get(i).y, mp.get(i+1).x, mp.get(i+1).y, thickness, new Color(255, 255, 255));
        }
    }
}
