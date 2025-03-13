import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class simulationReturn {
    public float time, dist, rate, smoothness;

    public simulationReturn(float time, float dist, float smoothness){
        this.time = time;
        this.dist = dist;
        this.rate = dist > 0.99f ? 1 : 0;
        this.smoothness = smoothness;
    }

    public simulationReturn(float time, float dist, float rate, float smoothness){
        this.time = time;
        this.dist = dist;
        this.rate = rate;
        this.smoothness = smoothness;
    }
}


public class BatchTrainer {
    public static final int BATCH_SIZE         = 300;
    public static final int ADJ_AMOUNT         = 30; /* amount of adjustment happening */
    public static final int STOCHASTIC_CHANCE  = 4; /* one in whatever */ 
    
    public static final float MAX_OFFSET       = 0.025f; /* can only be 5cm away from line before it fails */

    private static int trainings = 0;

    static volatile MotorInstruction[][][][][][] current;

    public static void save(String fp){
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(fp));

            writer.append("uint8_t lookuptable[4][4][4][4][4][8] = {");
            for(int i = 0; i < current.length; i++){
                writer.append("{");
                for(int j = 0; j < current[i].length; j++){
                    writer.append("{");
                    for(int k = 0; k < current[i][j].length; k++){
                        writer.append("{");
                        for(int l = 0; l < current[i][j][k].length; l++){
                            writer.append("{");
                            for(int n = 0; n < current[i][j][k][l].length; n++){
                                writer.append("{");
                                for(int o = 0; o < current[i][j][k][l][n].length; o++){
                                    writer.append((int)current[i][j][k][l][n][o].l + ", " + current[i][j][k][l][n][o].r + ", ");
                                }
                                writer.append("},");
                            }
                            writer.append("},");
                        }
                        writer.append("},");
                    }
                    writer.append("},");
                }
                writer.append("},");
            }
            writer.append("};");

