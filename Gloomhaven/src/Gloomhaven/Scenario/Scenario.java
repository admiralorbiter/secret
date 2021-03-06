package Gloomhaven.Scenario;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import Gloomhaven.Shop;
import Gloomhaven.AbilityCards.PlayerAbilityCard;
import Gloomhaven.AbilityCards.UsePlayerAbilityCard;
import Gloomhaven.CardDataObject.CardDataObject;
import Gloomhaven.Characters.Enemy;
import Gloomhaven.Characters.EnemyInfo;
import Gloomhaven.Characters.Player;
import Gloomhaven.Hex.Draw;
import Gloomhaven.Hex.Hex;
import Gloomhaven.Hex.HexCoordinate;
import Gloomhaven.Hex.UtilitiesHex;
import Unsorted.BossMoves;
import Unsorted.City;
import Unsorted.GUI;
import Unsorted.GUIScenario;
import Unsorted.InfusionTable;
import Unsorted.Item;
import Unsorted.ItemLoader;
import Unsorted.Setting;
import Unsorted.UtilitiesAB;
import Unsorted.UtilitiesBoard;
import Unsorted.UtilitiesGeneral;
import Unsorted.UtilitiesLoot;
import Unsorted.UtilitiesTargeting;

public class Scenario implements Serializable{
	public enum State {
	    CARD_SELECTION,
	    INITIATIVE,
	    ATTACK,
	    ENEMY_ATTACK,
	    ENEMY_CONTROL_LOGIC,
	    PLAYER_CHOICE,
	    PLAYER_DEFENSE,
	    PLAYER_DISCARD,
	    ROUND_END_DISCARD,
	    ROUND_END_REST,
	    PLAYER_ATTACK_LOGIC,
	    PLAYER_MOVE,
	    PLAYER_ATTACK,
	    LONG_REST,
	    PLAYER_PUSH_SELECTION,
	    PLAYER_PUSH,
	    PLAYER_ITEM,
	    CREATE_INFUSION,
	    USE_ANY_INFUSION,
	    MINDCONTROL,
	}
	
	private City gloomhaven;																						//City Data Object
	private Shop shop;																								//Shop Data Object
	private List<Player> party;																						//List of Players
	private ScenarioData data;																						//Scenario Data Object
	private State state;																							//Scenario State
	private EnemyInfo enemyInfo;																					//Enemy data and methods																				
	private Hex[][] board;																							//Hex Board
	
	private transient Graphics2D g;
	private KeyEvent key;
	private char k;
	private int num; 
	private Point mouseClick=null;
	private InfusionTable elements = new InfusionTable();
	private int currentPlayer=0;
	private int turnIndex=0;
	private int enemyTurnIndex=0;
	private String targetID;
	private int itemUsed;
	private PlayerAbilityCard card = null;
	private Point selectionCoordinate=null; 
	private int direction=0;
	private Enemy enemyControlled;
	private Enemy enemyTarget;
	private Point updatePoint;
	private boolean anyMostersKilled=false;
	private boolean anyDoorOpened=false;
	
	public Scenario(int sceneID, List<Player> party, City gloomhaven, Shop shop) {
		this.shop=shop;
		this.gloomhaven=gloomhaven;
		this.party=party;
		
		data = ScenarioDataLoader.loadScenarioData(sceneID);														//Loads scenario data based on id
		enemyInfo = new EnemyInfo(data);																			//Loads enemy info from the scenario
		board = ScenarioBoardLoader.loadBoardLayout(sceneID, data);													//Loads board based on id and data
		
		for(int i=0; i<party.size(); i++)
			party.get(i).getStats().startScenario();																//Resets player scenario stats for battle cards													
		
		party.get(0).setCoordinates(data.getStartingPosition());													//Sets player 1 position
		UtilitiesBoard.updatePositions(board, party, enemyInfo.getEnemies());										//Updates the board with player and enemy positions
		state=State.CARD_SELECTION;																					//Initialization -> Card Selection
	}
	
