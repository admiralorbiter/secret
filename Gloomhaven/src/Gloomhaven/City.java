package Gloomhaven;

import java.util.ArrayList;
import java.util.List;

public class City {
	private int prospLevel=1;
	private int reputationLevel=0;
	private List<String> globalAchievements = new ArrayList<String>();
	private List<String> partyAchievements = new ArrayList<String>();
	
	public City() {
		
	}
	
	public City(int prospLevel, int reputationLevel) {
		
	}
	
	//Setters and Getters
	public int getProspLevel() {return prospLevel;}
	public void setProspLevel(int level) {this.prospLevel=level;}
	public int getReputationLevel() {return reputationLevel;}
	public void setReputationLevel(int repLevel) {this.reputationLevel=repLevel;}
	public List<String> getGlobalAchievements() {return globalAchievements;}
	public void addGlobalAchievements(String achievement) {globalAchievements.add(achievement);}
	public void changeReputation(int change) {this.reputationLevel=this.reputationLevel-change;}
	public void changeProsperity(int change) {this.prospLevel=this.prospLevel-change;}
	public List<String> getPartyAchievements(){return partyAchievements;}
	public void addPartyAchievement(String achievement) {partyAchievements.add(achievement);}
}