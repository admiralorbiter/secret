package Unsorted;

import java.util.ArrayList;
import java.util.List;

import Gloomhaven.Characters.Enemy;

public class StatsTracker {

	
	private List<Enemy> enemiesKilled = new ArrayList<Enemy>();
	private List<Enemy> scenario_enemiesKilled = new ArrayList<Enemy>();
	private int scenario_eliteEnemiesKilled=0;
	private int scenario_itemUses=0;
	private int scenario_shortRests=0;
	private int scenario_longRests=0;
	private int scenario_experience=0;
	private List<String> scenario_treasureLoot = new ArrayList<String>();
	private int scenario_goldLoot = 0;
	
	//Unique
	private boolean overkill=false;
	private boolean firstBlood=false;
	private boolean singleBlow = false;
	private boolean noEnemiesAround=false;
	private boolean lessHalfHP=false;
	private boolean exhaustion=false;
	private boolean firstToOpenDoor=false;
	
	public StatsTracker() {

	}
	
	public void startScenario() {
		resetScenarioStats();
	}
	
	public void xpGained(int xp) {
		scenario_experience=scenario_experience+xp;
	}
	
	public void addLongRest() {scenario_longRests++;}
	public void addShortRest() {scenario_shortRests++;}
	public void increaseEliteKillCount() {scenario_eliteEnemiesKilled++;}
	public void addKilledEnemy(Enemy enemy) {scenario_enemiesKilled.add(enemy);}
	public void addGold(int amount) {scenario_goldLoot=scenario_goldLoot+amount;}
	public void addTreasure(String treasure) {scenario_treasureLoot.add(treasure);}
	public void increaseItemUse() {scenario_itemUses++;}

	public void endScenario() {
		for(int i=0; i<scenario_enemiesKilled.size(); i++) {
			enemiesKilled.add(scenario_enemiesKilled.get(i));
		}
		
		resetScenarioStats();
	}
	
	public void resetScenarioStats() {
		scenario_experience=0;
		scenario_goldLoot=0;
		scenario_eliteEnemiesKilled=0;
		scenario_itemUses=0;
		scenario_shortRests=0;
		scenario_longRests=0;
		scenario_treasureLoot = new ArrayList<String>();
		scenario_enemiesKilled=new ArrayList<Enemy>();
	}
	
	//Getters and Setters
	public int getScenarioExperience() {return scenario_experience;}
	public int getTotalLoot() {return scenario_treasureLoot.size()+scenario_goldLoot;}
	public int getTreasureLootTotal() {return scenario_treasureLoot.size();}
	public int getGoldLootTotal() {return scenario_goldLoot;}
	public int getNumberOfScenarioKills() {return scenario_enemiesKilled.size();}
	public int getScenarioEliteEnemiesKilled() {return scenario_eliteEnemiesKilled;}
	public int getScenarioItemsUsed() {return scenario_itemUses;}
	public int getScenarioShortRests() {return scenario_shortRests;}
	public int getScenarioLongRests() {return scenario_longRests;}
	public boolean overkillComplete() {return overkill;}
	public void setOverkill(boolean overkill) {this.overkill=overkill;}
	public boolean firstBlood() {return firstBlood;}
	public void setFirstBlood(boolean firstBlood) {this.firstBlood=firstBlood;}
	public boolean singleBlow() {return singleBlow;}
	public void setSingleBlow(boolean singleBlow) {this.singleBlow=singleBlow;}
	public boolean noEnemiesAroundFlag() { return noEnemiesAround;}
	public void setNoEnemiesAroundFlag(boolean noEnemiesAround) {this.noEnemiesAround=noEnemiesAround;}
	public boolean getLessHalfHPFlag() {return lessHalfHP;}
	public void setLessHalfHPFlag(boolean lessHalfHP) {this.lessHalfHP=lessHalfHP;}
	public boolean getExhaustionFlag() {return exhaustion;}
	public void setExhaustionFlag(boolean exhaustion) {this.exhaustion=exhaustion;}
	public boolean getFirstToOpenDoor() {return firstToOpenDoor;}
	public void setFirstToOpenDoor(boolean firstToOpenDoor) {this.firstToOpenDoor=firstToOpenDoor;}

}
