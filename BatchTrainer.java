import java.awt.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class simulationReturn {
    public float time, dist;

    public simulationReturn(float time, float dist){
        this.time = time;
        this.dist = dist;
    }
}


public class BatchTrainer {
    public static final int BATCH_SIZE         = 200;
    public static final int ADJ_AMOUNT         = 30; /* amount of adjustment happening */
    public static final int STOCHASTIC_CHANCE  = 5; /* one in whatever */ 
    
    public static final float MAX_OFFSET       = 0.025f; /* can only be 5cm away from line before it fails */

    private static int trainings = 0;

    static volatile MotorInstruction[][][][][][] current;

    public static void save(String fp){
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(fp));

            for(int i = 0; i < current.length; i++){
                for(int j = 0; j < current[i].length; j++){
                    for(int k = 0; k < current[i][j].length; k++){
                        for(int l = 0; l < current[i][j][k].length; l++){
                            for(int n = 0; n < current[i][j][k][l].length; n++){
                                for(int o = 0; o < current[i][j][k][l][n].length; o++){
                                    writer.append(current[i][j][k][l][n][o].l + " " + current[i][j][k][l][n][o].r + " ");
                                }
                            }
                        }
                    }
                }
            }
            writer.close();
        } 
        catch(Exception e) {

        }
    }

    public static void load(String fp){
        try{
            MotorInstruction[][][][][][] mi = new MotorInstruction[5][5][5][5][5][5];
            Scanner sc = new Scanner(new File(fp));

            for(int i = 0; i < current.length; i++){
                for(int j = 0; j < current[i].length; j++){
                    for(int k = 0; k < current[i][j].length; k++){
                        for(int l = 0; l < current[i][j][k].length; l++){
                            for(int n = 0; n < current[i][j][k][l].length; n++){
                                for(int o = 0; o < mi[i][j][k][l][n].length; o++){
                                    mi[i][j][k][l][n][o] = new MotorInstruction((byte)sc.nextInt(), (byte)sc.nextInt());
                                }
                            }
                        }
                    }
                }
            }
            sc.close();
            current = mi;
        } 
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    static MotorInstruction[][][][][][] getDefault(){
        MotorInstruction[][][][][][] mi = new MotorInstruction[5][5][5][5][5][5];

        new MotorInstruction((byte)0, (byte)0);

        for(int i = 0; i < mi.length; i++){
            for(int j = 0; j < mi[i].length; j++){
                for(int k = 0; k < mi[i][j].length; k++){
                    for(int l = 0; l < mi[i][j][k].length; l++){
                        for(int n = 0; n < mi[i][j][k][l].length; n++){
                            for(int o = 0; o < mi[i][j][k][l][n].length; o++){
                                mi[i][j][k][l][n][o] = new MotorInstruction((byte)90, (byte)90);
                            }
                        }
                    }
                }
            }
        }
        return mi;
    }

    public static simulationReturn runSimulation(Map map, MotorInstruction[][][][][][] mi){
        RobotSim rb = new RobotSim(map, mi);
        float dist = 0, offset = 0, time = 0;

        while(true){
            rb.Update();
            
            time += 0.02f;
            
            float[] d = map.getDistAlongAndFrom(rb.pos.x, rb.pos.y);
            dist = d[0];
            offset = d[1];

            if(offset > MAX_OFFSET || dist > 0.99f) break;
        }

        return new simulationReturn(time, dist);
    }
    
    public static void runSimulationRendered(Map map, MotorInstruction[][][][][][] mi){
        RobotSim rb = new RobotSim(map, MotorInstruction.RandomFrom(mi, 1000000,0));
        float dist = 0, offset = 0;

        while(true){
            rb.Update();
            
            float[] d = map.getDistAlongAndFrom(rb.pos.x, rb.pos.y);
            dist = d[0];
            offset = d[1];

            if(offset > MAX_OFFSET || dist > 0.99f) break;

            rb.Render();
            map.Render();
            
            try{Thread.sleep(50);} catch(Exception e){};
        }
    }

    public static simulationReturn runBatchTest(Map map, MotorInstruction[][][][][][] mi, int trials){
        float total_dist = 0, total_time = 0;

        ExecutorService EXEC = Executors.newCachedThreadPool();
        ArrayList<Callable<simulationReturn>> tasks = new ArrayList<Callable<simulationReturn>>();

        for(int i = 0; i < trials; i++){            
            tasks.add(new Callable<simulationReturn>() {
                @Override
                public simulationReturn call() throws Exception {return runSimulation(map, mi);}
            });
        }

        try {
            ArrayList<Future<simulationReturn>> results = (ArrayList<Future<simulationReturn>>)EXEC.invokeAll(tasks); 
        
            for(Future<simulationReturn> fr : results){    
                simulationReturn sr;
                sr = fr.get();
                total_dist += sr.dist;
                total_time += sr.time;
            } 
        } catch (Exception ex) {
    
        }
        return new simulationReturn(total_time / (float)trials, total_dist / (float)trials);   
    }

    public static MotorInstruction[][][][][][] getInstructionArray(Map map){
        current = getDefault();
       
        Thread RendThread = new Thread(() -> runSimulationRendered(map, current));

        int successes = 0;

        while(true){
            float currentDist;
            currentDist = runBatchTest(map, current, 40).dist;
            if(!RendThread.isAlive()){
                try{RendThread.join();} catch(Exception e){}
                RendThread = new Thread(() -> runSimulationRendered(map, current));
                RendThread.start();
            }
            
            MotorInstruction[][][][][][] old = current;
            for(int i = 0; i < BATCH_SIZE; i++){
                MotorInstruction[][][][][][] mi = MotorInstruction.RandomFrom(old, STOCHASTIC_CHANCE, ADJ_AMOUNT);
                float dist = runBatchTest(map, mi, 40).dist;
            
                if(dist >= currentDist) {
                    current = mi;
                    currentDist = dist;
                }
            }

            trainings++;

            float rate = currentDist > 0.99f ? runBatchTest(map, current, 100).dist : 0.0f;
            System.out.println("iterations: " + trainings + ", dist: " + currentDist + ", rate: " + rate);
           
            if(currentDist > 0.99f)
                if (rate > 0.95f) break;
        }

        System.out.println("Finished Initial Honing");
        
        while(true){
            simulationReturn currentRun = runSimulation(map, current);
            if(!RendThread.isAlive()){
                try{RendThread.join();} catch(Exception e){}
                RendThread = new Thread(() -> runSimulationRendered(map, current));
                RendThread.start();
            }
            System.out.println("iterations: " + trainings + ", time: " + currentRun.time + ", dist: " + currentRun.dist);
            
            MotorInstruction[][][][][][] old = current;
            for(int i = 0; i < BATCH_SIZE * 5; i++){
                MotorInstruction[][][][][][] mi = MotorInstruction.RandomFrom(old, STOCHASTIC_CHANCE * 5, ADJ_AMOUNT);
                simulationReturn sr = runBatchTest(map, mi, 25);
            
                if(sr.dist > 0.96f && sr.time < currentRun.time) {
                    current = mi;
                    currentRun = sr;
                }
            }

            trainings++;
            if(currentRun.time < 1.0f && currentRun.dist > 0.99f) break;
        }
      
        while(RendThread.isAlive());
        try{RendThread.join();} catch(Exception e){}
        return current;
    }
}
