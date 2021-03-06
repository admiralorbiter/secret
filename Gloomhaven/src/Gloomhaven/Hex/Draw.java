package Gloomhaven.Hex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.ImageIcon;

import Gloomhaven.Characters.Player;
import Gloomhaven.Characters.Character;
import Gloomhaven.Scenario.Scenario;
import Unsorted.GUI;
import Unsorted.Setting;

public final class Draw {
	
	@SuppressWarnings("ucd")
	public static  void parallelogramBoard(Graphics2D g, int size, boolean flatlayout, Point center, int radius) {
		for(int y=-radius; y<=radius; y++) {
			for(int x=-radius; x<=radius; x++) {			
				int q=x;
				int r=y;
				int s=-x-y;				
				drawHex(g, q, r, s, size, flatlayout, new Point(500, 500), null, null);
				//drawHex(g, s, q, r, 40, true, new Point(500, 500));
				//drawHex(g, r, s, q, 40, true, new Point(500, 500));
			}
		}
	}
	
	@SuppressWarnings("ucd")
	public static void triangleBoard(Graphics2D g, int size, boolean flatlayout, Point center, int radius) {
		for(int x=0; x<=radius; x++) {
			for(int y=0; y<=radius-x; y++) {		
				int q=x;
				int r=y;
				int s=-x-y;	
				drawHex(g, q, r, s, size, flatlayout, center, null, null);
			}
		}
	}
	
	public static void range(Graphics2D g, HexCoordinate point, int range, boolean flatlayout) {

		g.setColor(Color.blue);
		
		for(int q=-range; q<=range; q++) {
			int r1=Math.max(-range, -q-range);
			int r2 = Math.min(range, -q+range);
			for(int r = r1; r<=r2; r++) {
				int s=-q-r;
				HexCoordinate hex = UtilitiesHex.add(new HexCoordinate(q, s, r), point);

				drawHex(g, hex, Setting.size, flatlayout, Setting.center, null, null);
			}
		}
	}
	
	@SuppressWarnings("ucd")
	public static void hexagonBoard(Graphics2D g, int size, boolean flatlayout, Point center, int radius) {
	
		for(int q=-radius; q<=radius; q++) {
			int r1=Math.max(-radius, -q-radius);
			int r2 = Math.min(radius, -q+radius);
			for(int r = r1; r<=r2; r++) {
				int s=-q-r;
				drawHex(g, q, r, s, size, flatlayout, center, null, null);
			}
		}
	}

	
	@SuppressWarnings("ucd")
	public static void rectangleBoardUpDown(Graphics2D g, int size, boolean flatlayout, Point center,  Point dimensions) {
		int height=(int) dimensions.getY();
		int width=(int) dimensions.getX();
		for(int q=0; q<height; q++) {
			int q_offset=(int) Math.floor(q/2);
			for(int s=-q_offset; s<width-q_offset; s++) {
				int r=-s-q;
				drawHex(g, q, r, s, size, false, center, null, null);
			}
		}
	}
	
	@SuppressWarnings("ucd")
	public static void rectangleBoardSideways(Graphics2D g, int size, boolean flatlayout, Point center,  Point dimensions) {
		int height=(int) dimensions.getY();
		int width=(int) dimensions.getX();	
		for(int r=0; r<height; r++) {
			int r_offset=(int) Math.floor(r/2);
			for(int q=-r_offset; q<width-r_offset; q++) {
				int s=-q-r;
				drawHex(g, q, r, s, size, flatlayout, center, null, null);
			}
		}
	}
	
	public static void rectangleBoardSideways(Graphics2D g, Hex[][] board, Point dimensions, boolean flatlayout) {
		g.drawRect(Setting.center.x-Setting.size, Setting.center.y-Setting.size, (dimensions.x-2)*Setting.size*2, (dimensions.y-2)*Setting.size*2+Setting.size);
		
		for(int x=0; x<dimensions.x; x++) {
			for(int y=0; y<dimensions.y; y++) {
				if(board[x][y]!=null) {
					if(board[x][y].hasLoot())
						g.setColor(Setting.lootColor);
					else if(board[x][y].hasObstacle())
						g.setColor(Setting.obstacleColor);
					else
						g.setColor(Setting.defaultColor);
					drawHex(g, board[x][y], null, flatlayout, null);
				}
			}	
		}
	}
	
	public static void itemsAndObstaclesOnly(Graphics2D g, Hex[][] board, Point dimensions, boolean flatlayout) {
		g.drawRect(Setting.center.x-Setting.size, Setting.center.y-Setting.size, (dimensions.x-2)*Setting.size*2, (dimensions.y-2)*Setting.size*2+Setting.size);
		
		for(int x=0; x<dimensions.x; x++) {
			for(int y=0; y<dimensions.y; y++) {
				if(board[x][y]!=null) {
					if(board[x][y].hasLoot()) {
						g.setColor(Setting.lootColor);
						drawHex(g, board[x][y], null, flatlayout, Setting.lootColor);
					}
					else if(board[x][y].hasObstacle()) {
						g.setColor(Setting.obstacleColor);
						drawHex(g, board[x][y], null, flatlayout, Setting.obstacleColor);
					}else if(board[x][y].hasDoor()) {
						g.setColor(Setting.doorColor);
						drawHex(g, board[x][y], null, flatlayout, Setting.doorColor);
					}
				}
			}	
		}
	}
	
