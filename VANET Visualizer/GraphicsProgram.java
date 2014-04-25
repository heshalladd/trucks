import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class GraphicsProgram extends JPanel implements Runnable {

	private TruckGraphic mTruckGraphic;
	private World mWorld;
	private DataBase mDataBase;
	BufferedImage mTruckImage = null;
	BufferedImage mRoadImage = null;
    boolean radioOn = false;
	JButton b3;

	public GraphicsProgram() {
		setSize(1280, 768);
		setBackground(Color.GREEN);
		mTruckGraphic = new TruckGraphic(20, 150, 25, 14);
		try {
			mTruckImage = ImageIO.read(new File("images/truck.png"));
			mRoadImage = ImageIO.read(new File("images/street.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		b3 = new JButton("Toggle Radio");
		b3.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				radioOn = (radioOn) ? false : true;
				System.out.println(radioOn);
			}
			
		});
		this.setLayout(null);
		Insets insets = this.getInsets();
		Dimension size = b3.getPreferredSize();

		b3.setBounds(500 + insets.left, 500 + insets.top,
		             size.width, size.height);
		
		//b3.setBounds(500,500,size.width,size.height);
		this.add(b3);
		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				System.out.println(e.getPoint());
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});
	}

	public void setDataBase(DataBase dataBase) {
		mDataBase = dataBase;
		System.out.println("yes");
	}

	public void run() {
		JFrame frame = new JFrame("Truck Simulator");
		frame.add(this);
		frame.setSize(1024, 780);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		while (true) {

			this.repaint();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {

		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		

		g2d.drawImage(mRoadImage, 0, 50, null);
		g2d.drawImage(mRoadImage, 597, 50, null);
		g2d.setColor(Color.RED);
		g2d.drawRect(mDataBase.x1, 60, 75, 22);
		g2d.drawImage(mTruckImage, mDataBase.x1%1280, 60, null);
		g2d.setColor(new Color(255,0,0,100));
		if(radioOn){
			g2d.fillOval((mDataBase.x1-70+37)%(1280), (60-70)+11, 140, 140);

		}

		g2d.drawImage(mTruckImage, mDataBase.x2%1050, 60, null);
		
		g2d.setColor(Color.LIGHT_GRAY);
		g2d.fillRect(480, 480, 200, 200);
		g2d.setColor(Color.black);
		g2d.drawString("X: " + mDataBase.x1 + " Y: " + mTruckGraphic.mY1, 500,
				500);
		b3.repaint();
		// g2d.finalize();
		//
		// g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);
		// g2d.fillOval(x, y, 30, 30);
	}

}