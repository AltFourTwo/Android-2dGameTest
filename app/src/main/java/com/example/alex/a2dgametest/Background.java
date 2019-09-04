package com.example.alex.a2dgametest;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Background {

    private Bitmap image;
    private int x, y , dx;

    public Bitmap getImage() {
        return image;
    }

    public Background(Bitmap res) {
        image = res;
        dx = GamePanel.MOVESPEED;
    }

    public void update() {
        x+=dx;
        if (x < -GamePanel.WIDTH) {
            x = 0;
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(image, x, y, null);
        if (x <= 0) {
            canvas.drawBitmap(image, x+ GamePanel.WIDTH, y, null);
        }
        /*if (x - GamePanel.WIDTH <= -GamePanel.WIDTH) {
            canvas.drawBitmap(image, x+(GamePanel.WIDTH*2), y, null);
        }*/
    }
}
