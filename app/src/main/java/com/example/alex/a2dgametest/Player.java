package com.example.alex.a2dgametest;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Player extends GameObject{

    private Bitmap spritesheet;
    private int score;
    private boolean up;
    private boolean playing;
    private Animation animation = new Animation();
    private long startTime;

    public Player(Bitmap res, int w, int h, int numFrames) {
        x = 100;
        y = GamePanel.HEIGHT/2;
        dy = 0;
        score = 0;
        height = h;
        width = w;

        Bitmap[] image = new Bitmap[numFrames];
        spritesheet = res;

        for (int i = 0; i < image.length; i++) {
            image[i] = Bitmap.createBitmap(spritesheet, i*width, 0, width, height);
        }

        animation.setFrames(image);
        animation.setDelay(10);
        startTime = System.nanoTime();
    }

        public void setUp(boolean b) {up = b;}

        public void update() {
            long elapsed = (System.nanoTime() - startTime)/1000000;
            if (elapsed > 100) {
                score++;
                startTime = System.nanoTime();
            }
            animation.update();

            if(up) {
                dy -= 1.0;
            } else {
                dy += 1.0;
            }

            if (dy > 5) dy = 5;
            if (dy < -5) dy = -5;

            y += dy*3;
            if (y > GamePanel.HEIGHT) y = 0;
            if (y < 0) y = GamePanel.HEIGHT - 1;

        }

        public void draw(Canvas canvas) {
            canvas.drawBitmap(animation.getImage(), x, y, null);
        }

        public int getScore() {return score;}
        public void addScore(int pts) {score += pts;}
        public boolean getPlaying() {return playing;}
        public void setPlaying(boolean b) {playing = b;}
        public void resetDY() {dy = 0;}
        public void resetScore() {score = 0;}


}
