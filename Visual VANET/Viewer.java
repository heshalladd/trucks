import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class Viewer extends JPanel implements Runnable {

	BufferedImage mTruckImage = null;
	BufferedImage mRoadImage = null;
	BufferedImage mFrogImage = null;
	boolean radioOn = false;
	JButton b3;
	TruckDataList mTruckDataList;
	boolean mFroggerMode = true;
	static int mFrogX = 200;
	static int mFrogY = 200;
	int mSelectedTruck = -1;

	public Viewer() {
		setSize(1280, 768);
		setBackground(new Color(0,130,0));
		this.setDoubleBuffered(true);
		try {
			mTruckImage = ImageIO.read(new File("images/truck.png"));
			mRoadImage = ImageIO.read(new File("images/street.png"));
			mFrogImage = ImageIO.read(new File("images/frogger.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		b3 = new JButton("Toggle Radio");
		b3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				radioOn = (radioOn) ? false : true;
				System.out.println(radioOn);
			}

		});
		this.setLayout(null);
		Insets insets = this.getInsets();
		Dimension size = b3.getPreferredSize();

		b3.setBounds(500 + insets.left, 700 + insets.top, size.width,
				size.height);

		// b3.setBounds(500,500,size.width,size.height);
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
				Point p = e.getPoint();
				TruckDataUnit truckDataUnit;
				Rectangle truckRectangle;

				int x;
				for(int truckNumber = 1; truckNumber<=10;truckNumber++){
					truckDataUnit = mTruckDataList.truckDataList.get(truckNumber);
					if (truckDataUnit.mAlive) {
						x = truckDataUnit.mLocationX; 
						truckRectangle = new Rectangle(x%1280,getYFromX(x),75,22);
						if(truckRectangle.contains(p)){
							System.out.println("TruckNumber: "+ truckDataUnit.mNumber);
							mSelectedTruck = truckDataUnit.mNumber;
						}
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});
		this.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case 38:
					//System.out.println("UP");
					mFrogY-=15;
					break;
				case 40:
					mFrogY+=15;
					//System.out.println("Down");
					break;
				case 37:
					mFrogX-=15;
					//System.out.println("LEFT");
					break;
				case 39:
					mFrogX+=15;
					//System.out.println("RIGHT");
					break;
				}

			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyTyped(KeyEvent e) {

			}
		});
	}

	public void setTruckDataList(TruckDataList truckDataList) {
		mTruckDataList = truckDataList;
	}

	public void run() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu1 = new JMenu("File");
		JMenu menu2 = new JMenu("Settings");
		JMenu menu3 = new JMenu("About");
		JMenuItem menuItem1 = new JMenuItem("Quit");
		menu1.add(menuItem1);
		menuBar.add(menu1);
		
		menuBar.add(menu2);
		menuBar.add(menu3);
		JFrame frame = new JFrame("Truck Simulator");
		frame.setJMenuBar(menuBar);
		frame.add(this);
		frame.setSize(1280, 780);
		frame.setVisible(true);
		frame.setLocation(1280, 0);
		frame.isFocusable();
		this.requestFocusInWindow();
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

		g2d.drawImage(mRoadImage, 0, 30, null);
		g2d.drawImage(mRoadImage, 597, 30, null);
		g2d.drawImage(mRoadImage, 1194, 30, null);
		
		g2d.drawImage(mRoadImage, 0, 150, null);
		g2d.drawImage(mRoadImage, 597, 150, null);
		g2d.drawImage(mRoadImage, 1194, 150, null);
		
		g2d.drawImage(mRoadImage, 0, 270, null);
		g2d.drawImage(mRoadImage, 597, 270, null);
		g2d.drawImage(mRoadImage, 1194, 270, null);
		
		g2d.setColor(new Color(0,0,0,140));
		g2d.fillRect(15, 390, 400, 300);

		TruckDataUnit truckDataUnit;
		int y = 60;
		for (int truckNumber = 1; truckNumber <= 10; truckNumber++) {
			truckDataUnit = mTruckDataList.truckDataList.get(truckNumber);
			if (truckDataUnit.mAlive) {
				int x = truckDataUnit.mLocationX;
				y = getYFromX(x);
				
				g2d.drawImage(mTruckImage, x % 1280, y, null);
				if(mSelectedTruck == truckDataUnit.mNumber){
					Rectangle truckRectangle = new Rectangle(x%1280,y,75,22);
					g2d.setColor(Color.RED);
					float thickness = 2;
					Stroke oldStroke = g2d.getStroke();
					g2d.setStroke(new BasicStroke(thickness));
					g2d.draw(truckRectangle);
					g2d.setStroke(oldStroke);

				}
				
				
				//g2d.setColor(Color.black);
				//g2d.drawString(truckDataUnit.mInformation, 490, 490);
				if(mFroggerMode){
					g2d.drawImage(mFrogImage, mFrogX, mFrogY, null);
					Rectangle truckRectangle = new Rectangle(x%1280,y,75,22);
					g2d.setColor(new Color(255, 0, 0, 255));
					g2d.draw(truckRectangle);
					Rectangle frogRectangle = new Rectangle(this.mFrogX,this.mFrogY,40,56);
					g2d.draw(frogRectangle);
					if(truckRectangle.intersects(frogRectangle)){
						//System.out.println("SQUISH!!!");
					}
				}
			}
		}
	
		if(mSelectedTruck != -1){
			String s = mTruckDataList.truckDataList.get(mSelectedTruck).mInformation;
		       int yDraw = 400;
				g2d.setColor(Color.WHITE);
					for (String line : s.split("\n"))
		            g.drawString(line, 17, yDraw += g.getFontMetrics().getHeight());
		}
		
		b3.repaint();

	}

	public int getYFromX(int x){
		int multiples = x / 1280;
		int row = 1;
		while (multiples > 0) {
			multiples--;
			row++;
			if (row == 4)
				row = 1;
		}
		
		switch(row){
		case 1:
			return 90;
		case 2:
			return 210;
		case 3:
			return 330;
		}
		return -100;
		
	}
	
}