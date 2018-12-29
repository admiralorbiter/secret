package Gloomhaven.BattleGoals;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Random;

import Gloomhaven.Setting;
import Gloomhaven.Characters.Player;

public class BattleGoalSelection {
	
	Setting setting = new Setting();
	int index1=-1;
	int index2=-1;
	
	public BattleGoalSelection(List<BattleGoalCard> deck) {
		Random r = new Random();
		index1=r.nextInt(deck.size()-1);
		index2=index1;
		while(index2==index1) {
			index2=r.nextInt(deck.size()-1);
		}
	}
	
	public boolean chooseCard(Graphics g, KeyEvent key, Player player, List<BattleGoalCard> deck) {
			
			
		g.drawString("1: "+deck.get(index1).getName()+": "+deck.get(index1).getText()+"    "+deck.get(index1).getReward(), setting.getGraphicsX(), setting.getGraphicsYTop()+50);
		g.drawString("2: "+deck.get(index2).getName()+": "+deck.get(index2).getText()+"    "+deck.get(index2).getReward(), setting.getGraphicsX(), setting.getGraphicsYTop()+75);
		
		if(key!=null) {
			if(key.getKeyCode()==KeyEvent.VK_1) {
				player.setBattleGoalCard(deck.get(index1));
				deck.remove(index1);
				return true;
			}
			if(key.getKeyCode()==KeyEvent.VK_2) {
				player.setBattleGoalCard(deck.get(index2));
				deck.remove(index2);
				return true;
			}
		}
		return false;
	}
}