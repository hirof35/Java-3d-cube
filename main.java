package Mapping;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

// クラス名をファイル名と同じ先頭小文字の「mapping」にし、JPanelを継承させています
public class mapping extends JPanel {
    private static final int W = 600;
    private static final int H = 600;
    
    private double rotX = 0;
    private double rotY = 0;
    private final Vec3 light = new Vec3(0.5, -0.8, -0.2).normalize();
    private final Cube cube;
    private BufferedImage picture;

    public mapping() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        
        cube = new Cube(0, 0, 0, 100, 100, 100);
        
        // プロジェクト直下の画像ファイルを読み込む
        try {
            picture = ImageIO.read(new File("picture0.jpg"));
            System.out.println("画像の読み込みに成功しました！");
        } catch (Exception e) {
            System.out.println("picture0.jpg の読み込みに失敗したため、ダミーを生成します。原因: " + e.getMessage());
            picture = createDummyTexture();
        }

        Timer timer = new Timer(50, e -> tick());
        timer.start();
    }

    private void tick() {
        rotY += 0.05;
        rotX += 0.02;

        double cY = Math.cos(rotY), sY = Math.sin(rotY);
        double[] matrixRotY = {cY, 0, sY, 0, 1, 0, -sY, 0, cY};

        double cX = Math.cos(rotX), sX = Math.sin(rotX);
        double[] matrixRotX = {1, 0, 0, 0, cX, -sX, 0, sX, cX};

        cube.setCamera(0, 0, -1000, matrixRotX, matrixRotY);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<Surface> surfaces = cube.getSurfaces();
        Collections.sort(surfaces, (a, b) -> Double.compare(b.cZ, a.cZ));

        for (Surface s : surfaces) {
            double p = (s.norm.x * light.x + s.norm.y * light.y + s.norm.z * light.z);
            double ratio = (p + 1) / 2;

            Point[] pos = new Point[4];
            for (int i = 0; i < 4; i++) {
                Vec3 v = s.pos[i];
                double z = v.z;
                double x = v.x * 1000 / z + 300;
                double y = -v.y * 1000 / z + 300;
                pos[i] = new Point((int) x, (int) y);
            }

            drawTexture(g2d, picture, pos[0], pos[1], pos[2], pos[3], ratio);

            g2d.setColor(Color.GREEN);
            Path2D path = new Path2D.Double();
            path.moveTo(pos[0].x, pos[0].y);
            for (int i = 1; i < 4; i++) path.lineTo(pos[i].x, pos[i].y);
            path.closePath();
            g2d.draw(path);
        }
    }

    private void drawTexture(Graphics2D g2d, BufferedImage img, Point p0, Point p1, Point p2, Point p3, double ratio) {
        if (img == null) return;

        int imgW = img.getWidth();
        int imgH = img.getHeight();

        // 三角形1
        Graphics2D g1 = (Graphics2D) g2d.create();
        Path2D.Double clip1 = new Path2D.Double();
        clip1.moveTo(p0.x, p0.y);
        clip1.lineTo(p2.x, p2.y);
        clip1.lineTo(p3.x, p3.y);
        clip1.closePath();
        g1.setClip(clip1);

        double dx = p0.x, dy = p0.y;
        double sx = (p3.x - p0.x) / imgW;
        double sy = (p2.y - p3.y) / imgH;
        double rx = (p3.y - p0.y) / imgW;
        double ry = (p2.x - p3.x) / imgH;
        
        AffineTransform at1 = new AffineTransform(sx, rx, ry, sy, dx, dy);
        g1.drawImage(img, at1, null);
        applyBrightness(g1, clip1, ratio);
        g1.dispose();

        // 三角形2
        Graphics2D g2 = (Graphics2D) g2d.create();
        Path2D.Double clip2 = new Path2D.Double();
        clip2.moveTo(p0.x, p0.y);
        clip2.lineTo(p1.x, p1.y);
        clip2.lineTo(p2.x, p2.y);
        clip2.closePath();
        g2.setClip(clip2);

        sx = (p2.x - p1.x) / imgW;
        sy = (p1.y - p0.y) / imgH;
        rx = (p2.y - p1.y) / imgW;
        ry = (p1.x - p0.x) / imgH;

        AffineTransform at2 = new AffineTransform(sx, rx, ry, sy, dx, dy);
        g2.drawImage(img, at2, null);
        applyBrightness(g2, clip2, ratio);
        g2.dispose();
    }

    private void applyBrightness(Graphics2D g, Shape shape, double ratio) {
        if (ratio >= 1.0) return;
        int alpha = (int) ((1.0 - ratio) * 255);
        g.setColor(new Color(0, 0, 0, Math.max(0, Math.min(255, alpha))));
        g.fill(shape);
    }

    private BufferedImage createDummyTexture() {
        BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 600, 600);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Java 3D Texture", 140, 300);
        g.dispose();
        return img;
    }

    static class Vec3 {
        double x, y, z;
        Vec3(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
        Vec3 normalize() {
            double scale = 1.0 / Math.sqrt(x * x + y * y + z * z);
            this.x *= scale;
            this.y *= scale;
            this.z *= scale;
            return this;
        }
    }

    static class Surface {
        Vec3[] pos;
        Vec3 norm;
        double cZ;

        Surface(Vec3[] vertices) {
            this.pos = vertices;
            Vec3 p1 = vertices[0], p2 = vertices[1], p3 = vertices[2], p4 = vertices[3];
            Vec3 p = new Vec3(p1.x - p2.x, p1.y - p2.y, p1.z - p2.z);
            Vec3 q = new Vec3(p1.x - p3.x, p1.y - p3.y, p1.z - p3.z);
            Vec3 n = new Vec3(
                p.y * q.z - p.z * q.y,
                p.z * q.x - p.x * q.z,
                p.x * q.y - p.y * q.x
            );
            this.norm = n.normalize();
            this.cZ = (p1.z + p2.z + p3.z + p4.z) / 4.0;
        }
    }

    static class Cube {
        Vec3[] vertices;
        Vec3[] pos;
        int[][] polygons;

        Cube(double x, double y, double z, double w, double h, double d) {
            this.pos = new Vec3[8];
            for(int i=0; i<8; i++) this.pos[i] = new Vec3(0,0,0);

            this.vertices = new Vec3[]{
                new Vec3(x - w, y - h, z + d),
                new Vec3(x - w, y + h, z + d),
                new Vec3(x + w, y + h, z + d),
                new Vec3(x + w, y - h, z + d),
                new Vec3(x - w, y - h, z - d),
                new Vec3(x - w, y + h, z - d),
                new Vec3(x + w, y + h, z - d),
                new Vec3(x + w, y - h, z - d)
            };

            this.polygons = new int[][]{
                {2, 1, 5, 6}, {0, 1, 2, 3}, {4, 5, 1, 0},
                {2, 6, 7, 3}, {7, 6, 5, 4}, {0, 3, 7, 4}
            };
        }

        List<Surface> getSurfaces() {
            List<Surface> r = new ArrayList<>();
            for (int[] indices : polygons) {
                Vec3[] p = new Vec3[indices.length];
                for (int j = 0; j < indices.length; j++) {
                    p[j] = this.pos[indices[j]];
                }
                r.add(new Surface(p));
            }
            return r;
        }

        void setCamera(double cameraX, double cameraY, double cameraZ, double[] mRotX, double[] mRotY) {
            for (int i = 0; i < vertices.length; i++) {
                Vec3 c = vertices[i];
                double x = c.x - cameraX;
                double y = c.y - cameraY;
                double z = c.z;

                double p = mRotY[0] * x + mRotY[1] * y + mRotY[2] * z;
                double q = mRotY[3] * x + mRotY[4] * y + mRotY[5] * z;
                double r = mRotY[6] * x + mRotY[7] * y + mRotY[8] * z;

                x = mRotX[0] * p + mRotX[1] * q + mRotX[2] * r;
                y = mRotX[3] * p + mRotX[4] * q + mRotX[5] * r;
                z = mRotX[6] * p + mRotX[7] * q + mRotX[8] * r;

                this.pos[i].x = x;
                this.pos[i].y = y;
                this.pos[i].z = z - cameraZ;
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("3D Texture Mapping");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // クラス名を mapping に変更して生成
        frame.add(new mapping());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