	public boolean playRound(KeyEvent key, Graphics2D g, Point mouseClick) {
		
		this.g=g;																									
		this.key=key;
		this.mouseClick=mouseClick;

		if(Setting.drawLines)																						//[TESTING]
			GUI.drawLines(g);																						//Draws grid lines to help with gui placement
		
		setup();																									//Sets up loop and redraws most of the gui
		GUIScenario.drawControlsAndHelp(g, state, party, currentPlayer, card);										//Draws directions and controls for each state
		
		switch(state) {
			case CARD_SELECTION:
				if(enemyInfo.getEnemies().isEmpty()) {																//if there are no enemies currently on the board
					for(Player player : party)																		//set battle card flag if round isn't over
						player.getStats().setNoEnemiesAroundFlag(true);												//No enemies around flag
				}
				cardSelection();																					
				break;
			case INITIATIVE:
				initiative();
				break;
			case ATTACK:
				matchTurnWithEnemyOrPlayer();
				if(state==State.ATTACK)																				//[TODO] Figure out this bug
					turnIndex++;
				break;
			case LONG_REST:
				longRest();
				break;
			case PLAYER_CHOICE:
				playerCardChoice();
				break;
			case PLAYER_ITEM:
				usePlayerItem();
				break;
			case PLAYER_ATTACK_LOGIC:
				playerAttackLogic();
				break;
			case ENEMY_ATTACK:
				enemyAttack();
				break;
			case ENEMY_CONTROL_LOGIC:
				enemyControlLogic();
				break;
			case PLAYER_DEFENSE:
				playerDefense();
				break;
			case PLAYER_DISCARD:
				if(party.get(currentPlayer).discardForHealth(num, g))												//Prints ability cards then waits until one is picked. 
					enemyControlLogic();
				break;
			case PLAYER_MOVE:
				playerMove();
				Draw.drawParty(g, party, data.getHexLayout());																//Draws player hexes
				enemyInfo.drawEnemies(g, data.getHexLayout());																//Draws active enemy hexes
				Draw.itemsAndObstaclesOnly(g, board, data.getBoardSize(), data.getHexLayout());
				break;
			case PLAYER_ATTACK:
				playerAttack();
				break;
			case PLAYER_PUSH_SELECTION:
				playerPushSelection();
				break;
			case PLAYER_PUSH:
				playerPush();
				break;
			case MINDCONTROL:
				mindcontrol();
				break;
			case CREATE_INFUSION:
				createInfusion();
				break;
			case USE_ANY_INFUSION:
				useAnyInfusion();
				break;
			case ROUND_END_DISCARD:
				roundEndDiscard();
				break;
			case ROUND_END_REST:
				roundEndRest();
				break;
			default:
		}
		
		enemyInfo.update(board, data);																				//Removes dead enemies and increases stats
	
		return ScenarioEvaluateEnd.evaluateOne(enemyInfo.getEnemies(), data, party);								//Evaluates if the scenario goal has been met	
	}
	
	/** Setups the beginning of the loop */
	private void setup() {

		GUIScenario.drawStateOfScenario(g, gloomhaven, state, data);												//Draws the state of the scenario
		elements.graphicsDrawTable(g);																				//Draws the element table
		Draw.rectangleBoardSideways(g, board, data.getBoardSize(), data.getHexLayout());							//Draws board and border
		Draw.itemsAndObstaclesOnly(g, board, data.getBoardSize(), data.getHexLayout());
		Draw.drawParty(g, party, data.getHexLayout());																//Draws player hexes
		enemyInfo.drawEnemies(g, data.getHexLayout());																//Draws active enemy hexes
		GUIScenario.EntityTable(g, party, enemyInfo.getEnemies());													//Draws the entity table
		
		//TODO - Make this so it shows all the players
		party.get(0).graphicsPlayerInfo(g);																			//Draws player info
		party.get(0).graphicsDrawCardsInPlay(g);																	//Draws cards in play															

		k=UtilitiesGeneral.parseKeyCharacter(key);
		num=UtilitiesGeneral.parseKeyNum(key);
	}
	
	/** Card Selection Process */
	private void cardSelection() {

		if((k==Setting.restKey) && (party.get(currentPlayer).discardPileSize()>1))									//Player can choose to rest if there is a big enough discard pile
			party.get(currentPlayer).setLongRest();
		else if(key!=null && key.getKeyCode()!=KeyEvent.VK_ALT)														//If player presses ALT, hides the card selection
				party.get(currentPlayer).pickAbilityCards(key, num, g, mouseClick);
		else if(key==null)																							
			party.get(currentPlayer).pickAbilityCards(key, num, g, mouseClick);
		
		
		if(party.get(currentPlayer).cardsLocked()) {																//If player has picked both cards
			if((currentPlayer+1)!=party.size())																		//Go to the next player
				currentPlayer++;
			else {																									//Or change to state
				currentPlayer=0;
				selectionCoordinate=null;
				state=State.INITIATIVE;																				//Card Selection -> Initiative
			}
		}
	}
	
	/** Sets the initiative */
	private void initiative() {
		enemyInfo.initiationRound();																				//Sorts enemy by initiative
		party.sort(Comparator.comparingInt(Player::getInitiative));													//Order just the players based on initiative

		UtilitiesGeneral.setTurnNumbers(party, enemyInfo);															//Goes through player and enemies to determine turn order
		
		currentPlayer=0;
		turnIndex=0;
		state=State.ATTACK;
	}
	
	/** Matches the turn with enemy or player */
	private void matchTurnWithEnemyOrPlayer() {
		
		//Looks at enemies first to match with turn number
		for(int i=0; i<enemyInfo.getEnemyAbilityDeck().size(); i++) {
			if(enemyInfo.getEnemyAbilityDeck().get(i).getTurnNumber()==turnIndex) {									//Checks if it is enemy turn based on the turn tied to the ability deck
				enemyTurnIndex=0;																					//Resets enemy turn index for enemies in the current deck
				enemyInfo.setEnemyDeckIndex(i);																		//Sets the deck index for the enemy attack state					
				if(!enemyInfo.getEnemies().isEmpty())																//If enemies are left on the board
					state=State.ENEMY_ATTACK;																		//Match Turn -> Enemy Attack	
				else
					state=State.ROUND_END_DISCARD;																	//Ends the round if no enemies on board
			}
		}
		
		//After no enemies are found, looks at the party
		for(int i=0; i<party.size(); i++) {																			//Searches for a match on the turn and the players
			if(party.get(i).getTurnNumber()==turnIndex) {															//Once a match is found, checks if the player is on rest or needs to use cards
				if(party.get(i).onRest()) {					
					currentPlayer=i;
					party.get(i).resetCardChoice();
					state=State.LONG_REST;
				}
				else {
					currentPlayer=i;
					party.get(i).resetCardChoice();																	//Resets card choice so it can be used in player choice when picking cards
					state=State.PLAYER_CHOICE;
				}
				break;
			}
		}
	}
	
