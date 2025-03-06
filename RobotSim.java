import Graphics.Renderer;
import java.awt.Color;
import java.util.Random;

class Pos2d {
    float x, y, angle;

    public Pos2d (float x, float y, float angle){
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public Pos2d(Pos2d p){
        this.x = p.x;
        this.y = p.y;
        this.angle = p.angle;
    }
};

class MotorInstruction{
    byte l,r;

    static Random rand;

    public MotorInstruction(byte l, byte r) {
        this.l = l;
        this.r = r;

        if (rand == null) rand = new Random(System.currentTimeMillis());
    }

    public static MotorInstruction[][][][][][] RandomFrom(MotorInstruction[][][][][][] mi, int chance, int adjamount){
        MotorInstruction[][][][][][] nmi = new MotorInstruction[mi.length][mi[0].length][mi[0][0].length][mi[0][0][0].length][mi[0][0][0][0].length][mi[0][0][0][0][0].length];
        
        for(int i = 0; i < mi.length; i++){
            for(int j = 0; j < mi[i].length; j++){
                for(int k = 0; k < mi[i][j].length; k++){
                    for(int l = 0; l < mi[i][j][k].length; l++){
                        for(int n = 0; n < mi[i][j][k][l].length; n++){
                            for(int o = 0; o < mi[i][j][k][l][n].length; o++){
                                int adjx = (rand.nextInt(chance) == 0) ? (rand.nextBoolean() ? 1 : -1) * rand.nextInt(adjamount) : 0;
                                int adjy = (rand.nextInt(chance) == 0) ? (rand.nextBoolean() ? 1 : -1) * rand.nextInt(adjamount) : 0;

                                nmi[i][j][k][l][n][o] = new MotorInstruction((byte)Math.min(Math.max(mi[i][j][k][l][n][o].l + adjx, -90), 90), (byte)Math.min(Math.max(mi[i][j][k][l][n][o].r + adjy, -90), 90));
                            }
                        }
                    }
                }
            }
        }
        
        return nmi;
    }
};

class SensorReturn {
    int left, center, right;

    public SensorReturn(int l, int c, int r){
        left = l;
        center = c;
        right = r;
    }    
};

class lookPointer {
    int la_l, la_c, la_r;
    int lb_l, lb_c, lb_r;

    public static lookPointer getLookPointer(SensorReturn[] buffer, int bufferptr){
        lookPointer lp = new lookPointer();

        short la_left = 0,
            la_right = 0, 
            la_center = 0;

        short lb_left = 0,
            lb_center = 0,
            lb_right = 0;

        for (int i = bufferptr; i < bufferptr + (buffer.length / 2); i++){
            if(buffer[i] == null) continue;

            int multiplier = ((bufferptr + (buffer.length / 2)) - (i + 1));

            la_left += buffer[i].left * multiplier;
            la_right += buffer[i].right * multiplier;
            la_center += buffer[i].center * multiplier;

            multiplier = (i - (bufferptr + 1));
            lb_left += buffer[i].left * multiplier;
            lb_right += buffer[i].right * multiplier;
            lb_center += buffer[i].center * multiplier;
        }

        lp.la_l = la_left >> Integer.numberOfTrailingZeros(buffer.length / 2) - 1;
        lp.la_c = la_center >> Integer.numberOfTrailingZeros(buffer.length / 2) - 1;
        lp.la_r = la_right >> Integer.numberOfTrailingZeros(buffer.length / 2) - 1;

        lp.lb_l = lb_left >> Integer.numberOfTrailingZeros(buffer.length / 2) - 1;
        lp.lb_c = lb_center >> Integer.numberOfTrailingZeros(buffer.length / 2) - 1;
        lp.lb_r = lb_right >> Integer.numberOfTrailingZeros(buffer.length / 2) - 1; 

        //System.out.println("(" + lp.la_l + ", " + lp.la_c + ", " + lp.la_r + ") ");
        return lp;
    }
};


public class RobotSim{
    Pos2d pos; 
    Map map;
    final static float maxSpeed         = 0.600f;
    final static float trackWidth       = 0.107f;
    final static float sensoroffset     = 0.036f;
    final static float sensorWidth      = 0.046f;
    final static int BUFFER_LEN         = 64;

    SensorReturn[] buffer = new SensorReturn[BUFFER_LEN * 2];
    int bufferptr;
    Pos2d[] lastPos = new Pos2d[BUFFER_LEN * 2];

    MotorInstruction[][][][][][] mi;