            writer.close();
        } 
        catch(Exception e) {

        }
    }

    public static void load(String fp){
        try{
            MotorInstruction[][][][][][] mi = new MotorInstruction[4][4][4][4][4][4];
            Scanner fscan = new Scanner(new File(fp));
            String filestr = fscan.nextLine().substring(40)
                .replace("{", "")
                .replace(",", "")
                .replace("}", "")
                .replace(";", "");
            Scanner sc = new Scanner(filestr);
            current = getDefault();

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
            current = MotorInstruction.RandomFrom(mi, 100000, 0);
        } 
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    static MotorInstruction[][][][][][] getDefault(){
        MotorInstruction[][][][][][] mi = new MotorInstruction[4][4][4][4][4][4];

        new MotorInstruction((byte)0, (byte)0);

        for(int i = 0; i < mi.length; i++){
            for(int j = 0; j < mi[i].length; j++){
                for(int k = 0; k < mi[i][j].length; k++){
                    for(int l = 0; l < mi[i][j][k].length; l++){
                        for(int n = 0; n < mi[i][j][k][l].length; n++){
                            for(int o = 0; o < mi[i][j][k][l][n].length; o++){
                                if(j >= 2 && i < 2 && k < 2)
                                    mi[i][j][k][l][n][o] = new MotorInstruction((byte)90, (byte)90);
                                else if (i >= 2)
                                    mi[i][j][k][l][n][o] = new MotorInstruction((byte)30, (byte)90);
                                else if (k >= 2)
                                    mi[i][j][k][l][n][o] = new MotorInstruction((byte)90, (byte)30);
                                
                                mi[i][j][k][l][n][o] = new MotorInstruction((byte)60, (byte)60);
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
        float dist = 0, offset = 0, time = 0, maxdist = 0;

        boolean halfway = false;

        while(true){
            rb.Update();
            
            time += 0.01f;
            
            float[] d = map.getDistAlongAndFrom(rb.pos.x, rb.pos.y);
            dist = d[0];
            offset = d[1];
            maxdist = Math.max(dist, maxdist);

            if(offset > MAX_OFFSET || dist > 0.99f || dist < maxdist - 0.05f || time > 15.0f){
                if(halfway || dist < 0.99f) break;
                else return new simulationReturn(15.0f, 0.001f, (float)(1.0 / (rb.getAverage() + 0.01)));
            }

            if (dist > 0.5f) halfway = true;
        }

        return new simulationReturn(time, Math.min(dist,0.99f), (float)(1.0 / (rb.getAverage() + 0.01)));
    }
    
    public static void runSimulationRendered(Map map, MotorInstruction[][][][][][] mi){
        RobotSim rb = new RobotSim(map, MotorInstruction.RandomFrom(mi, 1000000,0));
        float dist = 0, offset = 0, maxdist = 0;

        while(true){
            rb.Update();
           
            float[] d = map.getDistAlongAndFrom(rb.pos.x, rb.pos.y);
            
            dist = d[0];
            offset = d[1];
            maxdist = Math.max(dist, maxdist);

            if(offset > MAX_OFFSET || dist > 0.99f || dist < maxdist - 0.05f) break;

            rb.Render();
            map.Render();
            
            try{Thread.sleep(10);} catch(Exception e){};
        }
    }

    public static float getScore(simulationReturn sr){
        if(sr.rate < 0.5f)
            return sr.dist * 12.5f + sr.rate * 5.0f;
        else
            return sr.dist * 12.5f + (sr.dist/sr.time) * 25.0f + sr.rate + (1.5f * sr.smoothness);   
    }

    public static simulationReturn runBatchTest(Map map, MotorInstruction[][][][][][] mi, int trials){
        float total_dist = 0, total_time = 0, total_smoothness = 0;
        int total_finished = 0;

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
                total_smoothness += sr.smoothness;
                total_finished += sr.dist >= 0.98f ? 1 : 0;
            } 
        } catch (Exception ex) {
    
        }

        //System.out.println(total_dist + ", " + total_time +  ", " + total_smoothness);
        return new simulationReturn(total_time / (float)trials, total_dist / (float)trials, (float)total_finished / (float)trials, total_smoothness / (float)trials);   
    }

    public static MotorInstruction[][][][][][] getInstructionArray(Map map){
        if (current == null)
            current = getDefault();
       
        Thread RendThread = new Thread(() -> runSimulationRendered(map, current));

        while(true){
            float currentDist = 0, currentTime = 0, currentRate = 0, currentSmooth = 0, currentScore = getScore(runBatchTest(map, current, 40));
           
            if(!RendThread.isAlive()){
                try{RendThread.join();} catch(Exception e){}
                RendThread = new Thread(() -> runSimulationRendered(map, current));
                RendThread.start();
            }
            
            MotorInstruction[][][][][][] old = current;
            for(int i = 0; i < BATCH_SIZE; i++){
                MotorInstruction[][][][][][] mi = MotorInstruction.RandomFrom(old, STOCHASTIC_CHANCE, ADJ_AMOUNT);
                simulationReturn test = runBatchTest(map, mi, 40);
                float score = getScore(test);
            
                if(score >= currentScore) {
                    current = mi;
                    currentDist = test.dist;
                    currentTime = test.time;
                    currentRate = test.rate;
                    currentSmooth = test.smoothness;
                    currentScore = score;
                }
            }


            if(trainings % 10 == 0) save("lookup.c");
            trainings++;

            System.out.println("iterations: " + trainings + ", dist: " + currentDist + ", time: " + currentTime + ", rate: " + currentRate + ", smooth: " + currentSmooth + " score: " + currentScore);
           
            if(currentDist > 0.99f)
                if (currentRate > 0.95f && currentTime < 8.0f) break;
        }

        while(RendThread.isAlive());
        try{RendThread.join();} catch(Exception e){}
        return current;
    }
}
