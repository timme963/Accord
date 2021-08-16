package de.uniks.stp.controller.snake.model;


import de.uniks.stp.util.ResourceManager;
import javafx.scene.image.Image;

import java.util.Random;

public class Food {
    private final Image foodPic;
    private int posX;
    private int posY;

    public Food() {
        Random rand = new Random();
        String[] foodList = {"apple", "berry", "cherry", "orange"};
        foodPic = ResourceManager.loadSnakeGameIcon(foodList[rand.nextInt(4)]);
    }

    public Image getFoodPic() {
        return foodPic;
    }

    public int getPosX() {
        return this.posX;
    }

    public Food setPosX(int posX) {
        this.posX = posX;
        return this;
    }

    public int getPosY() {
        return this.posY;
    }

    public Food setPosY(int posY) {
        this.posY = posY;
        return this;
    }
}