	/** Takes long rest. Lose one card, refresh cards, items and heal +2 */
	private void longRest() {
		boolean finished=false;																						//Indicates if the round is over
		party.get(currentPlayer).takeLongRest(g, num);																//Draws discard pile and has player pick a card and sets long rest to false
		if(!party.get(currentPlayer).onRest())																		//If long rest is over, then the turn is over
			finished=true;
		
		if(finished) {
			turnIndex++;																							//Moves to the next player
			state=State.ATTACK;																						//Long Rest -> Attack
		}
	}
	
	/** Prints picked ability cards. Has user make an attack */
	private void playerCardChoice() {
		int cardPick=party.get(currentPlayer).pickPlayCard(key, num, k, g);											//Prints ability cards then waits for one to pick
		if(cardPick>=1 && cardPick<=8) {
			card = party.get(currentPlayer).playCard();
			state=State.PLAYER_ATTACK_LOGIC;																		//Player Card Choice -> Player Attack Logic
		}
		
		if(cardPick>=100) {																							
			itemUsed=cardPick-100;																					//If Item is picked, goes to item phase
			state=State.PLAYER_ITEM;																				//Player Card Choice -> Player Item
		}
	}
	
	/** Resolves player picked item*/
	//TODO
	private void usePlayerItem() {
		List<Item> usableItems = ItemLoader.onTurn(party.get(currentPlayer).getItems());
		
		if(usableItems.get(itemUsed).getConsumed())																	//If the item is consume on use, consume
			ItemLoader.consumeItem(party.get(currentPlayer), usableItems.get(itemUsed));
		else if(usableItems.get(itemUsed).getSpent())																//If the item is spend on us, spend
			ItemLoader.spendItem(party.get(currentPlayer), usableItems.get(itemUsed));
		
		if(party.get(currentPlayer).getCreateAnyElement())															//If the item created an element
			state=State.CREATE_INFUSION;																			//Player Item -> Create Infusion
		else																										//else
			state=State.PLAYER_CHOICE;																				//Player Item -> Player Choice, so player has to play an item before thier last card
	}
	