    public RobotSim(Map m, MotorInstruction[][][][][][] mi){
        this.map = m;

        if (mi != null) this.mi = mi;

        Pos2d startpos = m.getHead();
        startpos.angle += (new Random(System.currentTimeMillis()).nextFloat() - 0.5f) * 0.05f; /* add a minor angle offset */

        pos = startpos;
    }

    Pos2d[] getSensorPos(Pos2d p){
        Pos2d center = new Pos2d((float)Math.cos(p.angle) * sensoroffset + p.x, (float)Math.sin(p.angle) * sensoroffset + p.y, p.angle);
        Pos2d offset = new Pos2d((float)Math.cos(p.angle + (Math.PI/2.0f)) * (sensorWidth / 2.0f), (float)Math.sin(p.angle + (Math.PI/2.0f)) * (sensorWidth / 2.0f), 0.0f);

        Pos2d left = new Pos2d(center.x + offset.x, center.y + offset.y, 0);
        Pos2d right = new Pos2d(center.x - offset.x, center.y - offset.y, 0);

        return new Pos2d[]{left, center, right};
    }

    SensorReturn getSensor(){
        Pos2d[] sensorpos = getSensorPos(pos);
        return new SensorReturn(
            map.sampleMap(sensorpos[0].x, sensorpos[0].y) ? 1 : 0, 
            map.sampleMap(sensorpos[1].x, sensorpos[1].y) ? 1 : 0, 
            map.sampleMap(sensorpos[2].x, sensorpos[2].y) ? 1 : 0
        );
    }

    void sensorWrite(SensorReturn sr){
        bufferptr++;
        bufferptr = bufferptr % BUFFER_LEN;
        
        SensorReturn sensor = getSensor();
        buffer[bufferptr] = sensor;
        buffer[bufferptr + BUFFER_LEN] = sensor;

        Pos2d newpos = new Pos2d(pos);
        lastPos[bufferptr] = newpos;
        lastPos[bufferptr + BUFFER_LEN] = newpos;
    }

    RobotSim(MotorInstruction[][][][][][] mi){
        this.mi = mi;
        pos = new Pos2d(0,0,0);
    }

    /* returns m/s from command */
    float commandToSpeed(byte cmd){
        return (cmd/90.0f) * maxSpeed;
    }

    /* move by 1 ms */
    public void Move(MotorInstruction m){
        float speed_l = commandToSpeed(m.l),
              speed_r = commandToSpeed(m.r);
        
        float speed_w = (speed_l - speed_r) / trackWidth;
        float speed_v = (speed_r + speed_l) / 2.0f;

        pos.angle += speed_w * 0.0005f;
        pos.x += speed_v * (float)Math.cos(pos.angle) * 0.001f;
        pos.y += speed_v * (float)Math.sin(pos.angle) * 0.001f;
        pos.angle += speed_w * 0.0005f;
    }

    MotorInstruction milookup(){
        lookPointer lp = lookPointer.getLookPointer(buffer, bufferptr);

        int p0 = (lp.la_l * mi.length               ) / BUFFER_LEN;
        int p1 = (lp.la_c * mi[0].length            ) / BUFFER_LEN;
        int p2 = (lp.la_r * mi[0][0].length         ) / BUFFER_LEN;
        int p3 = (lp.lb_l * mi[0][0][0].length      ) / BUFFER_LEN;
        int p4 = (lp.lb_c * mi[0][0][0][0].length   ) / BUFFER_LEN;
        int p5 = (lp.lb_r * mi[0][0][0][0][0].length) / BUFFER_LEN;
        
        return mi[p0][p1][p2][p3][p4][p5];
    }

    public void Update(){
        MotorInstruction instruction = milookup();
        for(int i = 0; i < 20; i++){
            Move(instruction);
            sensorWrite(getSensor());
        }
    }

    public void Render(){
        Renderer.instance.ClearPoints();

        for(int i = bufferptr; i < bufferptr + BUFFER_LEN; i++){
            if(lastPos[i] == null) continue;
            Pos2d[] ps = getSensorPos(lastPos[i]);

            Renderer.instance.AddPoint(ps[0].x, ps[0].y, 0.002f, (buffer[i].left > 0) ? new Color(0xFF00FFFF) : new Color(0xAAAAAAAA));
            Renderer.instance.AddPoint(ps[1].x, ps[1].y, 0.002f, (buffer[i].center > 0) ? new Color(0xFF00FFFF) : new Color(0xAAAAAAAA));
            Renderer.instance.AddPoint(ps[2].x, ps[2].y, 0.002f, (buffer[i].right > 0) ? new Color(0xFF00FFFF) : new Color(0xAAAAAAAA));
        } 
    }
}
