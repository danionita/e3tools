package design.main;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class IconStore {
	public static Map<String, ImageIcon> icons = new HashMap<>();
	
	public static ImageIcon getIcon(String id) {
		return getImage("/icons/" + id + ".png");
	}
	
	public static ImageIcon getImage(String path, int w, int h) {
		if (!icons.containsKey(path)) {
			try {
				ImageIcon img = new ImageIcon(ImageIO.read(IconStore.class.getResourceAsStream(path)));

				icons.put(path, img);
			} catch (IOException | IllegalArgumentException e) {
				System.out.println("Could not find image at " + path);
				e.printStackTrace();
			}
		}
		
		if (w == -1 && h == -1) {
			return icons.get(path);
		} else {
			ImageIcon img = icons.get(path);

			BufferedImage bi = new BufferedImage(
					img.getIconWidth(),
					img.getIconHeight(),
					BufferedImage.TYPE_INT_ARGB
					);

			Graphics g = bi.createGraphics();
			img.paintIcon(null, g, 0, 0);
			g.dispose();

			return new ImageIcon(bi.getScaledInstance(w, h, Image.SCALE_SMOOTH));
		}
	}
	
	public static ImageIcon getImage(String path) {
		return getImage(path, -1, -1);
	}
}