	/** Controls the flow and logic of the player attacks*/
	private void playerAttackLogic() {
		if(selectionCoordinate==null)
			selectionCoordinate=new Point(party.get(currentPlayer).getCoordinates());
		
		UtilitiesAB.resolveCard(party.get(currentPlayer), card, elements, board, data, shop);
		
		if(UsePlayerAbilityCard.getMove(card)>0)
			state=State.PLAYER_MOVE;
		else if(UsePlayerAbilityCard.getCardData(card).getConsumeElementalFlag())
			state=State.USE_ANY_INFUSION;
		else if(UsePlayerAbilityCard.getRange(card)>0 || UsePlayerAbilityCard.getAttack(card)>0)
			state=State.PLAYER_ATTACK;
		else {
			if(party.get(currentPlayer).getCardChoice()==false) {
				state=State.PLAYER_CHOICE;
			}else {
				//if turn is over
				if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))
					state=State.ROUND_END_DISCARD;
				else {
					turnIndex++;
					state=State.ATTACK;
				}
			}
		}
	}
	
	private void selection() {
		
		if(mouseClick!=null) {
			Point p = UtilitiesHex.getOffsetHexFromPixels(mouseClick, data.getHexLayout());
			if(p.x>=0 && p.y>=0 && board[p.x][p.y]!=null)
				selectionCoordinate=p;
			
			mouseClick=null;
		}
		
		if(key!=null)
			if(key.getKeyCode()==KeyEvent.VK_SPACE)
				direction++;
		
		if(k==Setting.up) {
			if(selectionCoordinate.y-1>=0 && board[selectionCoordinate.x][selectionCoordinate.y-1]!=null) {
				selectionCoordinate.y=selectionCoordinate.y-1;
			}
			direction++;
		}
		if(k==Setting.left) {
			if(selectionCoordinate.x-1>=0 && board[selectionCoordinate.x-1][selectionCoordinate.y]!=null) {
				selectionCoordinate.x=selectionCoordinate.x-1;
			}
			direction--;
		}
		if(k==Setting.down) {
			if(selectionCoordinate.y+1<=data.getBoardSize().getY() && board[selectionCoordinate.x][selectionCoordinate.y+1]!=null) {
				selectionCoordinate.y=selectionCoordinate.y+1;
			}
			direction--;
		}
		if(k==Setting.right) {
			if(selectionCoordinate.x+1<=data.getBoardSize().getX() && board[selectionCoordinate.x+1][selectionCoordinate.y]!=null) {
				selectionCoordinate.x=selectionCoordinate.x+1;
			}
			direction++;
		}
		
		if(direction==6)
			direction=0;
		else if(direction==-1)
			direction=5;
	}
	
	private boolean movePlayer(Player player, Point ending) {	
	
		//if(board[ending.x][ending.y].hasObstacle() || board[ending.x][ending.y].isHidden() || board[ending.x][ending.y].getQuickID().equals("E"))
		if(!board[ending.x][ending.y].isSpaceEmpty())
			return false;
		
		if(board[(int) ending.getX()][(int) ending.getY()].hasLoot()) {
			UtilitiesLoot.loot(board, shop, player, ending);
		}
		
		if(board[(int) ending.getX()][(int) ending.getY()].hasDoor() &&(board[(int) ending.getX()][(int) ending.getY()].isDoorOpen()==false)) {
			//showRoom(board[(int) ending.getX()][(int) ending.getY()].getRoomID());
			
			if(!anyDoorOpened) {
				anyDoorOpened=true;
				player.getStats().setFirstToOpenDoor(true);
			}
			board[(int) ending.getX()][(int) ending.getY()].openDoor();
			ScenarioBoardLoader.showRoom(board, data.getId(), board[ending.x][ending.y].getRoomID());
			//enemyInfo.updateEnemyList(data.getId(), board[ending.x][ending.y].getRoomID());
			//board[(int) ending.getX()][(int) ending.getY()].closeDoor();
			updatePoint=new Point(ending);
			enemyInfo.setUpdateEnemyFlag(true);
			System.out.println("Opening Door");
		}
		
		/*
		String quickID=board[(int) starting.getX()][(int) starting.getY()].getQuickID();
		String id=board[(int) starting.getX()][(int) starting.getY()].getID();
		
		board[(int) ending.getX()][(int) ending.getY()].setHex(quickID, id);
		board[(int) starting.getX()][(int) starting.getY()].reset();
		*/

		//Resets the old tile
		board[player.getCoordinates().x][player.getCoordinates().y].setSpaceEmpty();
		player.setCoordinates(ending);
		
		return true;

	}
	
	private void enemyAttack() {
		
		enemyInfo.drawAbilityCard(g, enemyInfo.getEnemy(enemyTurnIndex));  
		enemyInfo.enemyMoveProcedure(board, enemyTurnIndex, party, g, data);
		
		UtilitiesBoard.updatePositions(board, party, enemyInfo.getEnemies());
		
		System.out.println("Loc: scenario.java - Enemy "+enemyInfo.getEnemy(enemyTurnIndex).getClassID()+" is attacking ");
		
		if(enemyInfo.getEnemy(enemyTurnIndex).getCharacterData().getBossFlag())
			BossMoves.move(enemyInfo.getEnemy(enemyTurnIndex).getClassID());
		
		List<Player> targets = new ArrayList<Player>();
		if(enemyInfo.getTurnEnemies().size()!=0)
			targets = UtilitiesTargeting.createTargetListPlayer(board, enemyInfo.getEnemy(enemyTurnIndex).getBaseStats().getRange(), enemyInfo.getEnemy(enemyTurnIndex).getCubeCoordiantes(data.getHexLayout()), data.getBoardSize(), party, data.getHexLayout());
		//targets = enemyInfo.createTargetListForEnemy(enemyTurnIndex, party, g);

		if(targets.size()>0) {
			
			int min=100;
			int targetIndex=-1;
			
			for(int i=0; i<targets.size(); i++) {
				if(UtilitiesAB.distance(enemyInfo.getEnemy(enemyTurnIndex).getCoordinates(), party.get(currentPlayer).getCoordinates())<min) {
					targetIndex=i;
					min=UtilitiesAB.distance(enemyInfo.getEnemy(enemyTurnIndex).getCoordinates(), party.get(currentPlayer).getCoordinates());
				}else if(UtilitiesAB.distance(enemyInfo.getEnemy(enemyTurnIndex).getCoordinates(), party.get(currentPlayer).getCoordinates())==min) {
					if(targets.get(targetIndex).getInitiative()>targets.get(i).getInitiative()) {
						targetIndex=i;
					}
				}
			}
			
			targetID=targets.get(targetIndex).getID();													//[Temp] Picks first one on the list
		
			if(targets.get(targetIndex).hasRetaliate())
				System.out.println("Scenario.java Loc 276: Reminder that if the player attacks a target with retalite it doesn't resolve anymore");
		
			state=State.PLAYER_DEFENSE;															//Next State: Player Defense
		}else {
			state=State.ENEMY_CONTROL_LOGIC;																//Next State: Attack, Enemy Attack, Round End
		}
	}
	
	private void enemyControlLogic() {
		
		if(enemyTurnIndex==(enemyInfo.getEnemies().size()-1)) {												//If it has gone through all the enemies, go to next state
			if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))																//If if it is on the last turn, End Round
				state=State.ROUND_END_DISCARD;														
			else {
				turnIndex++;																				//End turn go back to attack logic state
				if(Setting.stateTest)
					System.out.println("Going to state attack with "+turnIndex+" as a turn index.");
				state=State.ATTACK;
			}	
		}
		else {
			enemyTurnIndex++;																		//Cycle through enemies and go to enemy attack state
			
			if(enemyInfo.getEnemies().size()>0)
				if(enemyInfo.getEnemy(enemyTurnIndex).getClassID().equals(enemyInfo.getDeckClass())) {
					if(Setting.stateTest)
						System.out.println("Going to enemy attack state: "+enemyInfo.getEnemy(enemyTurnIndex).getClassID());
					state=State.ENEMY_ATTACK;
				}
		}
	}
	
	private void playerDefense() {
		
		enemyInfo.drawAbilityCard(g, enemyInfo.getEnemy(enemyTurnIndex));
		GUI.drawEnemyAttack(g, enemyInfo.getEnemy(enemyTurnIndex), enemyInfo.getAttack(enemyTurnIndex));
		
		int playerIndex = getTargetIndex();
		
		if((k==Setting.healKey)||(party.get(playerIndex).abilityCardsLeft()==0)) {
			int damage = enemyInfo.getAttack(enemyTurnIndex);

			party.get(playerIndex).takeDamage(damage);;
			if(party.get(playerIndex).getCharacterData().getHealth()<=0)
				party.remove(playerIndex);
			
			if(party.size()==0)
				state=State.ROUND_END_DISCARD;
			else
				state=State.ENEMY_CONTROL_LOGIC;
		}
		
		if(k==Setting.discardKey)
			state=State.PLAYER_DISCARD;
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
	
	private void playerMove() {
		boolean finished=false;
		
		if(party.get(currentPlayer).canMove()) {
			g.setColor(Color.red);
			selection();
			Draw.range(g, party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), UsePlayerAbilityCard.getMove(card), data.getHexLayout());
			
			g.setColor(Color.cyan);
			Draw.drawHex(g, UtilitiesHex.getCubeCoordinates(data.getHexLayout(), selectionCoordinate), null, data.getHexLayout(), null);
			
			if(k==Setting.moveKey) {
				if(UtilitiesHex.distance(UtilitiesHex.getCubeCoordinates(data.getHexLayout(), selectionCoordinate), party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()))<UsePlayerAbilityCard.getMove(card)) {
				
					if(board[selectionCoordinate.x][selectionCoordinate.y].getQuickID().equals("P"))
						finished=true;
					else if(UsePlayerAbilityCard.hasFlying(card)) {
						finished=movePlayer(party.get(currentPlayer), selectionCoordinate);
					}
					else if(UsePlayerAbilityCard.hasJump(card)) {
						if(board[selectionCoordinate.x][selectionCoordinate.y].isSpaceEmpty()) {
							finished=movePlayer(party.get(currentPlayer), selectionCoordinate);
						}
					}
					else {
						if(board[selectionCoordinate.x][selectionCoordinate.y].isSpaceEmpty()) {
							finished=movePlayer(party.get(currentPlayer), selectionCoordinate);
						}
					}
				}

			}
		}else {
			finished=true;
		}
		
		//Next State: Player Attack, Attack Logic, Round End
		if(finished) {
			UtilitiesBoard.updatePositions(board, party, enemyInfo.getEnemies());
			
			if(UsePlayerAbilityCard.getCardData(card).getConsumeElementalFlag()) {
				state=State.USE_ANY_INFUSION;
			}
			else if(UsePlayerAbilityCard.getRange(card)>0 || UsePlayerAbilityCard.getAttack(card)>0) {
				state=State.PLAYER_ATTACK;
			}else if(UsePlayerAbilityCard.getCardData(card).getEffects().getPush()>0) {
				state=State.PLAYER_PUSH_SELECTION;
			}else {
				if(party.get(currentPlayer).getCardChoice()==false) {
					state=State.PLAYER_CHOICE;
				}else {
					//if turn is over
					if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))
						state=State.ROUND_END_DISCARD;
					else {
						turnIndex++;
						state=State.ATTACK;
					}
				}
			}
		}
	}
	
	private void playerAttack() {
		boolean finished=false;
		UtilitiesBoard.updatePositions(board, party, enemyInfo.getEnemies());
		if(party.get(currentPlayer).canAttack()) {
			
			//Creates target list of enemy coordinates
			List<Point> targets = new ArrayList<Point>();
			int cardRange=UsePlayerAbilityCard.getRange(card);
			if(UsePlayerAbilityCard.getRange(card)>=0) {
				if(UsePlayerAbilityCard.getRange(card)==0)
					cardRange=1;
	
				if(UsePlayerAbilityCard.hasTargetHeal(card)) {
					for(int range=1; range<=cardRange; range++)
						targets=UtilitiesTargeting.createTargetList(board, range, party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), "P", data.getBoardSize(), data.getHexLayout());
					targets.add(party.get(currentPlayer).getCoordinates());
				}
				else {
					for(int range=1; range<=cardRange; range++) {
						targets=UtilitiesTargeting.createTargetList(board, range, party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), "E", data.getBoardSize(), data.getHexLayout());
					}
				}
			}
			
			if(targets.size()>0) {
				UtilitiesTargeting.highlightTargets(targets, g, data.getHexLayout());
				
				selection();
				g.setColor(Color.cyan);
						
				if(UsePlayerAbilityCard.getRange(card)==0) {
					UtilitiesTargeting.drawAttack(g, party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), direction, UsePlayerAbilityCard.getCardData(card).getData().getTarget().getTargets(), data.getHexLayout());
				}
				else {
					Draw.drawHex(g, selectionCoordinate, null, data.getHexLayout(), null);
				}
				
				if(k==Setting.targetKey) {
				
					if(UsePlayerAbilityCard.hasTargetHeal(card)) {
						if(board[selectionCoordinate.x][selectionCoordinate.y].getQuickID().equals("P")) {
							if(targets.contains(selectionCoordinate)) {
								for(int i=0; i<party.size(); i++) {
									if(party.get(i).getCoordinates()==selectionCoordinate) {
										party.get(i).heal(UsePlayerAbilityCard.getHeal(card));
										finished=true;
									}
								}
							}
						}
					}
					else if(UsePlayerAbilityCard.getRange(card)==0) {
						int num = UsePlayerAbilityCard.getCardData(card).getData().getTarget().getTargets();
						HexCoordinate hex=UtilitiesHex.neighbor(party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), direction);
						selectionCoordinate=new Point(UtilitiesHex.getOffset(data.getHexLayout(), hex));
						
						finished=attackProcedure(new Point(selectionCoordinate), targets);
						
						if(num>=2) {
							hex=UtilitiesHex.neighbor(party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), direction+1);
							selectionCoordinate=new Point(UtilitiesHex.getOffset(data.getHexLayout(), hex));
							boolean temp=attackProcedure(new Point(selectionCoordinate), targets);
							if(temp)
								finished=true;
						}
						if(num>=3) {
							hex=UtilitiesHex.neighbor(party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), direction-1);
							selectionCoordinate=new Point(UtilitiesHex.getOffset(data.getHexLayout(), hex));
							boolean temp=attackProcedure(new Point(selectionCoordinate), targets);
							if(temp)
								finished=true;
						}
						
						if(num>=4) {
							hex=UtilitiesHex.neighbor(party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), direction+2);
							selectionCoordinate=new Point(UtilitiesHex.getOffset(data.getHexLayout(), hex));
							attackProcedure(new Point(selectionCoordinate), targets);
						}
						
						if(num>=5) {
							hex=UtilitiesHex.neighbor(party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), direction-2);
							selectionCoordinate=new Point(UtilitiesHex.getOffset(data.getHexLayout(), hex));
							boolean temp=attackProcedure(new Point(selectionCoordinate), targets);
							if(temp)
								finished=true;
						}
						
						if(num>=6) {
							hex=UtilitiesHex.neighbor(party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), direction+3);
							selectionCoordinate=new Point(UtilitiesHex.getOffset(data.getHexLayout(), hex));
							boolean temp=attackProcedure(new Point(selectionCoordinate), targets);
							if(temp)
								finished=true;
						}
					}
					else {
					
						if(UsePlayerAbilityCard.hasMindControl(card)) {
							enemyControlled=enemyInfo.getEnemy(selectionCoordinate);
							state=state.MINDCONTROL;
						}else {
							finished=attackProcedure(new Point(selectionCoordinate), targets);
							
							if(UsePlayerAbilityCard.getCardData(card).getData().getTarget().getTargets()>1 && card.getAbilityCardCount()!=UsePlayerAbilityCard.getCardData(card).getData().getTarget().getTargets()){
								System.out.println("1:  "+targets);
								targets.remove(targets.indexOf(selectionCoordinate));
								System.out.println("2:  "+targets);
								finished=false;
							}else {
								for(int i=0; i<party.get(currentPlayer).getCounterTriggers().size(); i++) {
									if(party.get(currentPlayer).getCounterTriggers().get(i).getTriggerFlag().equals("forEachTargeted"))
										System.out.println("Scenario.java Loc 648: Reminder that there needs to be a resolution for forEachTargeted flag");
								}
								finished=true;	
							}
						}
					}
				}
			}else {
				finished=true;
			}
		}else {
			finished=true;
		}
		
		if(finished) {
			selectionCoordinate=new Point(party.get(currentPlayer).getCoordinates());
			
			if(UsePlayerAbilityCard.getCardData(card).getEffects().getPush()>0) {
				state=State.PLAYER_PUSH_SELECTION;
			}else {
				if(party.get(currentPlayer).getCardChoice()==false) {
					state=State.PLAYER_CHOICE;
				}else {
					//if turn is over
					if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))
						state=State.ROUND_END_DISCARD;
					else {
						turnIndex++;
						state=State.ATTACK;
					}
				}
			}
		}
	}
	
	private boolean attackProcedure(Point selection, List<Point> targets) {
		if(board[selection.x][selection.y].getQuickID()=="E") {
			if(targets.contains(selection)) {
				card.increaseAbilityCardCounter();
				boolean adjacentBonus=false;
				UtilitiesAB.resolveAttack(enemyInfo.getEnemy(selection), party.get(currentPlayer), card, board, adjacentBonus, elements, data, anyMostersKilled);
				return true;
			}
		}
		return false;
	}
	
	private void playerPushSelection() {
		boolean finished=false;
		
		//Creates target list of enemy coordinates
		List<Point> targets = new ArrayList<Point>();
		int cardRange=UsePlayerAbilityCard.getRange(card);
		
		if(UsePlayerAbilityCard.getRange(card)>=0) {
			if(UsePlayerAbilityCard.getRange(card)==0)
				cardRange=1;

				for(int range=1; range<=cardRange; range++)
					targets=UtilitiesTargeting.createTargetList(board, range, party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()), "E", data.getBoardSize(), data.getHexLayout());
		}
		
		//If there are targets, highlight the targets and wait for selection
		if(targets.size()>0) {
			
			UtilitiesTargeting.highlightTargets(targets, g, data.getHexLayout());
			
			selection();
			g.setColor(Color.cyan);
			//if(UsePlayerAbilityCard.getCardData(card).getEffects().getRange()!=0)
			Draw.drawHex(g, UtilitiesHex.getCubeCoordinates(data.getHexLayout(), selectionCoordinate), null, data.getHexLayout(), null);

			//Space is used for selection of target
			if(k==Setting.targetKey) {
				if(board[selectionCoordinate.x][selectionCoordinate.y].getQuickID().equals("E")) {
					if(targets.contains(selectionCoordinate)) {

						//oppPoint = new Point(UtilitiesTargeting.findOppisiteHex(party.get(currentPlayer).getCoordinates(), enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())).getCoordinates()));
						enemyTarget=enemyInfo.getEnemy(selectionCoordinate);
						direction=UtilitiesHex.getDirection(enemyTarget.getCubeCoordiantes(data.getHexLayout()), party.get(currentPlayer).getCubeCoordiantes(data.getHexLayout()));
						//tempHoldVar=new Point(enemyInfo.getEnemyFromID(room.getID(room.getSelectionCoordinates())).getCoordinates());
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
				if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))
					state=State.ROUND_END_DISCARD;
				else {
					turnIndex++;
					state=State.ATTACK;
				}
			}
		}
	}
	
	private void playerPush() {
		boolean finished=false;
		
		HexCoordinate pushPoint = UtilitiesHex.neighbor(enemyTarget.getCubeCoordiantes(data.getHexLayout()), direction);
		g.setColor(Color.cyan);
		Draw.drawHex(g, pushPoint, null, data.getHexLayout(), null);
		
		if(num>=1 && num<=3) {
			if(num==1) {
				pushPoint = UtilitiesHex.neighbor(enemyTarget.getCubeCoordiantes(data.getHexLayout()), direction+1);
				enemyTarget.move(pushPoint, data.getBoardSize());
			}else if(num==2) {
				enemyTarget.move(pushPoint, data.getBoardSize());
			}
			else {
				pushPoint = UtilitiesHex.neighbor(enemyTarget.getCubeCoordiantes(data.getHexLayout()), direction-1);
				enemyTarget.move(pushPoint, data.getBoardSize());
			}
			finished=true;
		}
		
		if(finished) {
			UtilitiesBoard.updatePositions(board, party, enemyInfo.getEnemies());
			card.increaseAbilityCardCounter();
			/*
			 * I know the +1 below doesn't make sense but because most of the cards attack before pushing, it starts with 1. I think* they all
			 * attack first. If they don't, I will need to just clear the abilitycounter when I pick a push target
			 */
			if((UsePlayerAbilityCard.getCardData(card).getEffects().getPush()+1)>card.getAbilityCardCount())
			{
				//oppPoint=new Point(pointToMove);
				state=State.PLAYER_PUSH;
			}else {
				if(party.get(currentPlayer).getCardChoice()==false) {
					state=State.PLAYER_CHOICE;
				}else {
					//if turn is over
					if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))
						state=State.ROUND_END_DISCARD;
					else {
						turnIndex++;
						state=State.ATTACK;
					}
				}
			}
		}
	}
	
	private void mindcontrol() {
		boolean finished=false;
		
		if(UsePlayerAbilityCard.getCardData(card).getData().getMove()>0)
			finished=mindControlMove(party.get(currentPlayer), enemyControlled);
		
		if(UsePlayerAbilityCard.getCardData(card).getData().getAttack()>0)
			finished=mindControlAttack(party.get(currentPlayer), enemyControlled);
		
		//Next State: Next card, Attack Logic, End Round
		if(finished) {
			UtilitiesBoard.updatePositions(board, party, enemyInfo.getEnemies());
			
			if(party.get(currentPlayer).getCardChoice()==false) {
				state=State.PLAYER_CHOICE;
			}else {
				//if turn is over
				if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))
					state=State.ROUND_END_DISCARD;
				else {
					turnIndex++;
					state=State.ATTACK;
				}
			}
		}
	}
	
	private boolean mindControlMove(Player player, Enemy enemy) {
		boolean finished=false;
		
		//Highlight tiles that players can move to
		HexCoordinate enemyPoint=enemy.getCubeCoordiantes(data.getHexLayout());
		CardDataObject cardData = UsePlayerAbilityCard.getCardData(card);
		
		for(int r=1; r<=cardData.getData().getRange(); r++) {
			Draw.range(g, enemyPoint, r, data.getHexLayout());
		}
		
		selection();
		g.setColor(Color.cyan);
		Draw.drawHex(g, UtilitiesHex.getCubeCoordinates(data.getHexLayout(), selectionCoordinate), null, data.getHexLayout(), null);
				
		if(k==Setting.moveKey) {
			if(UtilitiesHex.distance(enemyPoint, UtilitiesHex.getCubeCoordinates(data.getHexLayout(), selectionCoordinate))<=cardData.getData().getRange()) {
				if(board[selectionCoordinate.x][selectionCoordinate.y].getQuickID().equals("E")) {
					return true;
				}
				else if(UsePlayerAbilityCard.hasFlying(card)) {
					//NEED TO HANDLE MULTIPLE PEOPLE OR THINGS ON A HEX
					enemy.move(selectionCoordinate, data.getBoardSize());
					return true;
				}
				else if(UsePlayerAbilityCard.hasJump(card)) {
					if(board[selectionCoordinate.x][selectionCoordinate.y].isSpaceEmpty()) {
						enemy.move(selectionCoordinate, data.getBoardSize());
						return true;
					}
				}
				else {
					//NEED TO ADD IN A CHECK FOR PATH IF JUMP IS NOT TRUE
					if(board[selectionCoordinate.x][selectionCoordinate.y].isSpaceEmpty()) {
						enemy.move(selectionCoordinate, data.getBoardSize());
						return true;
					}
				}
				
			}
		}
		return false;		
	}
	
	private boolean mindControlAttack(Player player, Enemy enemy) {
		boolean finished=false;
		
		//Creates target list of enemy coordinates
		List<Point> targets = new ArrayList<Point>();
		int cardRange=UsePlayerAbilityCard.getCardData(card).getData().getRange();
		if(cardRange>=0) {
			if(cardRange==0)
				cardRange=1;
	
				for(int range=1; range<=cardRange; range++)
					targets=UtilitiesTargeting.createTargetList(board, range, enemy.getCubeCoordiantes(data.getHexLayout()), "E", data.getBoardSize(), data.getHexLayout());
		}
		
		if(targets.size()>0) {
			UtilitiesTargeting.highlightTargets(targets, g, data.getHexLayout());
			selection();
			g.setColor(Color.cyan);
			Draw.drawHex(g, UtilitiesHex.getCubeCoordinates(data.getHexLayout(), selectionCoordinate), null, data.getHexLayout(), null);
			
			if(k==Setting.targetKey) {
				if(board[selectionCoordinate.x][selectionCoordinate.y].getQuickID().equals("E")) {
					if(targets.contains(selectionCoordinate)) {
						UtilitiesAB.resolveAttackEnemyOnEnemy(enemyInfo.getEnemy(selectionCoordinate), enemy, UsePlayerAbilityCard.getCardData(card).getData().getAttack());
						return true;
					}
				}
			}
			return false;
		}else {
			return true;
		}
	}
	
	private void createInfusion() {
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
	
	private void useAnyInfusion() {
		if(elements.consumeAny(g, num)) {
			selectionCoordinate=new Point(party.get(currentPlayer).getCoordinates());
			if(UsePlayerAbilityCard.getRange(card)>0 || UsePlayerAbilityCard.getAttack(card)>0) {
				state=State.PLAYER_ATTACK;
			}else if(UsePlayerAbilityCard.getCardData(card).getEffects().getPush()>0) {
				
				state=State.PLAYER_PUSH_SELECTION;
			}else {
				if(party.get(currentPlayer).getCardChoice()==false) {
					state=State.PLAYER_CHOICE;
				}else {
					//if turn is over
					if(turnIndex==(party.size()+enemyInfo.getEnemyAbilityDeck().size()-1))
						state=State.ROUND_END_DISCARD;
					else {
						turnIndex++;
						state=State.ATTACK;
					}
				}
			}
		}
	}
	
	private void roundEndDiscard() {
		elements.endOfRound();
		
		if(enemyInfo.getUpdateEnemyFlag())
			enemyInfo.updateEnemyList(data.getId(), board[updatePoint.x][updatePoint.y].getRoomID());

		for(int i=0; i<party.size(); i++)
			party.get(i).endTurn();																//End of turn clean up for each player
		
		for(Player player : party) {
			if(player.isExhausted()) {
				for(Player cardHolder : party) {
					if(cardHolder.getBattleGoalCard().getThresholdKeyword().equals("exhaustion"))
						cardHolder.getStats().setExhaustionFlag(true);
				}
			}
		}
		
		if(party.size()==0) 
			System.exit(1);																		//If party is dead, end program
		
		if(enemyInfo.getCount()==0) {
			System.out.println("No more enemies probably a mistake");
			System.exit(1);																		//If all enemies are dead, end program
		}
		currentPlayer=0;																		//Resets current player to use in round end rest state
		state=State.ROUND_END_REST;	
	}
	
	private void roundEndRest() {
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
			turnIndex=0;																				//Resets turn
			currentPlayer=0;																	//Resets currentPlayer
			state=State.CARD_SELECTION;															//Next State: Card Selection (Back to beginning)
		}
	}
	
	public ScenarioData getData() {return data;}
}