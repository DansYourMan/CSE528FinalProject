import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JPanel;

class Bullet {
    private Point position;
    private final int speed = 5; // Speed at which the bullet moves

    public Bullet(Point startPos) {
        // Initialize the bullet's position
        this.position = new Point(startPos);
    }

    public void move() {
        // Move the bullet towards the top of the screen
        position.translate(0, -speed);
    }

    public boolean isOutOfScreen() {
        // Check if the bullet is out of the screen
        return position.y < 0;
    }

    public Point getPosition() {
        return position;
    }

    public void draw(Graphics g, JPanel panel) {
        // Draw a bullet on the screen (represented as a small oval)
        g.setColor(Color.BLACK);
        g.fillOval(position.x * Board.TILE_SIZE + (Board.TILE_SIZE / 2) - 2, position.y * Board.TILE_SIZE, 4, 4);
    }
}
