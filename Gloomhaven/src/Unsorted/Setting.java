package Unsorted;

import java.awt.Color;
import java.awt.Point;

import javax.swing.ImageIcon;

public final class Setting {
	public static boolean test = false;
	public static boolean testAbilityCard = false;
	public static boolean stateTest=true;
	public static boolean straightToScenario=true;
	
	public static boolean drawLines = false;
	
	public static int size=40;

	public static Point center = new Point(GUISettings.width/3+50, 100);
	
	public static int TEST_HEALTH=1;
	public static int TEST_SHIELD=0;
	
	public static int ALTATTACK=4;
	public static int ALTMOVE=4;
	
	public static String title="Gloomhaven";
	public static Color lineColor = Color.white;
	public static Color defaultColor = Color.white;
	public static Color highlightColor = Color.YELLOW;
	public static Color playerColor = Color.RED;
	public static Color enemyColor = Color.MAGENTA;
	public static Color lootColor = Color.YELLOW;
	public static Color obstacleColor = Color.GREEN;
	public static Color doorColor = new Color(166, 164, 123);
	public static Color hextFill = Color.black;
	public static Color playerInfoColor = Color.white;
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
	
	public static ImageIcon background = new ImageIcon("src/Gloomhaven/img/RoundedBackground.png");
	
	//test
	public static boolean TestShowEnemyCardIndex=true;
	public static boolean TestStats=false;
}
