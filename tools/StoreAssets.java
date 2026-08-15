import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.AttributedString;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Generates the Play Store graphics from the app's own design tokens and launcher-icon
 * geometry, so the store listing and the installed icon are literally the same mark.
 *
 *   java tools/StoreAssets.java          (needs JDK 21 — C:\Android\jdk21\jdk-21.0.12+8)
 *
 * Outputs into store/:
 *   icon-512.png                 Play Store listing icon (required, 512x512, no alpha)
 *   icon-1024.png                Devpost / press (docs/07-SUBMISSION-KIT.md)
 *   feature-graphic-1024x500.png Play Store feature graphic (required)
 *
 * Everything here is original artwork: no game box art, no third-party logo, no influencer
 * branding (docs/01-PLAY-STORE-CRITICAL-PATH.md §Compliance traps).
 */
public final class StoreAssets {

    // docs/03-DESIGN-SYSTEM.md — core/design/Color.kt, kept in sync by hand.
    static final Color VOID_        = new Color(0x08090C);
    static final Color CABINET      = new Color(0x101218);
    static final Color RAISED       = new Color(0x181B23);
    static final Color OUTLINE_DIM  = new Color(0x242833);
    static final Color COIN         = new Color(0xF7C948);
    static final Color COIN_SHADOW  = new Color(0xC79A2E);
    static final Color NEON         = new Color(0x00E5A0);
    static final Color TEXT_PRIMARY = new Color(0xF2F4F8);
    static final Color TEXT_SECOND  = new Color(0x9AA3B2);

    static final String FONT_DIR = "app/src/main/res/font/";

    public static void main(String[] args) throws Exception {
        new File("store").mkdirs();

        Font bold = font("chakrapetch_bold.ttf");
        Font semi = font("chakrapetch_semibold.ttf");

        write(icon(512), "store/icon-512.png");
        write(icon(1024), "store/icon-1024.png");
        write(featureGraphic(bold, semi), "store/feature-graphic-1024x500.png");

        System.out.println("Wrote store/icon-512.png, store/icon-1024.png, "
                + "store/feature-graphic-1024x500.png");
    }

    // ---------------------------------------------------------------- icon

    /**
     * The launcher mark (res/drawable/ic_launcher_foreground.xml) rendered full-bleed. The
     * adaptive icon masks its outer 18/108, so the store icon scales the artwork up to fill
     * the square the way the masked launcher icon reads on a home screen.
     */
    static BufferedImage icon(int size) {
        // ARGB, then filled edge to edge: Play's icon spec asks for a 32-bit PNG, and a
        // store icon must never actually be transparent.
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = quality(img);

        g.setColor(VOID_);
        g.fillRect(0, 0, size, size);

        // The cabinet disc from ic_launcher_background.xml.
        g.setColor(CABINET);
        double discR = size * 0.415;
        g.fill(new Ellipse2D.Double(size / 2.0 - discR, size / 2.0 - discR, discR * 2, discR * 2));

        // Vector space is 108x108; artwork bbox is x 26..82, y 24..82 (centre 54,53).
        double s = size / 108.0 * 1.20;
        AffineTransform saved = g.getTransform();
        g.translate(size / 2.0 - 54 * s, size / 2.0 - 53 * s);
        g.scale(s, s);
        coinGlow(g, 54, 40, 30);
        drawMark(g, true);
        g.setTransform(saved);

        // Faint top hairline, the same one every raised surface in the app carries.
        g.setColor(new Color(255, 255, 255, 14));
        g.fillRect(0, 0, size, Math.max(1, size / 256));

        g.dispose();
        return img;
    }

    // ------------------------------------------------------- feature graphic

