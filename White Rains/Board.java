import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.swing.*;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class Board extends JPanel implements ActionListener, KeyListener {

    // Controls the delay between each tick in ms
    private final int DELAY = 25;
    // Controls the delay for square state change in ms
    private final int SQUARE_STATE_CHANGE_DELAY = 2000; // 2 seconds
    // Controls the delay for danger square respawn in ms
    private final int SQUARE_RESPAWN_DELAY = 5000; // 5 seconds
    // Controls the size of the board
    public static final int TILE_SIZE = 50;
    public static final int ROWS = 12;
    public static final int COLUMNS = 19;
    // Number of danger squares to appear on the board
    public static final int NUM_DANGER_SQUARES = 5;
    // Suppress serialization warning
    private static final long serialVersionUID = 490905409104883233L;

    // Images for warning and death states
    private Image warningImage;
    private Image deathImage;

    // References to the timers
    private Timer timer;
    private Timer dangerSquareRespawnTimer;
    private Timer squareStateChangeTimer;
    // Objects that appear on the game board
    private Player player;
    private ArrayList<DangerSquare> dangerSquares;
    private static ArrayList<Bullet> bullets = new ArrayList<>();

    // Flag indicating whether the game is over
    private boolean gameOver = false;

    public Board() {
        // Set the game board size
        setPreferredSize(new Dimension(TILE_SIZE * COLUMNS, TILE_SIZE * ROWS));
        // Set the game board background color
        setBackground(new Color(232, 232, 232));

        // Load images
        warningImage = new ImageIcon("images/warning.png").getImage();
        deathImage = new ImageIcon("images/death.png").getImage();

        // Initialize the game state
        player = new Player();
        dangerSquares = populateDangerSquares();

        // This timer will call the actionPerformed() method every DELAY ms
        timer = new Timer(DELAY, this);
        timer.start();

        // This timer will choose 5 new danger squares every 5 seconds
        dangerSquareRespawnTimer = new Timer(SQUARE_RESPAWN_DELAY, e -> respawnDangerSquares());
        dangerSquareRespawnTimer.start();

        // This timer will check the state of danger squares every 2 seconds and convert
        // warning squares to death squares
        squareStateChangeTimer = new Timer(SQUARE_STATE_CHANGE_DELAY, e -> updateSquareStates());
        squareStateChangeTimer.start();
    }

    public static void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // This method is called by the timer every DELAY ms.
        // Use this space to update the state of your game or animation
        // before the graphics are redrawn.

        // Prevent the player from disappearing off the board
        if (!gameOver) {
            player.tick();

            // Move and update bullets
            ArrayList<Bullet> bulletsToRemove = new ArrayList<>();
            for (Bullet bullet : bullets) {
                bullet.move();
                if (bullet.isOutOfScreen()) {
                    bulletsToRemove.add(bullet);
                }
            }
            bullets.removeAll(bulletsToRemove);

            // Check for player damage and handle other game updates
            checkPlayerDamage();

            // Trigger repainting
            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the board and objects on the board
        drawBackground(g);
        drawLife(g);
        for (DangerSquare square : dangerSquares) {
            square.draw(g, this, warningImage, deathImage);
        }
        player.draw(g, this);

        // If game is over, display "GAME OVER" on top of the screen
        if (gameOver) {
            drawGameOver(g);
        }

        for (Bullet bullet : bullets) {
            bullet.draw(g, this);
        }

        // Smooths out animations on some systems
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        String text = "GAME OVER";
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = (getWidth() - metrics.stringWidth(text)) / 2;
        int y = getHeight() / 2;
        g.drawString(text, x, y);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used, but required by the KeyListener interface
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // React to key down events
        player.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used, but required by the KeyListener interface
    }

    private void drawBackground(Graphics g) {
        // Draw a checkered background
        g.setColor(new Color(214, 214, 214));
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                // Only color every other tile
                if ((row + col) % 2 == 1) {
                    // Draw a square tile at the current row/column position
                    g.fillRect(
                            col * TILE_SIZE,
                            row * TILE_SIZE,
                            TILE_SIZE,
                            TILE_SIZE);
                }
            }
        }
    }

    private void drawLife(Graphics g) {
        // Draw the player's life and ammo
        String text = "HP:" + player.getlife() + " Ammo:" + player.getAmmo();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setColor(new Color(30, 201, 139));
        g2d.setFont(new Font("Lato", Font.BOLD, 25));
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        Rectangle rect = new Rectangle(0, TILE_SIZE * (ROWS - 1), TILE_SIZE * COLUMNS, TILE_SIZE);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g2d.drawString(text, x, y);
    }

    private ArrayList<DangerSquare> populateDangerSquares() {
        ArrayList<DangerSquare> dangerSquares = new ArrayList<>();
        Random rand = new Random();

        // Create the given number of danger squares in random positions on the board
        for (int i = 0; i < NUM_DANGER_SQUARES; i++) {
            int dangerX = rand.nextInt(COLUMNS);
            int dangerY = rand.nextInt(ROWS);
            dangerSquares.add(new DangerSquare(dangerX, dangerY));
        }

        return dangerSquares;
    }

    private void respawnDangerSquares() {
        // Choose 5 new random positions for danger squares
        dangerSquares.clear(); // Clear the existing danger squares

        Random rand = new Random();
        Set<Point> newPositions = new HashSet<>(); // Use a set to avoid duplicate positions

        while (newPositions.size() < NUM_DANGER_SQUARES) {
            int dangerX = rand.nextInt(COLUMNS);
            int dangerY = rand.nextInt(ROWS);
            Point newPoint = new Point(dangerX, dangerY);

            // Ensure the point is not already in the set
            if (!newPositions.contains(newPoint)) {
                newPositions.add(newPoint);
                dangerSquares.add(new DangerSquare(dangerX, dangerY));
            }
        }
    }

    private void updateSquareStates() {
        // Update the state of each danger square from warning to death after 2 seconds
        LocalTime now = LocalTime.now();
        for (DangerSquare square : dangerSquares) {
            long elapsedMillis = ChronoUnit.MILLIS.between(square.getLastUpdateTime(), now);
            if (elapsedMillis >= SQUARE_STATE_CHANGE_DELAY) {
                square.changeStateToDeath();
            }
        }
    }

    private void checkPlayerDamage() {
        // Check if the player is standing on a death square and apply damage
        Point playerPos = player.getPos();
        ArrayList<DangerSquare> squaresToRemove = new ArrayList<>();

        for (DangerSquare square : dangerSquares) {
            if (square.isDeath() && square.getPos().equals(playerPos)) {
                // Apply damage to the player
                player.addlife(-10); // Change this value for different damage

                // Remove the square since it has dealt damage
                squaresToRemove.add(square);
            }
        }

        // Remove danger squares that have dealt damage
        dangerSquares.removeAll(squaresToRemove);

        // Check if the player's health is zero or below and set game over flag
        if (player.getLifeInt() <= 0) {
            gameOver = true;

            // Stop the timers
            timer.stop();
            dangerSquareRespawnTimer.stop();
            squareStateChangeTimer.stop();
        }
    }

    // Class representing danger squares
    class DangerSquare {
        private Point pos;
        private boolean isDeath;
        private LocalTime lastUpdateTime;

        public DangerSquare(int x, int y) {
            pos = new Point(x, y);
            isDeath = false;
            lastUpdateTime = LocalTime.now();
        }

        public Point getPos() {
            return pos;
        }

        public boolean isDeath() {
            return isDeath;
        }

        public void changeStateToDeath() {
            isDeath = true;
            lastUpdateTime = LocalTime.now(); // Update the last update time
        }

        public LocalTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void draw(Graphics g, JPanel panel, Image warningImage, Image deathImage) {
            if (isDeath) {
                g.drawImage(deathImage, pos.x * TILE_SIZE, pos.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, panel);
            } else {
                g.drawImage(warningImage, pos.x * TILE_SIZE, pos.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, panel);
            }
        }
    }
}
