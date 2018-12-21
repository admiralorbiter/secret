package Gloomhaven;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.*;

import Gloomhaven.AbilityCards.PlayerAbilityCard;
import Gloomhaven.AbilityCards.UsePlayerAbilityCard;
import Gloomhaven.Characters.Enemy;
import Gloomhaven.Characters.EnemyInfo;
import Gloomhaven.Characters.Player;
import Gloomhaven.GamePanel.GameState;

public class Scenario {

	public enum State {
	    CARD_SELECTION,
	    INITIATIVE,
	    ATTACK,
	    ENEMY_ATTACK,
	    PLAYER_CHOICE,
	    PLAYER_DEFENSE,
	    PLAYER_DISCARD,
	    ENEMY_DEFENSE,
	    PLAYER_CARD,
	    ROUND_END_DISCARD,
	    ROUND_END_REST,
	    PLAYER_ATTACK_LOGIC,
	    PLAYER_MOVE,
	    PLAYER_ATTACK,
	    PLAYER_LOOT,
	    PLAYER_HEAL,
	    LONG_REST,
	    MINDCONTROL,
	    PLAYER_PUSH_SELECTION,
	    PLAYER_PUSH,
	    PLAYER_ITEM,
	    CREATE_INFUSION,
	    USE_ANY_INFUSION,
	    END;
	}
	
	private State state;																			//State of the scenario
	private List<Player> party = new ArrayList<Player>();											//Current party of players
	private EnemyInfo enemyInfo;																	//Holds the info about the enemies in the scenario
	private Room room;																				//Holds the room data including where enemies and players are											
	private Setting setting = new Setting();
	private InfusionTable elements = new InfusionTable();
	
	private Point oppPoint;
	
	//Key variables
	private char k=' ';																				//Character from key
	private int num=-1;																				//Number from Key
	
	//Variables that are global due to having to refresh and keep the state the same
	private int currentPlayer;																		//Current Player's turn (Used before the round, during initiative and closing the round)
	private int playerIndex;																		//Player being targeted
	private int turn;																				//Turn number (Used during the battle/attack phases so enemies are involved)				
	private Point dimensions;																		//Dimensions of the room
	//private List<Player> targets;																	//Target list created by the enemy
	private String targetID;																		//Player being targeted by the enemy						
	private PlayerAbilityCard card = null;											//Card object used by the player's attack
	private Enemy enemyControlled = null;
	private Enemy enemyTarget = null;
	//Need to refactor - Enemy turn index is held in enemy info, so no need to keep a variable
	private int enemyTurnIndex;		
	private Point tempHoldVar= null;
	private int itemUsed;
	
	public Scenario(String sceneID, List<Player> party) {			
		
		this.party=party;		//imports the party into the scenarios
		for(int i=0; i<party.size(); i++)
			ItemLoader.continuousEffects(party.get(i));
		
		//SetupScenario setup = new SetupScenario(sceneID);	
		//Setups up the room and enemies based on the scene id
		List<Enemy> enemies = new ArrayList<Enemy>();
			
		room = new Room(sceneID, party, enemies);
		enemyInfo=new EnemyInfo(enemies, room);
	
		currentPlayer=0;																			//sets current player to 0 for the card selection around
		enemyInfo.orderEnemies();																	//orders the enemies in list
		dimensions=room.getDimensions();															//Sets dimensions from room
		
		state=State.CARD_SELECTION;
	}
	
	//Returns if the round is finished or still being played
	//[Rem] Needs to be fleshed out
	public boolean finished() {
		return false;
	}
		