    static BufferedImage featureGraphic(Font bold, Font semi) {
        int w = 1024, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = quality(img);

        g.setPaint(new GradientPaint(0, 0, CABINET, 0, h, VOID_));
        g.fillRect(0, 0, w, h);

        // Cabinet floor glow under the mark.
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(w * 0.79, h * 0.46), (float) (h * 0.62),
                new float[]{0f, 1f},
                new Color[]{new Color(247, 201, 72, 46), new Color(247, 201, 72, 0)}));
        g.fillRect(0, 0, w, h);

        scanlines(g, w, h);
        vignette(g, w, h);

        // Wordmark: "CONTINUE" in bone white, "?" in coin gold — the app's one accent.
        Font wordFont = tracked(bold.deriveFont(Font.BOLD, 118f), 0.02);
        g.setFont(wordFont);
        FontMetrics fm = g.getFontMetrics();
        int x = 78, baseline = 250;
        String word = "CONTINUE";
        g.setColor(TEXT_PRIMARY);
        g.drawString(word, x, baseline);
        int wordW = fm.stringWidth(word);
        g.setColor(COIN);
        g.drawString("?", x + wordW, baseline);

        // Gold rule + tagline.
        g.setColor(COIN);
        g.fillRect(x + 4, baseline + 34, 150, 4);
        g.setFont(tracked(semi.deriveFont(Font.PLAIN, 25f), 0.16));
        g.setColor(TEXT_SECOND);
        g.drawString("THE GAMES YOU STARTED", x, baseline + 92);
        g.setColor(NEON);
        g.drawString("DESERVE AN ENDING", x, baseline + 132);

        // Small caps kicker above the wordmark.
        g.setFont(tracked(semi.deriveFont(Font.PLAIN, 21f), 0.34));
        g.setColor(new Color(0x5A6373));
        g.drawString("GAMING BACKLOG · ARCADE CABINET", x + 4, baseline - 128);

        // The mark, on the right. Kept clear of the "?" — two gold shapes touching would read
        // as one blob at the sizes Play actually renders this.
        double s = 2.85;
        AffineTransform saved = g.getTransform();
        g.translate(w * 0.845 - 54 * s, h * 0.5 - 53 * s);
        g.scale(s, s);
        coinTrail(g);
        drawMark(g, false);
        g.setTransform(saved);

        // Inset hairline frame — the cabinet edge.
        g.setColor(OUTLINE_DIM);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(20, 20, w - 40, h - 40, 10, 10));

        g.dispose();
        return img;
    }

    // ---------------------------------------------------------------- parts

    /** Coin + cabinet slot, in the 108x108 vector space of ic_launcher_foreground.xml. */
    static void drawMark(Graphics2D g, boolean rim) {
        // Slot body.
        g.setColor(RAISED);
        g.fill(new RoundRectangle2D.Double(26, 66, 56, 16, 8, 8));
        g.setColor(OUTLINE_DIM);
        g.setStroke(new BasicStroke(0.9f));
        g.draw(new RoundRectangle2D.Double(26, 66, 56, 16, 8, 8));
        // The slit itself.
        g.setColor(VOID_);
        g.fill(new RoundRectangle2D.Double(42, 69.5, 24, 5, 2.5, 2.5));

        // Coin.
        g.setPaint(new GradientPaint(38, 24, COIN, 70, 56, COIN_SHADOW));
        g.fill(new Ellipse2D.Double(38, 24, 32, 32));
        g.setColor(COIN_SHADOW);
        g.fill(new Ellipse2D.Double(46, 32, 16, 16));
        // Struck highlight, so the coin reads as metal rather than a yellow dot.
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(41, 27, 26, 26, 105, 70, Arc2D.OPEN));
        if (rim) {
            g.setColor(new Color(247, 201, 72, 120));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Double(37, 23, 34, 34));
        }
    }

    /** Two fading ghosts above the coin — the mascot motion, mid-drop. */
    static void coinTrail(Graphics2D g) {
        coinGlow(g, 54, 40, 34);
        double[][] ghosts = {{40 - 17, 30}, {40 - 31, 15}};
        for (double[] ghost : ghosts) {
            g.setColor(new Color(247, 201, 72, (int) ghost[1] * 3));
            g.fill(new Ellipse2D.Double(38, ghost[0], 32, 32));
        }
    }

    static void coinGlow(Graphics2D g, double cx, double cy, double r) {
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) r,
                new float[]{0f, 1f},
                new Color[]{new Color(247, 201, 72, 60), new Color(247, 201, 72, 0)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    /** 3% scanlines — docs/03-DESIGN-SYSTEM.md says they appear sparingly, never as texture. */
    static void scanlines(Graphics2D g, int w, int h) {
        g.setColor(new Color(255, 255, 255, 8));
        for (int y = 0; y < h; y += 4) g.fillRect(0, y, w, 1);
    }

    static void vignette(Graphics2D g, int w, int h) {
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(w / 2.0, h / 2.0), (float) (w * 0.62),
                new float[]{0.55f, 1f},
                new Color[]{new Color(8, 9, 12, 0), new Color(8, 9, 12, 190)}));
        g.fillRect(0, 0, w, h);
    }

    // ---------------------------------------------------------------- utils

    static Font font(String name) throws Exception {
        return Font.createFont(Font.TRUETYPE_FONT, new File(FONT_DIR + name));
    }

    /** AWT has no letter-spacing on Font directly; TRACKING is the supported route. */
    static Font tracked(Font f, double tracking) {
        return f.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    static Graphics2D quality(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g;
    }

    static void write(BufferedImage img, String path) throws Exception {
        ImageIO.write(img, "png", new File(path));
    }

    private StoreAssets() {}
}
