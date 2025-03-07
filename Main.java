import Graphics.Renderer;
import Graphics.Window;
import Graphics.GUI.*;

public class Main {
    public static void main(String[] args){
        Window w = new Window();
        
        new Thread(() -> w.start()).start();

        Map m = Map.fromOBJ("map.obj", 0.025f);

        try{Thread.sleep(1000);} catch(Exception e) {};

        System.out.println(w.gui);

        w.gui.queueButton("save", 0.05f, 0.05f, 0.1f, 0.04f, new ButtonInterface() {
            @Override
            public void onClick() {
                BatchTrainer.save("h.txt");
            }
        });

        w.gui.queueButton("load", 0.05f, 0.11f, 0.1f, 0.04f, new ButtonInterface() {
            @Override
            public void onClick() {
                BatchTrainer.load("h.txt");
            }
        });

        w.gui.applyQueue();
        
        RobotSim rb = new RobotSim(m, BatchTrainer.getInstructionArray(m));
        
        while(true){
            m.Render();
            rb.Render(); 
            rb.Update();

            float[] d = m.getDistAlongAndFrom(rb.pos.x, rb.pos.y);

            System.out.println(d[0] + ", " + d[1]);

            try{Thread.sleep(100);} catch(Exception e) {};
        }
    }
}