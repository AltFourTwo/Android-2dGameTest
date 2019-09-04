package com.example.alex.a2dgametest;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {

    // test vars
    //private int borders = 0;
    private boolean godmode = true;



    public static final int WIDTH = 640;
    public static final int HEIGHT = 360;
    public static final int MOVESPEED = -5;
    private long smokeStartTime;
    private long missileStartTime;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topborder;
    private ArrayList<BotBorder> botborder;
    private Random rand = new Random();
    private int minBorderHeight;
    private int maxBorderHeight;
    private boolean topDown = true;
    private boolean botDown = true;
    private boolean newGameCreated;

    // increase to slow down difficulty progression, decrease to speed up difficulty progression
    private int progressDenominator = 20;

    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean dissapear;
    private boolean started;
    private int best;


    public GamePanel(Context context) {
        super(context);

        getHolder().addCallback(this);

        Constants.CURRENT_CONTEXT = context;

        setFocusable(true);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Constants.INIT_TIME = System.currentTimeMillis();

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.gamescreenexamplehalf));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 64, 26, 3);
        smoke = new ArrayList<Smokepuff>();
        missiles = new ArrayList<Missile>();
        topborder = new ArrayList<TopBorder>();
        botborder = new ArrayList<BotBorder>();

        smokeStartTime = System.nanoTime();
        missileStartTime = System.nanoTime();

        thread = new MainThread(getHolder(), this);

        // we can safely start the game loop
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        int counter = 0;
        while (retry && counter < 1000) {
            counter++;
            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;
            } catch(Exception e) {e.printStackTrace();}
        }
    }

    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if(!player.getPlaying() && newGameCreated && reset) {
                player.setPlaying(true);
                player.setUp(true);
            }
            if(player.getPlaying()) {
                if(!started) started = true;
                reset = false;
                player.setUp(true);
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            player.setUp(false);
            return true;
        }



        return true;
    }

    public void update() {
        if (player.getPlaying()) {

            if(botborder.isEmpty() || topborder.isEmpty()) {
                player.setPlaying(false);
                return;
            }

            bg.update();
            player.update();
            if(best < player.getScore()) best = player.getScore();


            // calculate the threshold of height the border can have based on the score
            // max and min border heart are updated, and the border switched direction when either
            // max or min is met
            maxBorderHeight = 30+player.getScore()/progressDenominator;
            // cap max border height so that borders can only take up a total of 1/2 the screen
            if(maxBorderHeight > HEIGHT/4) maxBorderHeight = HEIGHT/4;
            minBorderHeight = 5+player.getScore()/progressDenominator;


            if(!godmode) {
                // check top border collision
                for (int i = 0; i < topborder.size(); i++) {
                    if (collision(player, topborder.get(i))) player.setPlaying(false);
                }

                // check bottom border collision
                for (int i = 0; i < botborder.size(); i++) {
                    if (collision(player, botborder.get(i))) player.setPlaying(false);
                }
            }

            // update top border
            this.updateTopBorder();

            // update bottom border
            this.updateBottomBorder();

            // add missiles on timer
            long missileElapsed = (System.nanoTime() - missileStartTime)/1000000;
            if (missileElapsed > (2000 - player.getScore()/4)) {
                // first missile always goes down the middle
                if(missiles.size() == 0) {
                    missiles.add(new Missile(
                            BitmapFactory.decodeResource(getResources(), R.drawable.missile),
                            WIDTH + 10,
                            HEIGHT/2,
                            45,
                            15,
                            player.getScore(),
                            13));
                }
                // next missiles are random
                else {
                    missiles.add(new Missile(
                            BitmapFactory.decodeResource(getResources(), R.drawable.missile),
                            WIDTH + 10,
                            rand.nextInt(HEIGHT-15-maxBorderHeight),
                            45,
                            15,
                            player.getScore(),
                            13));
                }
                //reset timer
                missileStartTime = System.nanoTime();
            }

            // add smoke puffs on timer
            long elapsed = (System.nanoTime() - smokeStartTime)/1000000;
            if (elapsed > 120) {
                smoke.add(new Smokepuff(player.getX(), player.getY()+10));
                smokeStartTime = System.nanoTime();
            }

            for (int i = 0; i < smoke.size(); i++) {smoke.get(i).update();}
            if (smoke.size()-1 > 5) smoke.remove(0);

            for (int i = 0; i < missiles.size(); i++) {
                missiles.get(i).update();
                if(collision(missiles.get(i),player)) {
                    missiles.remove(i);
                    if(!godmode) player.setPlaying(false);
                    if(godmode) player.addScore(50);
                    break;
                }
                if(missiles.get(i).getX() < -100) {
                    missiles.remove(i);
                    break;
                }
            }
        }
        else {
            player.resetDY();
            if(!reset) {
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                dissapear = true;
                explosion = new Explosion(
                        BitmapFactory.decodeResource(getResources(), R.drawable.explosion),
                        player.getX(),
                        player.getY()-30,
                        100,
                        100,
                        25);
            }

            explosion.update();
            long resetElapsed = (System.nanoTime() - startReset)/1000000;

            if(!newGameCreated && resetElapsed > 2500) {
                newGame();
            }
        }
    }

    public boolean collision(GameObject a, GameObject b) {
        if(Rect.intersects(a.getRectangle(), b.getRectangle())) {return true;}
        return false;
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        final float scaleFactorX = Constants.SCREEN_WIDTH/(WIDTH*1.f);
        final float scaleFactorY = Constants.SCREEN_HEIGHT/(HEIGHT*1.f);
        if (canvas != null ) {
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            if(!dissapear) player.draw(canvas);
            for(Smokepuff sp:smoke) {sp.draw(canvas);}
            for(Missile mss:missiles) {mss.draw(canvas);}

            // draw topborder
            for(TopBorder tb:topborder) {tb.draw(canvas);}
            // draw botborder
            for(BotBorder bb:botborder) {bb.draw(canvas);}

            // draw explosion when player dies
            if(started) {explosion.draw(canvas);}




            drawText(canvas);
            canvas.restoreToCount(savedState);

        }
        //manager.draw(canvas);
    }

    public void updateTopBorder() {
        //every 50 points , insert randomly placed top blocks that break the patten
        if(player.getScore()%50 == 0) {
            topborder.add(new TopBorder(
                    BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    topborder.get(topborder.size()-1).getX()-21,
                    0,
                    (rand.nextInt(maxBorderHeight)+1)));
        }

        //update top border
        for (int i = 0; i < topborder.size(); i++) {
            topborder.get(i).update();
            if(topborder.get(i).getX() < -21) {
                //remove element of ArrayList, replace it by adding a new one
                topborder.remove(i);

                // calculate topdown which determines the direction the border is moving
                // (up or down)
                if(topborder.get(topborder.size()-1).getHeight() >= maxBorderHeight) {
                    topDown = false;
                }
                if(topborder.get(topborder.size()-1).getHeight() <= minBorderHeight) {
                    topDown = true;
                }

                if(topDown) {
                    topborder.add(new TopBorder(
                            BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            topborder.get(topborder.size()-1).getX()+21,
                            0,
                            topborder.get(topborder.size()-1).getHeight()+1));
                }
                // new border added will have smaller height
                else {
                    topborder.add(new TopBorder(
                            BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            topborder.get(topborder.size()-1).getX()+21,
                            0,
                            topborder.get(topborder.size()-1).getHeight()-1));
                }
            }
        }
    }

    public void updateBottomBorder() {
        // every 40 points, insert randomly placed bottom blocks that break pattern
        if(player.getScore()%40 == 0) {
            botborder.add(new BotBorder(
                    BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    botborder.get(botborder.size()-1).getX()+21,
                    (rand.nextInt(maxBorderHeight)+HEIGHT-maxBorderHeight)));
        }

        //update bottom border
        for (int i = 0; i < botborder.size(); i++) {
            botborder.get(i).update();
            // if border is moving off screen, remove it and add a corresponding new one
            if(botborder.get(i).getX() < -21) {
                botborder.remove(i);


                //System.out.println("minBorderHeight is " + minBorderHeight);
                //System.out.println("maxBorderHeight is " + maxBorderHeight);
                // determine if border will be moving up or down
                if(botborder.get(botborder.size()-1).getY() <= HEIGHT - maxBorderHeight) {
                    botDown = true;
                }
                if(botborder.get(botborder.size()-1).getY() >= HEIGHT - minBorderHeight) {
                    botDown = false;
                }

                if(botDown) {
                    botborder.add(new BotBorder(
                            BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botborder.get(botborder.size()-1).getX()+21,
                            botborder.get(botborder.size()-1).getY() + 1));
                }
                // new border added will have smaller height
                else {
                    botborder.add(new BotBorder(
                            BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botborder.get(botborder.size()-1).getX()+21,
                            botborder.get(botborder.size()-1).getY() - 1));
                }
            }
        }
    }

    public void newGame() {

        if(player.getScore() > best) {
            best = player.getScore();
        }

        dissapear = false;
        player.resetScore();
        player.setY(HEIGHT/2);
        player.resetDY();

        botborder.clear();
        topborder.clear();

        missiles.clear();
        smoke.clear();

        minBorderHeight = 5;
        maxBorderHeight = 30;

        // create initial borders
        for(int i = 0; i*21 < WIDTH+40; i++) {
            // first top border created upon a new game
            if(i==0) {
                topborder.add(new TopBorder(
                        BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*21,
                        0,
                        10));

                botborder.add(new BotBorder(
                        BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*21,
                        HEIGHT - minBorderHeight));
            }
            else {
                topborder.add(new TopBorder(
                        BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*21,
                        0,
                        topborder.get(i-1).getHeight()+1));

                botborder.add(new BotBorder(
                        BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*21,
                        botborder.get(i-1).getY()-1));
            }
        }

        newGameCreated = true;
    }

    public void drawText(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("DISTANCE: " + (player.getScore()), 10, HEIGHT -10 , paint);
        canvas.drawText("BEST: " + best, WIDTH - 215, HEIGHT - 10, paint);

        if(!player.getPlaying() && newGameCreated && reset) {
            Paint paint1 = new Paint();
            paint1.setTextSize(40);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("TAP TO START", WIDTH/2 - 50, HEIGHT/2 - 20 , paint1);

            paint1.setTextSize(20);
            canvas.drawText("PRESS AND HOLD TO GO UP", WIDTH/2 - 50, HEIGHT/2 + 20 , paint1);
            canvas.drawText("RELEASE TO GO DOWN", WIDTH/2 - 50, HEIGHT/2 + 40 , paint1);
        }
    }
}
