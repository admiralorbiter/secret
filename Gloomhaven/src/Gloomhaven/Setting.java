package Gloomhaven;

import java.awt.Color;
import java.awt.Point;

public final class Setting {
	public static boolean test = false;
	
	public static int size=40;
	public static boolean flatlayout=true;
	public static Point center = new Point(500, 100);
	
	public static String title="Gloomhaven";

	public static Color defaultColor = Color.WHITE;
	public static Color highlightColor = Color.YELLOW;
	public static Color playerColor = Color.RED;
	public static Color enemyColor = Color.MAGENTA;
	public static Color lootColor = Color.YELLOW;
	public static Color obstacleColor = Color.GREEN;
	public static Color hextFill = Color.black;
	//Party Info
	public static int numberOfPlayers=1;
	public static String playerClass="Mind Thief";
	public static int getMaxHandCount() {
		if(playerClass=="Brute" || playerClass=="Mind Thief")
			return 10;
		if(playerClass=="Scoundrel")
			return 9;
		if(playerClass=="Spellweaver")
			return 8;
		if(playerClass=="Cragheart")
			return 11;
		if(playerClass=="Tinkerer")
			return 12;
		
		return 0;
	}
	
	//Temp
	public static int sceneID=1;
	
	public static char restKey='r';
	public static char discardKey='d';
	public static char healKey='h';
	public static char moveKey='m';
	public static char up='w';
	public static char down='s';
	public static char left='a';
	public static char right='d';
	public static char targetKey='t';
}