	public static void drawHex(Graphics2D g, Point h, Character entity, boolean flatlayout, Color fill) {
		drawHex(g, h, Setting.size, flatlayout, Setting.center, entity, fill);
	}
	
	public static void drawHex(Graphics2D g, HexCoordinate h, Character entity, boolean flatlayout, Color fill) {
		drawHex(g, h, Setting.size, flatlayout, Setting.center, entity, fill);
	}
	
	public static void drawHex(Graphics2D g, HexCoordinate h, int size, boolean flatlayout, Point center, Character entity, Color fill) {
		drawHex(g, h.q, h.r, h.s, size, flatlayout, center, entity, fill);
	}
	
	public static void drawHex(Graphics2D g, Point h, int size, boolean flatlayout, Point center, Character entity, Color fill) {
		
		HexCoordinate hex;
		
		if(flatlayout)
			hex = UtilitiesHex.flatOffsetToCube(1, h);
		else
			hex = UtilitiesHex.pointyOffsetToCube(1, h);
		
		drawHex(g, hex, size, flatlayout, center, entity, fill);
	}
	
	public static void drawHex(Graphics2D g, Hex hex, Character entity, boolean flatlayout, Color fill) {
		if(hex!=null) {
			if(!hex.isHidden())
				drawHex(g, hex.offsetCoordinate, entity, flatlayout, fill);
		}
	}
	
	public static void drawHex(Graphics2D g, int q, int r, int s, int size, boolean flatlayout, Point center, Character entity, Color fill) {
		HexLayout layout;

		if(flatlayout)
			layout = new HexLayout(UtilitiesHex.getFlatLayoutOrientation(), new Point(size, size), center);
		else
			layout = new HexLayout(UtilitiesHex.getPointyLayoutOrientation(), new Point(size, size), center);
		
		
		List<Point2D> corners = UtilitiesHex.polygonCorners(layout, new HexCoordinate(q, r, s));
		int tX[] = new int[8];
		int tY[] = new int[8];

		for(int i=0; i<6; i++) {
			tX[i]=(int) corners.get(i).getX();
			tY[i]=(int) corners.get(i).getY();
		}

		Color oldColor = g.getColor();
		
		if(fill==null)
			g.setColor(Setting.hextFill);
		else
			g.setColor(fill);
		
		g.fillPolygon(tX, tY, 6);
		g.setColor(oldColor);
		g.drawPolygon(tX, tY, 6);
		
		
		
		if(Setting.test) {
			if(flatlayout) {
				//g.drawString(q+", "+r+","+s, tX[3]+20, tY[3]);
				if(size>=40 && entity!=null)
					g.drawString(entity.getID() , tX[3]+20, tY[3]);
				g.drawString(UtilitiesHex.flatOffsetFromCube(1, new HexCoordinate(q, r, s)).x+","+UtilitiesHex.flatOffsetFromCube(1, new HexCoordinate(q, r, s)).y, tX[3]+20, tY[3]+15);
			}
			else {
				g.drawString((int)UtilitiesHex.pointyOffsetFromCube(1, new HexCoordinate(q, r, s)).getX()+","+(int)UtilitiesHex.pointyOffsetFromCube(1, new HexCoordinate(q, r, s)).getY(), tX[3]+20, tY[3]+40);
				//g.drawString(q+", "+r+","+s, tX[3]+20, tY[3]+20);
				if(size>=40 && entity!=null)
					g.drawString(entity.getID() , tX[3]+20, tY[3]+20);
			}
		}
		if(entity!=null)
			if(entity.getImageIcon()!=null && Setting.test!=true) {
				/*
				AffineTransform at = AffineTransform.getTranslateInstance(tX[4], tY[4]);
				Image newimg = image.getImage().getScaledInstance(size*2, size*2, java.awt.Image.SCALE_DEFAULT);
				image.setImage(newimg);
				at.translate(-size/2, 0);
				at.rotate(Math.toRadians(90), image.getIconWidth()/2, image.getIconHeight()/2);
				*/
				g.drawImage(entity.getImageIcon().getImage(),  tX[4], tY[4]+size/2, size, size, null);
			}
	}

	public static void drawParty(Graphics2D g, List<Player> party, boolean flatlayout) {
		g.setColor(Setting.playerColor);
		for(int i=0; i<party.size(); i++) {
			drawHex(g, party.get(i).getCoordinates(), party.get(i), flatlayout, null);
			GUI.drawCharacterInfo(g, flatlayout, party.get(i));
		}
		
		g.setColor(Setting.defaultColor);
	}
}