	//Function called to play the around, technically plays part of the round so the graphics can be updated
	//[Rem] Else ifs in order to have the graphics update
	public void playRound(KeyEvent key, Graphics g) {
	
		//System.out.println(state);
		
		//List<Item> items = ItemLoader.testLoadAllItems();

		//[Test]
		g.drawString("State of Scenario", setting.getGraphicsX()*5, setting.getGraphicsYTop());
		g.drawString(state.toString(), setting.getGraphicsX()*5, setting.getGraphicsYTop()+15);
		
		
		party.get(0).graphicsPlayerInfo(g);
		elements.graphicsDrawTable(g);
		party.get(0).graphicsDrawCardsInPlay(g);
		
		room.drawBoard(g);																			//Draws current room
		//room.drawRoom(g, party.get(currentPlayer).getCoordinate());
		//room.drawRoom(g, party.get(currentPlayer).getCoordinate(), 2);
		parseKey(key);																				//Parses the input key as either a character or number
	
		//STATE: CARD_SELECTION: Players pick their cards for initiative (or take a rest)--------------------------------------------------------------------------------
		if(state==State.CARD_SELECTION) {
			g.drawString("Picking Cards for your turn.", setting.getGraphicsX(), setting.getGraphicsYTop()+30);
			
			//Allows the user to take a long rest if they have enough in the discard pile 
			if(party.get(currentPlayer).discardPileSize()>1)	
				g.drawString("Take a long rest with "+setting.getRestKey(), setting.getGraphicsX(), setting.getGraphicsYTop()+45);
			
			//Draw's the player's available ability cards
			party.get(currentPlayer).drawAbilityCards(g);			
			
			//Player enters long rest or picks ability cards
			if((k==setting.getRestKey()) && (party.get(currentPlayer).discardPileSize()>1))
				party.get(currentPlayer).setLongRest();
			else
				party.get(currentPlayer).pickAbilityCards(key, num, g);
			
			//Cycles through the players then the enemy picks an ability card
			//Next State: Initiative
			if(party.get(currentPlayer).cardsLocked()) {
				if((currentPlayer+1)!=party.size())
					currentPlayer++;
				else {
					enemyInfo.pickRandomAbilityCard();
					currentPlayer=0;
					state=State.INITIATIVE;
				}
			}
		}
		//State: INITIATIVE: Everyone is ordered based on their initiative-----------------------------------------------------------------------------------------------
		else if(state==State.INITIATIVE) {
	
			int enemyInit=enemyInfo.getInitiative();												//Collect enemy initiative from ability card
			party.sort(Comparator.comparingInt(Player::getInitiative));								//Order just the players based on initiative
			
			//List of all the initiatives for the round
			g.drawString("Initiatives:", 5*setting.getGraphicsX(), setting.getGraphicsYBottom()-30);											
			g.drawString("Enemy: "+String.valueOf(enemyInit), 5*setting.getGraphicsX(), setting.getGraphicsYBottom()-15);								
			for(int i=0; i<party.size();  i++) {
				g.drawString("Player "+party.get(i).getID()+"      "+String.valueOf(party.get(i).getInitiative()), 5*setting.getGraphicsX(), setting.getGraphicsYBottom()+15*i);
			}
			
			/*enemyInfo.setTurnNumber(0);
			//[Temp] Couldn't get the order right so players go first, enemies last
			for(int i=0; i<party.size(); i++)
				party.get(i).setTurnNumber(i+1);
			
			enemyInfo.setTurnNumber(party.size());
			*/
			
			//Goes through the party and enemy and gives a turn number
			//The party is in order, so i just have to fit the enemy in
			//[Rem] Doesn't work if the players have the same init
			for(int i=0; i<party.size(); i++) {

				if(enemyInit==party.get(i).getInitiative()) {
					//Do nothing at the moment
					//[Temp]
				}
				else if(enemyInit<party.get(i).getInitiative()) {		//If enemy's init is lowest, it goes first
					enemyInfo.setTurnNumber(i);
					party.get(i).setTurnNumber(i+1);
				}
				else if(party.get(i).getInitiative()<enemyInit) {	//Sorts player's with lower init before enemy
					party.get(i).setTurnNumber(i);
					enemyInfo.setTurnNumber(i+1);
				}
				else if(party.get(i-1).getInitiative()<enemyInit){	//places the enemy between players if the previous one is lower, but the next is higher
					enemyInfo.setTurnNumber(i);
					party.get(i).setTurnNumber(i+1);
				}
				else {												//Everyone else is placed after the enemy
					party.get(i).setTurnNumber(i+1);
				}

			}
			
			playerIndex=0;																			//playerIndex is reset so it can be used instead of current player
			turn=0;																					//Sets the turn number to 0 and will cyle through all players/enemies
			state=State.ATTACK;																		//Next State: ATTACK
		}
		//State: ATTACK: has the logic for who should be attacking and setting the state for enemy attack, player defense, etc-------------------------------------------
		//Also should know the end state of when attacks are over and round is done
		else if(state==State.ATTACK) {
			//Next State: Enemy Attack
			if(enemyInfo.getTurnNumber()==turn) {													//If enemy turns, do enemy attack
				enemyTurnIndex=0;																	//Resets enemy turn index
				state=State.ENEMY_ATTACK;															//Goes to STATE:ENEMY_ATTACK
			}
			else {
				
				//Next State: Long Rest or Player Choice
				for(int i=0; i<party.size(); i++) {													//Searches for a match on the turn and the players
					if(party.get(i).getTurnNumber()==turn) {										//Once a match is found, sets the index, changes state, and breaks
						if(party.get(i).onRest()) {					
							playerIndex=i;
							party.get(i).resetCardChoice();
							state=State.LONG_REST;
							break;
						}
						else {
							playerIndex=i;
							party.get(i).resetCardChoice();											//Resets card choice so it can be used in player choice when picking cards
							state=State.PLAYER_CHOICE;
							break;
						}
					}
				}
			}
		}
		//State: LONG_REST: Player takes long rest and picks a card to discard-------------------------------------------------------------------------------------------
		else if(state==State.LONG_REST) {
			
			boolean finished=false;																	//Indicates if the round is over
			party.get(playerIndex).takeLongRest(g, num);											//Draws discard pile and has player pick a card and sets long rest to false
			if(party.get(playerIndex).onRest()==false)												//If long rest is over, then the turn is over
				finished=true;
			
			if(finished) {
				turn++;																				//Moves to the next player
				state=State.ATTACK;																	//Next State: Attack
			}
		}
		//State: ENEMY_ATTACK: Goes through all the enemy procedure for attack for all enemies---------------------------------------------------------------------------
		else if(state==State.ENEMY_ATTACK) {
			enemyInfo.drawAbilityCard(g);
			enemyInfo.enemyMoveProcedure(enemyTurnIndex, party, g);
			
			List<Player> targets = new ArrayList<Player>();														//Resets the target list
			targets=enemyInfo.createTargetListForEnemy(enemyTurnIndex, party, g);							//Goes through the enemies and sets range / distance flags
			//once i have enemy pick target, need to change this code slightly.
			if(targets.size()>0) {
				
				int min=100;
				int targetIndex=-1;
				
				for(int i=0; i<targets.size(); i++) {
					if(UtilitiesAB.distance(enemyInfo.getEnemy(enemyTurnIndex).getCoordinates(), party.get(currentPlayer).getCoordinates())<min) {
						targetIndex=i;
					}else if(UtilitiesAB.distance(enemyInfo.getEnemy(enemyTurnIndex).getCoordinates(), party.get(currentPlayer).getCoordinates())==min) {
						if(targets.get(targetIndex).getInitiative()>targets.get(i).getInitiative()) {
							targetIndex=i;
						}
					}
				}
				
				targetID=targets.get(targetIndex).getID();													//[Temp] Picks first one on the list
				if(targets.get(targetIndex).hasRetaliate())
					party.get(currentPlayer).resolveRetaliate(enemyInfo.getEnemy(enemyTurnIndex));
				state=State.PLAYER_DEFENSE;															//Next State: Player Defense
			}else {
				enemyControlLogic();																//Next State: Attack, Enemy Attack, Round End
			}
		}
		//State: PLAYER_DEFENSE: Player chooses to discard or take the damage--------------------------------------------------------------------------------------------
		else if(state==State.PLAYER_DEFENSE) {
			enemyInfo.drawAbilityCard(g);
			g.drawString("Press "+setting.getDiscardKey()+" to discard card or "+setting.getHealKey()+" to take damage.", 5*setting.getGraphicsX(), setting.getGraphicsYBottom());
			int playerIndex = getTargetIndex();														//Sets target
			
			if((k==setting.getHealKey()) || (party.get(playerIndex).abilityCardsLeft()==0)) {						//If player doesn't discard (or can't), take damage
				int damage=enemyInfo.getAttack(enemyTurnIndex);										//Retrieves enemy's attack
				party.get(playerIndex).takeDamage(damage);										//Decreases player health
				if(party.get(playerIndex).getCharacterData().getHealth()<=0) {											//If player's health is below 0, kill player
					party.remove(playerIndex);
				}
				if(party.size()==0)
					state=State.ROUND_END_DISCARD;													//If dead, Next State: Round End
				else
					enemyControlLogic();															//Next State: Attack, Enemy Attack, Round End
			}
			
			if(k==setting.getDiscardKey()) {
				state=State.PLAYER_DISCARD;															//Next State: Player Discard
			}
		}
		//State: PLAYER_DISCARD: Player discards a card instead of taking damage
		else if(state==State.PLAYER_DISCARD) {
			
			if(party.get(playerIndex).discardForHealth(num, g))										//Prints ability cards then waits until one is picked. 
				enemyControlLogic();																//Next State: Attack, Enemy Attack, Round End
		}
		//State: PLAYER_CHOICE: Player chooses their card
		else if(state==State.PLAYER_CHOICE) {

			int cardPick=party.get(playerIndex).pickPlayCard(key, num, k, g);								//Prints ability cards then waits for one to pick
			if(cardPick>=1 && cardPick<=8)
				state=State.PLAYER_ATTACK_LOGIC;													//Next State: Player Attack Logic
			if(cardPick>=100) {
				itemUsed=cardPick-100;
				state=State.PLAYER_ITEM;
			}
		}
		//State: PLAYER_ATTACK_LOGIC: Player picks card, then uses data for next state-----------------------------------------------------------------------------------
		else if(state==State.PLAYER_ATTACK_LOGIC) {
			card = party.get(playerIndex).playCard();												//Card Picked by the player
			room.setSelectionCoordinates(party.get(playerIndex).getCoordinates());					//Sets selection coordinates based on player
			
			g.drawString("Move "+UsePlayerAbilityCard.getMove(card)+"     Attack: "+UsePlayerAbilityCard.getAttack(card)+"  (Loc: Scenario -Player Attack Logic)", 5*setting.getGraphicsX(), setting.getGraphicsYBottom());
			
			UtilitiesAB.resolveCard(party.get(playerIndex), card, elements, room);
			//Next State: Player Move, Player attack, Back to Attack, or End Turn
			if(UsePlayerAbilityCard.getMove(card)>0) {
				state=State.PLAYER_MOVE;
			//if there is range? that way it hits all targed attacks
			//}else if(UsePlayerAbilityCard.getAttack(card)>0 || UsePlayerAbilityCard.hasTargetHeal(card)==true) {
			}else if(UsePlayerAbilityCard.getCardData(card).getElementalConsumed()) {
				state=State.USE_ANY_INFUSION;
			}else if(UsePlayerAbilityCard.getRange(card)>0 || UsePlayerAbilityCard.getAttack(card)>0) {
				state=State.PLAYER_ATTACK;
			}else {
				if(party.get(currentPlayer).getCardChoice()==false) {
					state=State.PLAYER_CHOICE;
				}else {
					//if turn is over
					if(turn==party.size())
						state=State.ROUND_END_DISCARD;
					else {
						turn++;
						state=State.ATTACK;
					}
				}
			}
		}
		else if(state==State.PLAYER_ITEM) {
			//picked item stored in itemUsed as an int
			List<Item> usableItems = ItemLoader.onTurn(party.get(currentPlayer).getItems());
		
			if(usableItems.get(itemUsed).getConsumed()) {
				ItemLoader.consumeItem(party.get(currentPlayer), usableItems.get(itemUsed));
			}else if(usableItems.get(itemUsed).getSpent()) {
				ItemLoader.spendItem(party.get(currentPlayer), usableItems.get(itemUsed));
			}
			if(party.get(currentPlayer).getCreateAnyElement()) {
				state=State.CREATE_INFUSION;
			}else {
				state=State.PLAYER_CHOICE;
			}
		}
		//State: PLAYER_MOVE: Player moves to a new hex or stays there---------------------------------------------------------------------------------------------------
		else if(state==State.PLAYER_MOVE) {
			boolean finished=false;
			if(party.get(playerIndex).canMove()){
				//Highlight tiles that players can move to
				Point playerPoint=party.get(playerIndex).getCoordinates();
				for(int r=1; r<=UsePlayerAbilityCard.getMove(card); r++) {
					room.drawRange(g, playerPoint, r, Color.BLUE);
				}
		
				//Moves selection highlight
				g.drawString("Press "+setting.getMoveKey()+" to move.", setting.getGraphicsX(), setting.getGraphicsYBottom());
				selection(g);
				
				//Player moves if the space is empty or the space hasn't changed
				if(k==setting.getMoveKey()) {
					if(room.isSpace(room.getSelectionCoordinates(), "P")) {
						finished=true;
					}
					else if(UsePlayerAbilityCard.hasFlying(card)) {
						//NEED TO HANDLE MULTIPLE PEOPLE OR THINGS ON A HEX
						room.movePlayer(party.get(playerIndex), room.getSelectionCoordinates());
						party.get(playerIndex).setCoordinates(new Point(room.getSelectionCoordinates()));
						finished=true;
					}
					else if(UsePlayerAbilityCard.hasJump(card)) {
						if(room.isSpaceEmpty(room.getSelectionCoordinates())) {
							if(room.getQuickID(room.getSelectionCoordinates())=="Loot")
								room.loot(party.get(playerIndex), room.getSelectionCoordinates());
							room.movePlayer(party.get(playerIndex), room.getSelectionCoordinates());
							party.get(playerIndex).setCoordinates(new Point(room.getSelectionCoordinates()));
							finished=true;
						}
	
					}else {
						//NEED TO ADD IN A CHECK FOR PATH IF JUMP IS NOT TRUE
						if(room.isSpaceEmpty(room.getSelectionCoordinates())) {
							room.movePlayer(party.get(playerIndex), room.getSelectionCoordinates());
							party.get(playerIndex).setCoordinates(new Point(room.getSelectionCoordinates()));
							finished=true;
						}
					}
				}
			}
			else {
				finished=true;
			}
			//Next State: Player Attack, Attack Logic, Round End
			if(finished) {
				if(UsePlayerAbilityCard.getCardData(card).getElementalConsumed()) {
					state=State.USE_ANY_INFUSION;
				}
				else if(UsePlayerAbilityCard.getRange(card)>0 || UsePlayerAbilityCard.getAttack(card)>0) {
					//room.setSelectionCoordinates(new Point(room.getSelectionCoordinates()));		//Resets selection coordinates
					state=State.PLAYER_ATTACK;
				}else if(UsePlayerAbilityCard.getCardData(card).getPush()>0) {
					//room.setSelectionCoordinates(new Point(room.getSelectionCoordinates()));		//Resets selection coordinates
					state=State.PLAYER_PUSH_SELECTION;
				}else {
					if(party.get(currentPlayer).getCardChoice()==false) {
						state=State.PLAYER_CHOICE;
					}else {
						//if turn is over
						if(turn==party.size())
							state=State.ROUND_END_DISCARD;
						else {
							turn++;
							state=State.ATTACK;
						}
					}
				}
			}	
		}
		//State: PLAYER_ATTACK: Creates target list and has player select a target to attack-----------------------------------------------------------------------------
		else if(state==State.PLAYER_ATTACK) {
			boolean finished=false;
			if(party.get(playerIndex).canAttack()) {
				g.drawString("Press "+setting.getTargetKey()+" to target.", setting.getGraphicsX(), setting.getGraphicsYBottom());
				
				//Creates target list of enemy coordinates
				List<Point> targets = new ArrayList<Point>();
				int cardRange=UsePlayerAbilityCard.getRange(card);
				if(UsePlayerAbilityCard.getRange(card)>=0) {
					if(UsePlayerAbilityCard.getRange(card)==0)
						cardRange=1;
		
					if(UsePlayerAbilityCard.hasTargetHeal(card)) {
						for(int range=1; range<=cardRange; range++)
							targets=UtilitiesTargeting.createTargetList(room.getBoard(), range, party.get(playerIndex).getCoordinates(), "P", room.getDimensions());
						targets.add(party.get(playerIndex).getCoordinates());
					}
					else {
						for(int range=1; range<=cardRange; range++)
							targets=UtilitiesTargeting.createTargetList(room.getBoard(), range, party.get(playerIndex).getCoordinates(), "E", room.getDimensions());
					}
				}
				
				//If there are targets, highlight the targets and wait for selection
				if(targets.size()>0) {
	
					room.highlightTargets(targets, g);
					selection(g);
					
					//Space is used for selection of target
					if(k==setting.getTargetKey()) {
						
						
						if(UsePlayerAbilityCard.hasTargetHeal(card)) {
							if(room.isSpace(room.getSelectionCoordinates(), "P")) {							//If the space selected has an enemy
								if(targets.contains(room.getSelectionCoordinates())){						//If the target is in range
									String id = room.getID(room.getSelectionCoordinates());
									for(int i=0; i<party.size(); i++)
									{
										if(party.get(i).getID()==id) {
											party.get(i).heal(UsePlayerAbilityCard.getHeal(card));
											finished=true;
										}
									}
								}
							}
						}
						else if(UsePlayerAbilityCard.getCardData(card).getSemiCircle() || UsePlayerAbilityCard.getCardData(card).getSortOfSemiCircle()) {
							
							int pointFlag=UtilitiesAB.getPointFlag(room.getSelectionCoordinates(), party.get(currentPlayer).getCoordinates());
							Point selectionCoordinates = room.getSelectionCoordinates();
							attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()), targets);
							if(pointFlag==0)
							{
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()+1), targets);
								if(UsePlayerAbilityCard.getCardData(card).getSemiCircle())
									attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()), targets);
							}
							else if(pointFlag==1) {
								if(UsePlayerAbilityCard.getCardData(card).getSemiCircle())
									attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()-1), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()+1), targets);
							}
							else if(pointFlag==2) {
								if(UsePlayerAbilityCard.getCardData(card).getSemiCircle())
									attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()-1), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()), targets);
							}
							else if(pointFlag==3) {
								if(UsePlayerAbilityCard.getCardData(card).getSemiCircle())
									attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()-1), targets);
							}
							else if(pointFlag==4) {
								if(UsePlayerAbilityCard.getCardData(card).getSemiCircle())
									attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()+1), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()-1), targets);
							}
							else if(pointFlag==5) {
					
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()), targets);
								if(UsePlayerAbilityCard.getCardData(card).getSemiCircle())
									attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()+1), targets);
							}
							finished=true;
						}
						else if(UsePlayerAbilityCard.getCardData(card).getOpposingAttack()) {
							int pointFlag=UtilitiesAB.getPointFlag(room.getSelectionCoordinates(), party.get(currentPlayer).getCoordinates());
							Point selectionCoordinates = room.getSelectionCoordinates();
							attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()), targets);
							if(pointFlag==0)
							{
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()+2), targets);
							}
							else if(pointFlag==1) {
								attackProcedure(new Point((int)selectionCoordinates.getX()+2, (int)selectionCoordinates.getY()), targets);
							}
							else if(pointFlag==2) {
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()-2), targets);
							}
							else if(pointFlag==3) {
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()-2), targets);
							}
							else if(pointFlag==4) {
								attackProcedure(new Point((int)selectionCoordinates.getX()-2, (int)selectionCoordinates.getY()), targets);
							}
							else if(pointFlag==5) {
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()+2), targets);
							}
							finished=true;
						}
						else if(UsePlayerAbilityCard.getCardData(card).getCircle()) {
							Point start = room.getSelectionCoordinates();
							int range=1;
							for(int x=-range; x<=range; x++) {
								for(int y=-range; y<=range; y++) {
									for(int z=-range; z<=range; z++) {
										if(x+y+z==0) {																	//If the x,y,z axial coordinate's are equal to zero
											Point convertedPoint = new Point();
											
											//Converts cube coord to a coord to plot
											//https://www.redblobgames.com/grids/hexagons/#conversions
											if(start.getY()%2!=0) {													//If the column is odd use odd conversion
												int tempx=x+(z+(z&1))/2;
												int tempy=z;
												
												convertedPoint=new Point(tempx, tempy);;
											}
											else {
												int tempx=x+(z-(z&1))/2;
												int tempy=z;
											
												convertedPoint=new Point(tempx, tempy);;
											}							
											
											//Plotted point is equal to the converted point + player point
											int xToPlot=(int)(convertedPoint.getX()+start.getX());					
											int yToPlot=(int) (convertedPoint.getY()+start.getY());
											
											//Checks that the plotted x and y are inside the dimensions
											if(xToPlot>=0 && xToPlot<dimensions.getX()) 
												if(yToPlot>=0 && yToPlot<dimensions.getY()) {
													attackProcedure(new Point(xToPlot, yToPlot),targets);
												}
										}
									}
								}
							}
						}
						else if(UsePlayerAbilityCard.getCardData(card).getTriangle()) {
							int pointFlag=UtilitiesAB.getPointFlag(room.getSelectionCoordinates(), party.get(currentPlayer).getCoordinates());
							Point selectionCoordinates = room.getSelectionCoordinates();
							attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()), targets);
							if(pointFlag==0)
							{
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()+1), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()), targets);
							}
							else if(pointFlag==1) {
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()+1), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()+1), targets);
							}
							else if(pointFlag==2) {
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()+1), targets);
							}
							else if(pointFlag==3) {
								attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()-1), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()+1), targets);
							}
							else if(pointFlag==4) {
								attackProcedure(new Point((int)selectionCoordinates.getX(), (int)selectionCoordinates.getY()-1), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX()+1, (int)selectionCoordinates.getY()-1), targets);
							}
							else if(pointFlag==5) {
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()), targets);
								attackProcedure(new Point((int)selectionCoordinates.getX()-1, (int)selectionCoordinates.getY()-1), targets);
							}
							finished=true;
						}
						else {
							if(room.isSpace(room.getSelectionCoordinates(), "E")) {							//If the space selected has an enemy
								if(targets.contains(room.getSelectionCoordinates())){						//If the target is in range
									//String id = room.getID(room.getSelectionCoordinates());					//Get id of the enemy
									//int damage=party.get(currentPlayer).getAttack(card);					//Get attack of the player
									//enemyInfo.playerAttack(id, damage);
									if(UsePlayerAbilityCard.hasMindControl(card)) {
										enemyControlled=enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates()));
										state=State.MINDCONTROL;
									}
									else {
										boolean adjacentBonus=UsePlayerAbilityCard.getCardData(card).getAdjacentBonus();
							
										if(adjacentBonus)
											adjacentBonus=UtilitiesTargeting.targetAdjacentToAlly(enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())), party, playerIndex, room);
									
										UtilitiesAB.resolveAttack(enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())), party.get(playerIndex), UsePlayerAbilityCard.getCardData(card), room, adjacentBonus, elements);
										
										card.increaseCounter();
										
										if(UsePlayerAbilityCard.getCardData(card).getTargetNum()>1 && card.getCounter()!=UsePlayerAbilityCard.getCardData(card).getTargetNum()) {
											System.out.println("1:  "+targets);
											targets.remove(targets.indexOf(room.getSelectionCoordinates()));
											System.out.println("2:  "+targets);
										}else {
											
											if(UsePlayerAbilityCard.getCardData(card).getFlag().equals("forEachTargeted")) {
												
											}
											
											card.resetCounter();
											finished=true;	
										}
									}
								}
							}
						}
					}
				}else {																					//If there are no enemies in target range
					finished=true;
				}
			}
			else {
				finished=true;
			}
			//Next State: Next card, Attack Logic, End Round
			if(finished) {
				if(UsePlayerAbilityCard.getCardData(card).getPush()>0) {
					//room.setSelectionCoordinates(new Point(room.getSelectionCoordinates()));		//Resets selection coordinates
					state=State.PLAYER_PUSH_SELECTION;
				}else {
					if(party.get(currentPlayer).getCardChoice()==false) {
						state=State.PLAYER_CHOICE;
					}else {
						//if turn is over
						if(turn==party.size())
							state=State.ROUND_END_DISCARD;
						else {
							turn++;
							state=State.ATTACK;
						}
					}
				}
			}
		}
		else if(state==State.PLAYER_PUSH_SELECTION) {
			boolean finished=false;
			//Creates target list of enemy coordinates
			List<Point> targets = new ArrayList<Point>();
			int cardRange=UsePlayerAbilityCard.getRange(card);
			if(UsePlayerAbilityCard.getRange(card)>=0) {
				if(UsePlayerAbilityCard.getRange(card)==0)
					cardRange=1;
	
					for(int range=1; range<=cardRange; range++)
						targets=UtilitiesTargeting.createTargetList(room.getBoard(), range, party.get(currentPlayer).getCoordinates(), "E", dimensions);
			}
			
			//If there are targets, highlight the targets and wait for selection
			if(targets.size()>0) {
				
				room.highlightTargets(targets, g);
				selection(g);
				//Space is used for selection of target
				if(k==setting.getTargetKey()) {
					if(room.isSpace(room.getSelectionCoordinates(), "E")) {							//If the space selected has an enemy
						if(targets.contains(room.getSelectionCoordinates())){						//If the target is in range
							oppPoint = new Point(UtilitiesTargeting.findOppisiteHex(party.get(currentPlayer).getCoordinates(), enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())).getCoordinates()));
							enemyTarget=enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates()));
							tempHoldVar=new Point(enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())).getCoordinates());
							state=State.PLAYER_PUSH;
						}
					}
				}
			}
			else {
				finished=true;
			}
			if(finished) {
				if(party.get(currentPlayer).getCardChoice()==false) {
					state=State.PLAYER_CHOICE;
				}else {
					//if turn is over
					if(turn==party.size())
						state=State.ROUND_END_DISCARD;
					else {
						turn++;
						state=State.ATTACK;
					}
				}
			}
		}
		else if(state==State.PLAYER_PUSH) {
			boolean finished=false;
			Point pointToMove = null;
			int pointFlag = -1;
			g.setColor(Color.MAGENTA);
			room.drawHex(g, (int)oppPoint.getX(), (int)oppPoint.getY());
			//UtilitiesAB.drawArrows(g, new Point(party.get(currentPlayer).getCoordinate()), oppPoint);
			g.drawString("Press 1, 2, 3.", 5*setting.getGraphicsX(), setting.getGraphicsYBottom());
			pointFlag=UtilitiesAB.getPointFlag(party.get(currentPlayer).getCoordinates(), oppPoint);
			
			if(num>=1 && num<=3) {
				
				if(pointFlag==0) {
					if(num==1) {
						pointToMove=new Point((int)oppPoint.getX()+1, (int)oppPoint.getY());
						//oppPoint=new Point((int)pointToMove.getX()+1, (int)pointToMove.getY());
						finished=true;
					}
					else if(num==2) {
						pointToMove = new Point(oppPoint);
						finished=true;
					}
					else if(num==3) {
						pointToMove=new Point((int)oppPoint.getX(), (int)oppPoint.getY()+1);
						finished=true;
					}
				}
				if(pointFlag==1) {
					if(num==1) {
						pointToMove=new Point((int)oppPoint.getX(), (int)oppPoint.getY());
						finished=true;
					}
					else if(num==2) {
						pointToMove=new Point(oppPoint);
						finished=true;
					}
					else if(num==3) {
						pointToMove=new Point((int)oppPoint.getX(), (int)oppPoint.getY()+1);
						finished=true;
					}
				}
				else if(pointFlag==2) {
					if(num==1) {
						pointToMove=new Point((int)oppPoint.getX(), (int)oppPoint.getY()-1);
						finished=true;
					}
					else if(num==2) {
						pointToMove=new Point(oppPoint);
						finished=true;
					}
					else if(num==3) {
						pointToMove=new Point((int)oppPoint.getX()+1, (int)oppPoint.getY());
						finished=true;
					}
				}
				else if(pointFlag==4) {
					if(num==1) {
						pointToMove=new Point((int)oppPoint.getX()-1, (int)oppPoint.getY()-1);
						finished=true;
					}
					else if(num==2) {
						pointToMove=new Point(oppPoint);
						finished=true;
					}
					else if(num==3) {
						pointToMove=new Point((int)oppPoint.getX()+1, (int)oppPoint.getY()+1);
						finished=true;
					}
				}
				else if(pointFlag==3 || pointFlag==5) {
					if(num==1) {
						pointToMove=new Point((int)oppPoint.getX()-1, (int)oppPoint.getY());
						finished=true;
					}
					else if(num==2) {
						pointToMove=new Point(oppPoint);
						finished=true;
					}
					else if(num==3) {
						pointToMove=new Point((int)oppPoint.getX(), (int)oppPoint.getY()+1);
						finished=true;
					}
				}
			}
			
			if(finished) {
				UsePlayerAbilityCard.getCardData(card).increasePush();
				room.moveEnemy(enemyTarget, pointToMove);
				oppPoint = new Point(UtilitiesTargeting.findOppisiteHex(tempHoldVar, pointToMove));
				tempHoldVar=new Point(pointToMove);

				if(UsePlayerAbilityCard.getCardData(card).getPush()>UsePlayerAbilityCard.getCardData(card).getPushCount())
				{
					//oppPoint=new Point(pointToMove);
					state=State.PLAYER_PUSH;
				}else {
					if(party.get(currentPlayer).getCardChoice()==false) {
						state=State.PLAYER_CHOICE;
					}else {
						//if turn is over
						if(turn==party.size())
							state=State.ROUND_END_DISCARD;
						else {
							turn++;
							state=State.ATTACK;
						}
					}
				}
			}
		}
		else if(state==State.MINDCONTROL) {
			boolean finished=false;
			if(UsePlayerAbilityCard.getCardData(card).getMindControData().getMove()>0)
				finished=mindControlMove(party.get(playerIndex), enemyControlled, g);
			
			if(UsePlayerAbilityCard.getCardData(card).getMindControData().getAttack()>0)
				finished=mindControlAttack(party.get(playerIndex), enemyControlled, g);
			
			//Next State: Next card, Attack Logic, End Round
			if(finished) {
				if(party.get(currentPlayer).getCardChoice()==false) {
					state=State.PLAYER_CHOICE;
				}else {
					//if turn is over
					if(turn==party.size())
						state=State.ROUND_END_DISCARD;
					else {
						turn++;
						state=State.ATTACK;
					}
				}
			}
		}
		else if(state==State.CREATE_INFUSION) {
			int startingY=setting.getGraphicsYBottom();
			int offsetY=15;
			
			g.drawString("Pick an infusion", 10, startingY+offsetY*0);
			g.drawString("1 Fire", 10, startingY+offsetY*1);
			g.drawString("2 Ice", 10, startingY+offsetY*2);
			g.drawString("3 Air", 10, startingY+offsetY*3);
			g.drawString("4 Earth", 10, startingY+offsetY*4);
			g.drawString("5 Light", 10, startingY+offsetY*5);
			g.drawString("6 Dark", 10, startingY+offsetY*6);
			
			if(num>=1 && num<=6) {
				String element="";
				switch(num) {
					case 1: element="Fire";
					break;
					case 2: element="Ice";
					break;
					case 3: element="Air";
					break;
					case 4: element="Earth";
					break;
					case 5: element="Light";
					break;
					case 6: element="Dark";
					break;
				}
				elements.infuse(element);
				
				party.get(currentPlayer).setCreateAnyElement(false);
				
				state=State.PLAYER_CHOICE;
			}
			
		}
		else if(state==State.USE_ANY_INFUSION) {
			if(elements.consumeAny(g, num)) {
				if(UsePlayerAbilityCard.getRange(card)>0 || UsePlayerAbilityCard.getAttack(card)>0) {
					//room.setSelectionCoordinates(new Point(room.getSelectionCoordinates()));		//Resets selection coordinates
					state=State.PLAYER_ATTACK;
				}else if(UsePlayerAbilityCard.getCardData(card).getPush()>0) {
					//room.setSelectionCoordinates(new Point(room.getSelectionCoordinates()));		//Resets selection coordinates
					state=State.PLAYER_PUSH_SELECTION;
				}else {
					if(party.get(currentPlayer).getCardChoice()==false) {
						state=State.PLAYER_CHOICE;
					}else {
						//if turn is over
						if(turn==party.size())
							state=State.ROUND_END_DISCARD;
						else {
							turn++;
							state=State.ATTACK;
						}
					}
				}
			}
		}
		//State: ROUND_END_DISCARD: Decides if scenario is over or another round should begin----------------------------------------------------------------------------
		else if(state==State.ROUND_END_DISCARD) {
			
			elements.endOfRound();
			
			for(int i=0; i<party.size(); i++)
				party.get(i).endTurn();																//End of turn clean up for each player
			
			if(party.size()==0) 
				System.exit(1);																		//If party is dead, end program
			
			if(enemyInfo.getCount()==0)
				System.exit(1);																		//If all enemies are dead, end program
			
			currentPlayer=0;																		//Resets current player to use in round end rest state
			state=State.ROUND_END_REST;																//If game isn't immediately over, next state: round end rest
		}
		//State: ROUND_END_REST:Gives the player an option of a short rest-----------------------------------------------------------------------------------------------
		else if(state==State.ROUND_END_REST) { 	
			
			boolean finished=false;
			
			//If player has enough in the discard pile give option of short rest
			if(party.get(currentPlayer).discardPileSize()>1) {
				party.get(currentPlayer).shortRestInfo(g);											//Short rest shuffles back discard pile and randomly discards a card
				
				if(k=='y') {																		//Takes rest, moves on to next player or finishes round
					party.get(currentPlayer).takeShortRest();
					if((currentPlayer+1)!=party.size())	
						currentPlayer++;
					else
						finished=true;
				}
				
				if(k=='n') {																		//Doesn't take rest, moves on to next player or finishes round
					if((currentPlayer+1)!=party.size())
						currentPlayer++;
					else
						finished=true;
				}
			}else {
				finished=true;
			}
			
			if(finished) {
				for(int i=0; i<party.size(); i++)
					party.get(i).reset();														//Resets card variables for party
				turn=0;																				//Resets turn
				currentPlayer=0;																	//Resets currentPlayer
				state=State.CARD_SELECTION;															//Next State: Card Selection (Back to beginning)
			}
		}	
	}
	
	//Parses key event into either a character or a number to be used
	private void parseKey(KeyEvent key) {
		k=Character.MIN_VALUE;
		try{
			k = key.getKeyChar();
			}catch(NullPointerException ex){ 														// handle null pointers for getKeyChar
			   
			}
		
		num = -1;
		try{
			num=Integer.parseInt(String.valueOf(k));  
			}catch(NumberFormatException ex){ 														// handle if it isn't a number character
			   
			}
	}

	//Selection function: draws current hex selected and moves selection
	private void selection(Graphics g) {
		//room.drawSelectionHex(g);																	//Draws selection hex
		
		//String direction = "north";
		//room.drawSelectionHex(g, 2, UtilitiesAB.getPointFlag(party.get(currentPlayer).getCoordinate(), room.getSelectionCoordinates()));
		if(UsePlayerAbilityCard.getCardData(card).getSemiCircle())
			room.drawSelectionHexSemiCircle(g, UtilitiesAB.getPointFlag(room.getSelectionCoordinates(), party.get(currentPlayer).getCoordinates()));
		else if(UsePlayerAbilityCard.getCardData(card).getSortOfSemiCircle())
			room.drawSelectionHexAdjCircle(g, UtilitiesAB.getPointFlag(room.getSelectionCoordinates(), party.get(currentPlayer).getCoordinates()));
		else if(UsePlayerAbilityCard.getCardData(card).getOpposingAttack()) 
			room.drawSelectionHexAdjOpposing(g, UtilitiesAB.getPointFlag(room.getSelectionCoordinates(), party.get(currentPlayer).getCoordinates()));
		else if(UsePlayerAbilityCard.getCardData(card).getCircle())
			room.drawRange(g, room.getSelectionCoordinates(), 1, Color.BLUE);
		else if(UsePlayerAbilityCard.getCardData(card).getTriangle())
			room.drawSelectionHexTriangle(g, UtilitiesAB.getPointFlag(room.getSelectionCoordinates(), party.get(currentPlayer).getCoordinates()));
		else
			room.drawSelectionHex(g);
		
		delayBy(100);																				//Makes it feel more smoother
		Point selectTemp=new Point(room.getSelectionCoordinates());									//[Rem] Probably more efficient to move iff it is changed	
		
		//Checks that new coordinate is inside the current room
		if(k==setting.up()) {
			if(selectTemp.y-1>=0) {
				selectTemp.y=selectTemp.y-1;
			}
			
		}
		if(k==setting.left()) {
			if(selectTemp.x-1>=0) {
				selectTemp.x=selectTemp.x-1;
			}
		}
		if(k==setting.down()) {
			if(selectTemp.y+1<room.getDimensions().getY()) {
				selectTemp.y=selectTemp.y+1;
			}
		}
		if(k==setting.right()) {
			if(selectTemp.x+1<room.getDimensions().getX()) {
				selectTemp.x=selectTemp.x+1;
			}
		}
		
		room.setSelectionCoordinates(selectTemp);													//Changes selection coordinate to new coordinate
	}
	
	//Controls the enemy logic at the end of the enemy round
	private void enemyControlLogic() {
		
		if(enemyTurnIndex==(enemyInfo.getCount()-1)) {												//If it has gone through all the enemies, go to next state
			if(turn==party.size())																	//If if it is on the last turn, End Round
				state=State.ROUND_END_DISCARD;														
			else {
				turn++;																				//End turn go back to attack logic state
				state=State.ATTACK;
			}	
		}
		else {
			enemyTurnIndex++;																		//Cycle through enemies and go to enemy attack state
			state=State.ENEMY_ATTACK;
		}
	}
	
	//Function that delays for a certain amount of seconds
	//Used to slow down the selection function
	private void delayBy(int time) {
		try {
			TimeUnit.MILLISECONDS.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
		
	//Returns the playerIndex based on the targetID
	private int getTargetIndex() {
		for(int index=0; index<party.size(); index++) {												//Cycles through the party
			if(party.get(index).getID()==targetID) {												//When it matches ID, sets the player index and returns
				return index;
			}
		}
		return -1;																					//Only returns a -1 if there is an error
	}
	
	//[Test] Function to print order of the init
	private void testPrintTurnList() {
		//[Test]
		for(int i=0; i<party.size()+1; i++) {
			for(int j=0; j<party.size(); j++) {
				if(party.get(j).getTurnNumber()==i)
					System.out.println(i+" - "+party.get(j).getInitiative());
				else
					System.out.println(i+" - "+enemyInfo.getTurnNumber());
			}
		}
	}
	
	//[Test] Function to print the enemy targets
	private void testPrintEnemyTargets(List<Player> targets) {
		//[Test]
		System.out.println("Targets for Enemy "+enemyTurnIndex+":");
		for(int i=0; i<targets.size(); i++) {
			System.out.println(i+": "+targets);
		}
		System.out.println("");
	}
	
	private boolean mindControlMove(Player player, Enemy enemy, Graphics g) {
		boolean finished=false;

		
		//Highlight tiles that players can move to
		Point enemyPoint=enemy.getCoordinates();
		SimpleCards cardData = UsePlayerAbilityCard.getCardData(card).getMindControData();
		
		for(int r=1; r<=cardData.range; r++) {
			room.drawRange(g, enemyPoint, r, Color.BLUE);
		}

		//Moves selection highlight
		g.drawString("Press "+setting.getMoveKey()+"to move.", setting.getGraphicsX(), setting.getGraphicsYBottom());
		selection(g);
		
		//Player moves if the space is empty or the space hasn't changed
		if(k==setting.getMoveKey()) {
			if(room.isSpace(room.getSelectionCoordinates(), "E")) {
				return true;
			}
			else if(UsePlayerAbilityCard.hasFlying(card)) {
				//NEED TO HANDLE MULTIPLE PEOPLE OR THINGS ON A HEX
				room.moveEnemy(enemy, room.getSelectionCoordinates());
				enemy.setCoordinates(new Point(room.getSelectionCoordinates()));
				return true;
			}
			else if(UsePlayerAbilityCard.hasJump(card)) {
				if(room.isSpaceEmpty(room.getSelectionCoordinates())) {
					room.moveEnemy(enemy, room.getSelectionCoordinates());
					enemy.setCoordinates(new Point(room.getSelectionCoordinates()));
					return true;
				}

			}else {
				//NEED TO ADD IN A CHECK FOR PATH IF JUMP IS NOT TRUE
				if(room.isSpaceEmpty(room.getSelectionCoordinates())) {
					room.moveEnemy(enemy, room.getSelectionCoordinates());
					enemy.setCoordinates(new Point(room.getSelectionCoordinates()));
					return true;
				}
			}
			return false;
		}
		return false;
	}
	
	private boolean mindControlAttack(Player player, Enemy enemy, Graphics g) {
		boolean finished=false;
		
		g.drawString("Press "+setting.getTargetKey()+" to target.", setting.getGraphicsX(), setting.getGraphicsYBottom());
		
		//Creates target list of enemy coordinates
		List<Point> targets = new ArrayList<Point>();
		int cardRange=UsePlayerAbilityCard.getCardData(card).getMindControData().getRange();
		if(cardRange>=0) {
			if(cardRange==0)
				cardRange=1;
	
				for(int range=1; range<=cardRange; range++)
					targets=UtilitiesTargeting.createTargetList(room.getBoard(), range, enemy.getCoordinates(), "E", room.getDimensions());
		}
		
		//If there are targets, highlight the targets and wait for selection
		if(targets.size()>0) {

			room.highlightTargets(targets, g);
			selection(g);
			
			//Space is used for selection of target
			if(k==setting.getTargetKey()) {
				if(room.isSpace(room.getSelectionCoordinates(), "E")) {							//If the space selected has an enemy
					if(targets.contains(room.getSelectionCoordinates())){						//If the target is in range
							UtilitiesAB.resolveAttackEnemyOnEnemy(enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())), enemy, UsePlayerAbilityCard.getCardData(card).getMindControData().getAttack());
							return true;
					}
				}
				return false;
			}
			else {
				return false;
			}
		}else {																					//If there are no enemies in target range
			return true;
		}
	}
	
	private void attackProcedure(Point attackCoordinate, List<Point> targets) {
		if(room.isSpace(attackCoordinate, "E")) {							//If the space selected has an enemy
			if(targets.contains(attackCoordinate)){						//If the target is in range
	
				boolean adjacentBonus=UsePlayerAbilityCard.getCardData(card).getAdjacentBonus();
	
				if(adjacentBonus)
					adjacentBonus=UtilitiesTargeting.targetAdjacentToAlly(enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())), party, playerIndex, room);
			
				UtilitiesAB.resolveAttack(enemyInfo.getEnemyFromID(room.getID(attackCoordinate)), party.get(playerIndex), UsePlayerAbilityCard.getCardData(card), room, adjacentBonus, elements);
			
			}
		}
	}
	
}
